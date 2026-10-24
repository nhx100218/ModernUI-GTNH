package blocamlimb.nhx100218.modernui.uilib.ui.image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import blocamlimb.nhx100218.modernui.uilib.ui.image.DocumentRemoteImageCache.Entry;
import blocamlimb.nhx100218.modernui.uilib.ui.image.DocumentRemoteImageCache.Status;

/**
 * 远程图片缓存的任务归属、失效与重试语义测试。
 *
 * <p>用本机回环上的 {@link HttpServer} 造可控网络条件（首次 500 再 200、404、阻塞响应），
 * 不依赖外网；每个用例自建自停服务端，互不影响。重点覆盖：</p>
 * <ul>
 *   <li>clear 使在飞 / 排队任务失效——旧任务不发布结果、不回调旧消费者，且被取消的排队任务
 *       必须真正释放执行器队列位；</li>
 *   <li>清空扫掠与并发插入共享同一归属线性化，不产生「已移出 map 却未弃用」的孤儿条目；</li>
 *   <li>临时失败只在下一次 request 上重试，退避期内不重复请求、尝试次数有界；</li>
 *   <li>终态失败（404）不重试；</li>
 *   <li>队列有界，队满拒绝时落为临时失败而不是阻塞调用线程；</li>
 *   <li>消费者回调不在缓存/条目锁内执行。</li>
 * </ul>
 */
public class DocumentRemoteImageCacheTest {

    private DocumentRemoteImageCache cache;
    private HttpServer server;
    private ExecutorService serverExecutor;
    private byte[] pngBytes;

    @Before
    public void setUp() throws Exception {
        cache = DocumentRemoteImageCache.getInstance();
        cache.clearForTesting();
        pngBytes = encodePng();
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (serverExecutor != null) {
            serverExecutor.shutdownNow();
            serverExecutor = null;
        }
        cache.clearForTesting();
    }

    // ======================= 临时失败的有界重试 =======================

    /**
     * 临时失败（500）后：退避期内的 request 不重试；退避到期后的下一次 request 才重试，
     * 且重试复用同一条目（共享加载），成功只回调触发重试的那次请求。
     */
    @Test
    public void transientFailureRetriesOnlyOnNextRequestAfterBackoff() throws Exception {
        final AtomicInteger httpCalls = new AtomicInteger();
        String url = startServer(new Responder() {
            @Override
            public void respond(HttpExchange exchange) throws IOException {
                sendPng(exchange, httpCalls.incrementAndGet() == 1 ? 500 : 200);
            }
        });
        final AtomicInteger firstCallback = new AtomicInteger();
        final AtomicInteger retryCallback = new AtomicInteger();
        final CountDownLatch retryCompleted = new CountDownLatch(1);

        Entry entry = cache.request(url, new Runnable() {
            @Override
            public void run() {
                firstCallback.incrementAndGet();
            }
        });
        awaitStatus(entry, Status.FAILED);
        Assert.assertEquals("首次尝试应真的打到服务端", 1, httpCalls.get());

        Entry repeated = cache.request(url, null);
        Assert.assertSame("同一 URL 必须复用同一条目", entry, repeated);
        Assert.assertEquals("退避期内不得重试", Status.FAILED, entry.getStatus());
        Assert.assertEquals("退避期内不得再打服务端", 1, httpCalls.get());

        expireBackoff(entry);
        Assert.assertSame("重试仍走同一条目（重复请求合并）", entry, cache.request(url, new Runnable() {
            @Override
            public void run() {
                retryCallback.incrementAndGet();
                retryCompleted.countDown();
            }
        }));
        Assert.assertTrue("重试完成后才核对回调次数", retryCompleted.await(5L, TimeUnit.SECONDS));
        awaitStatus(entry, Status.LOADED);
        Assert.assertNotNull("重试成功后条目应带位图", entry.getImage());
        Assert.assertEquals("首次请求的回调不因重试补发", 0, firstCallback.get());
        Assert.assertEquals("触发重试的那次请求才收到回调", 1, retryCallback.get());
        Assert.assertEquals("重试应真的再打一次服务端", 2, httpCalls.get());
    }

    /** 临时失败的尝试次数有界：到顶后停在 FAILED，再 request 也不打服务端。 */
    @Test
    public void transientFailureStopsAtAttemptLimit() throws Exception {
        final AtomicInteger httpCalls = new AtomicInteger();
        String url = startServer(new Responder() {
            @Override
            public void respond(HttpExchange exchange) throws IOException {
                httpCalls.incrementAndGet();
                sendPng(exchange, 503);
            }
        });
        Entry entry = cache.request(url, null);
        for (int attempt = 1; attempt <= 3; attempt++) {
            awaitStatus(entry, Status.FAILED);
            Assert.assertEquals("第 " + attempt + " 次尝试应已发生", attempt, httpCalls.get());
            expireBackoff(entry);
            cache.request(url, null);
        }

        awaitStatus(entry, Status.FAILED);
        expireBackoff(entry);
        cache.request(url, null);
        Assert.assertEquals("达到尝试上限后必须停在 FAILED", Status.FAILED, entry.getStatus());
        Assert.assertEquals("达到尝试上限后不得再打服务端", 3, httpCalls.get());
    }

    /** 终态失败（404）不做自动重试，即使退避时钟已过期。 */
    @Test
    public void permanentFailureIsNeverRetried() throws Exception {
        final AtomicInteger httpCalls = new AtomicInteger();
        String url = startServer(new Responder() {
            @Override
            public void respond(HttpExchange exchange) throws IOException {
                httpCalls.incrementAndGet();
                sendPng(exchange, 404);
            }
        });
        Entry entry = cache.request(url, null);
        awaitStatus(entry, Status.FAILED);

        expireBackoff(entry);
        cache.request(url, null);
        Assert.assertEquals("404 属终态失败", Status.FAILED, entry.getStatus());
        Assert.assertEquals("终态失败不得重试", 1, httpCalls.get());
    }

    // ======================= 旧任务失效、归属线性化与排队容量 =======================

    /** clear 之后，在飞的共享下载必须自然结束且不发布结果、不回调旧消费者。 */
    @Test
    public void clearInvalidatesInFlightDownloadWithoutPublishingOrCallback() throws Exception {
        final CountDownLatch arrived = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch finished = new CountDownLatch(1);
        final AtomicInteger httpCalls = new AtomicInteger();
        String url = startServer(new Responder() {
            @Override
            public void respond(HttpExchange exchange) throws IOException {
                httpCalls.incrementAndGet();
                arrived.countDown();
                awaitQuietly(release, 5L);
                sendPng(exchange, 200);
            }
        });
        Field executorField = DocumentRemoteImageCache.class.getDeclaredField("executorService");
        executorField.setAccessible(true);
        ExecutorService original = (ExecutorService) executorField.get(cache);
        ThreadPoolExecutor observed = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>()) {
            @Override
            protected void afterExecute(Runnable command, Throwable error) {
                finished.countDown();
            }
        };
        executorField.set(cache, observed);
        try {
            final AtomicInteger callbacks = new AtomicInteger();
            Runnable callback = new Runnable() {
                @Override
                public void run() {
                    callbacks.incrementAndGet();
                }
            };
            Entry entry = cache.request(url, callback);
            Assert.assertTrue("下载应已开始", arrived.await(5L, TimeUnit.SECONDS));
            Assert.assertSame("在飞请求由所有消费者共享", entry, cache.request(url, callback));

            cache.clearForTesting();
            release.countDown();
            // 等真正退出任务，不能把服务端写完响应或 Future 已取消当成下载线程已经结束。
            Assert.assertTrue("旧任务应自然结束", finished.await(5L, TimeUnit.SECONDS));
            Assert.assertEquals("共享请求只下载一次", 1, httpCalls.get());
            Assert.assertEquals("清空后旧任务不得回调消费者", 0, callbacks.get());
            Assert.assertNull("清空后旧任务不得把位图写进条目", entry.getImage());
            Assert.assertNotEquals("清空后旧任务不得发布 LOADED", Status.LOADED, entry.getStatus());
        } finally {
            release.countDown();
            cache.clear();
            executorField.set(cache, original);
            observed.shutdownNow();
        }
    }

    /** 消费者回调不得在条目/缓存锁内执行：回调阻塞时 clear 仍须立即返回。 */
    @Test
    public void completionCallbackRunsOutsideTheEntryLock() throws Exception {
        String url = startServer(new Responder() {
            @Override
            public void respond(HttpExchange exchange) throws IOException {
                sendPng(exchange, 200);
            }
        });
        final CountDownLatch inCallback = new CountDownLatch(1);
        final CountDownLatch releaseCallback = new CountDownLatch(1);
        Entry entry = cache.request(url, new Runnable() {
            @Override
            public void run() {
                inCallback.countDown();
                awaitQuietly(releaseCallback, 5L);
            }
        });
        Assert.assertTrue("回调应被调用", inCallback.await(5L, TimeUnit.SECONDS));

        final AtomicReference<Throwable> clearFailure = new AtomicReference<Throwable>();
        Thread clearer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    cache.clearForTesting();
                } catch (Throwable error) {
                    clearFailure.set(error);
                }
            }
        }, "remote-image-clear-probe");
        clearer.start();
        clearer.join(1500L);
        Assert.assertFalse("回调执行期间不得持有缓存/条目锁（clear 不得被回调拖住）", clearer.isAlive());
        Assert.assertNull(clearFailure.get());
        releaseCallback.countDown();
        clearer.join(5000L);
        Assert.assertEquals("发布先于 clear：条目应保持已加载终态", Status.LOADED, entry.getStatus());
    }

    /**
     * 队列有界：队满时该次请求落为临时失败并立即返回，绝不阻塞调用线程。
     *
     * <p>用「唯一工作线程被占用 + 唯一队列位被占用」的饱和池替换内部执行器，确定性地触发拒绝；
     * 用例结束恢复原执行器。</p>
     */
    @Test
    public void saturatedQueueFailsEntryInsteadOfBlockingCaller() throws Exception {
        Field executorField = DocumentRemoteImageCache.class.getDeclaredField("executorService");
        executorField.setAccessible(true);
        ExecutorService original = (ExecutorService) executorField.get(cache);
        final CountDownLatch occupyThread = new CountDownLatch(1);
        ThreadPoolExecutor saturated = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1), new ThreadPoolExecutor.AbortPolicy());
        saturated.execute(new Runnable() {
            @Override
            public void run() {
                awaitQuietly(occupyThread, 30L);
            }
        });
        saturated.execute(new Runnable() {
            @Override
            public void run() {
                // 占住唯一队列位
            }
        });
        executorField.set(cache, saturated);
        try {
            long startedAt = System.nanoTime();
            Entry entry = cache.request("http://127.0.0.1:1/saturated.png", null);
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            Assert.assertEquals("队满应落为失败而不是排队等待", Status.FAILED, entry.getStatus());
            Assert.assertTrue("请求不得阻塞在缓存内，实测 " + elapsedMs + "ms", elapsedMs < 2000L);
        } finally {
            executorField.set(cache, original);
            occupyThread.countDown();
            saturated.shutdownNow();
        }
    }

    /** 入队尚未返回时并发 clear：clear 返回就必须释放队列位，不能等提交线程事后补绑句柄。 */
    @Test
    public void clearDuringSubmissionReleasesQueuedTaskBeforeReturning() throws Exception {
        final AtomicInteger httpCalls = new AtomicInteger();
        final String url = startServer(new Responder() {
            @Override
            public void respond(HttpExchange exchange) throws IOException {
                httpCalls.incrementAndGet();
                sendPng(exchange, 200);
            }
        });
        Field executorField = DocumentRemoteImageCache.class.getDeclaredField("executorService");
        executorField.setAccessible(true);
        ExecutorService original = (ExecutorService) executorField.get(cache);
        final CountDownLatch holdWorker = new CountDownLatch(1);
        final CountDownLatch submitted = new CountDownLatch(1);
        final CountDownLatch returnFromExecute = new CountDownLatch(1);
        final ThreadPoolExecutor blocked = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1)) {
            @Override
            public void execute(Runnable command) {
                super.execute(command);
                if (command instanceof FutureTask<?>) {
                    submitted.countDown();
                    awaitQuietly(returnFromExecute, 5L);
                }
            }
        };
        blocked.execute(new Runnable() {
            @Override
            public void run() {
                awaitQuietly(holdWorker, 30L);
            }
        });
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        final AtomicReference<Entry> requested = new AtomicReference<Entry>();
        final AtomicInteger queuedAfterClear = new AtomicInteger(-1);
        Thread requester = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    requested.set(cache.request(url, null));
                } catch (Throwable error) {
                    failure.set(error);
                }
            }
        }, "remote-image-submit-window");
        Thread clearer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    cache.clear();
                    queuedAfterClear.set(blocked.getQueue().size());
                } catch (Throwable error) {
                    failure.set(error);
                }
            }
        }, "remote-image-clear-window");
        executorField.set(cache, blocked);
        try {
            requester.start();
            Assert.assertTrue("任务已入队，但 execute 尚未返回", submitted.await(5L, TimeUnit.SECONDS));
            clearer.start();
            // 正确实现会等待提交临界区；旧实现会提前返回并留下无句柄的队列任务。
            awaitBlockedOrFinished(clearer);
            returnFromExecute.countDown();
            requester.join(5000L);
            clearer.join(5000L);
            Assert.assertFalse("提交线程必须结束", requester.isAlive());
            Assert.assertFalse("清空线程必须结束", clearer.isAlive());
            Assert.assertNull("交错提交与清空不得抛异常", failure.get());
            Assert.assertEquals("clear 返回时旧任务必须已经出队", 0, queuedAfterClear.get());
            Assert.assertTrue("旧条目必须已弃用", requested.get().isAbandoned());

            final CountDownLatch completed = new CountDownLatch(1);
            Entry fresh = cache.request(url, new Runnable() {
                @Override
                public void run() {
                    completed.countDown();
                }
            });
            Assert.assertEquals("工作线程仍占用时，新代也应取得队列位", Status.LOADING, fresh.getStatus());
            holdWorker.countDown();
            Assert.assertTrue("新代请求应完成", completed.await(5L, TimeUnit.SECONDS));
            Assert.assertEquals("旧代不得发起网络请求", 1, httpCalls.get());
        } finally {
            returnFromExecute.countDown();
            requester.join(5000L);
            clearer.join(5000L);
            cache.clear();
            executorField.set(cache, original);
            holdWorker.countDown();
            blocked.shutdownNow();
        }
    }

    /**
     * clear 必须真正释放被取消旧任务占用的排队位。
     *
     * <p>{@code Future.cancel(false)} 只把任务标记为已取消，ThreadPoolExecutor 的队列位仍被占着；
     * 工作线程被慢下载占住时，这些「取消不掉的格子」会把有界队列吃满，clear 之后的新请求会被
     * 直接拒绝。本用例用注入的饱和池（1 工作线程 + 2 队列位）确定性地占满队列：clear 后<b>不释放
     * 工作线程</b>就发新请求，新请求必须立刻拿到队列位，并在工作线程释放后正常完成。</p>
     */
    @Test
    public void clearReleasesQueuedCapacityForNextGenerationRequest() throws Exception {
        final String url = startServer(new Responder() {
            @Override
            public void respond(HttpExchange exchange) throws IOException {
                sendPng(exchange, 200);
            }
        });

        Field executorField = DocumentRemoteImageCache.class.getDeclaredField("executorService");
        executorField.setAccessible(true);
        ExecutorService original = (ExecutorService) executorField.get(cache);
        final CountDownLatch holdWorker = new CountDownLatch(1);
        ThreadPoolExecutor bounded = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(2), new ThreadPoolExecutor.AbortPolicy());
        bounded.execute(new Runnable() {
            @Override
            public void run() {
                awaitQuietly(holdWorker, 30L);
            }
        });
        executorField.set(cache, bounded);
        try {
            final AtomicInteger oldCallbacks = new AtomicInteger();
            Entry first = cache.request(url + "?q=1", new Runnable() {
                @Override
                public void run() {
                    oldCallbacks.incrementAndGet();
                }
            });
            Entry second = cache.request(url + "?q=2", null);
            Assert.assertEquals("旧任务应已排入队列", Status.LOADING, first.getStatus());
            Assert.assertEquals("旧任务应已排入队列", Status.LOADING, second.getStatus());

            Entry overflow = cache.request(url + "?q=3", null);
            Assert.assertEquals("队列占满时新请求落为临时失败", Status.FAILED, overflow.getStatus());

            cache.clearForTesting();

            final AtomicInteger freshCallbacks = new AtomicInteger();
            final CountDownLatch freshCallback = new CountDownLatch(1);
            Entry fresh = cache.request(url + "?q=4", new Runnable() {
                @Override
                public void run() {
                    freshCallbacks.incrementAndGet();
                    freshCallback.countDown();
                }
            });
            Assert.assertEquals("clear 释放队列位后，新请求必须能入队而不是被拒绝",
                    Status.LOADING, fresh.getStatus());

            holdWorker.countDown();
            awaitStatus(fresh, Status.LOADED);
            Assert.assertTrue("新一代成功应回调", freshCallback.await(5L, TimeUnit.SECONDS));
            Assert.assertNotNull("新一代请求应正常完成并拿到位图", fresh.getImage());
            Assert.assertEquals("新一代成功应回调一次", 1, freshCallbacks.get());
            Assert.assertNull("旧任务不得发布位图", first.getImage());
            Assert.assertEquals("旧任务不得回调", 0, oldCallbacks.get());
        } finally {
            executorField.set(cache, original);
            holdWorker.countDown();
            bounded.shutdownNow();
        }
    }

    /**
     * clear 之后同 URL 的新一代请求正常完成，而旧一代任务既不发布结果也不回调。
     *
     * <p>与「在飞任务失效」用例的区别：这里在 clear 之后<b>再次请求同一 URL</b>，验证新代条目
     * 不是被旧任务的结果或状态污染，而是走完整的提交 / 下载 / 发布链拿到自己的结果。</p>
     */
    @Test
    public void newGenerationCompletesWhileClearedGenerationStaysSilent() throws Exception {
        final CountDownLatch arrived = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        String url = startServer(new Responder() {
            @Override
            public void respond(HttpExchange exchange) throws IOException {
                arrived.countDown();
                awaitQuietly(release, 5L);
                sendPng(exchange, 200);
            }
        });
        final AtomicInteger oldCallbacks = new AtomicInteger();
        Entry oldEntry = cache.request(url, new Runnable() {
            @Override
            public void run() {
                oldCallbacks.incrementAndGet();
            }
        });
        Assert.assertTrue("旧任务应已开始", arrived.await(5L, TimeUnit.SECONDS));

        cache.clearForTesting();
        release.countDown();

        final AtomicInteger newCallbacks = new AtomicInteger();
        final CountDownLatch newCallback = new CountDownLatch(1);
        Entry newEntry = cache.request(url, new Runnable() {
            @Override
            public void run() {
                newCallbacks.incrementAndGet();
                newCallback.countDown();
            }
        });
        Assert.assertNotSame("清空后同 URL 必须创建新一代条目", oldEntry, newEntry);
        awaitStatus(newEntry, Status.LOADED);
        Assert.assertTrue("新一代成功应回调", newCallback.await(5L, TimeUnit.SECONDS));
        Assert.assertNotNull("新一代应拿到位图", newEntry.getImage());
        Assert.assertEquals("新一代成功回调一次", 1, newCallbacks.get());
        Assert.assertNull("旧一代不得发布位图", oldEntry.getImage());
        Assert.assertNotEquals("旧一代不得进入 LOADED", Status.LOADED, oldEntry.getStatus());
        Assert.assertEquals("旧一代不得回调", 0, oldCallbacks.get());
    }

    /** 清空扫掠已结束、map.clear 尚未执行时插入新 URL，不能留下未弃用的孤儿条目。 */
    @Test
    public void concurrentInsertAndClearLeaveNoUnabandonedOrphan() throws Exception {
        Field entriesField = DocumentRemoteImageCache.class.getDeclaredField("entries");
        entriesField.setAccessible(true);
        Object original = entriesField.get(cache);
        final CountDownLatch beforeMapClear = new CountDownLatch(1);
        final CountDownLatch finishClear = new CountDownLatch(1);
        final Map<String, Entry> gatedEntries = new ConcurrentHashMap<String, Entry>() {
            @Override
            public void clear() {
                beforeMapClear.countDown();
                awaitQuietly(finishClear, 5L);
                super.clear();
            }
        };
        final String url = startServer(new Responder() {
            @Override
            public void respond(HttpExchange exchange) throws IOException {
                sendPng(exchange, 200);
            }
        });
        final AtomicReference<Entry> requested = new AtomicReference<Entry>();
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        final CountDownLatch completed = new CountDownLatch(1);
        Thread clearer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    cache.clear();
                } catch (Throwable error) {
                    failure.set(error);
                }
            }
        }, "remote-image-clear-map-window");
        Thread requester = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    requested.set(cache.request(url, new Runnable() {
                        @Override
                        public void run() {
                            completed.countDown();
                        }
                    }));
                } catch (Throwable error) {
                    failure.set(error);
                }
            }
        }, "remote-image-insert-map-window");
        entriesField.set(cache, gatedEntries);
        try {
            clearer.start();
            Assert.assertTrue("扫掠结束后停在实际清表之前", beforeMapClear.await(5L, TimeUnit.SECONDS));
            requester.start();
            // 插入必须等清表完成；无归属锁时 request 会先返回，随后条目被清表移走却未弃用。
            awaitBlockedOrFinished(requester);
            finishClear.countDown();
            clearer.join(5000L);
            requester.join(5000L);
            Assert.assertFalse("清空必须结束", clearer.isAlive());
            Assert.assertFalse("请求必须结束", requester.isAlive());
            Assert.assertNull("并发清空与插入不得抛异常", failure.get());
            Entry entry = requested.get();
            Assert.assertNotNull("request 必须返回条目", entry);
            Assert.assertSame("扫掠之后插入的条目必须属于新代", entry, gatedEntries.get(url));
            Assert.assertFalse("新代条目不得被旧清空弃用", entry.isAbandoned());
            Assert.assertTrue("新代请求应正常完成", completed.await(5L, TimeUnit.SECONDS));
        } finally {
            finishClear.countDown();
            clearer.join(5000L);
            requester.join(5000L);
            cache.clear();
            entriesField.set(cache, original);
        }
    }

    // ======================= 夹具 =======================

    /** 服务端响应策略 */
    private interface Responder {
        void respond(HttpExchange exchange) throws IOException;
    }

    /** 启动本机回环上的单路径测试服务，返回请求 URL */
    private String startServer(final Responder responder) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serverExecutor = Executors.newCachedThreadPool(new java.util.concurrent.ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "remote-image-test-http");
                thread.setDaemon(true);
                return thread;
            }
        });
        server.setExecutor(serverExecutor);
        server.createContext("/img.png", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                responder.respond(exchange);
            }
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/img.png";
    }

    private void sendPng(HttpExchange exchange, int status) throws IOException {
        if (status != 200) {
            exchange.sendResponseHeaders(status, -1L);
            exchange.close();
            return;
        }
        exchange.getResponseHeaders().add("Content-Type", "image/png");
        exchange.sendResponseHeaders(200, pngBytes.length);
        OutputStream body = exchange.getResponseBody();
        body.write(pngBytes);
        body.flush();
        exchange.close();
    }

    private static byte[] encodePng() throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xFF123456);
        image.setRGB(1, 1, 0xFFABCDEF);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ImageIO.write(image, "png", buffer);
        return buffer.toByteArray();
    }

    private static void awaitStatus(Entry entry, Status expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5L);
        while (System.nanoTime() < deadline) {
            if (entry.getStatus() == expected) {
                return;
            }
            Thread.sleep(5L);
        }
        Assert.assertEquals("等待条目进入期望状态超时", expected, entry.getStatus());
    }

    /** 把重试退避时钟拨到已过期，避免测试真的等满 2s 退避；不改变生产语义。 */
    private static void expireBackoff(Entry entry) throws Exception {
        Field field = Entry.class.getDeclaredField("nextRetryAtNanos");
        field.setAccessible(true);
        synchronized (entry) {
            field.setLong(entry, 0L);
        }
    }

    /** 等交错线程到达监视器入口或完成；超时仅防止测试挂死，不靠固定睡眠制造竞态。 */
    private static void awaitBlockedOrFinished(Thread thread) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5L);
        while (thread.isAlive() && thread.getState() != Thread.State.BLOCKED) {
            Assert.assertTrue("交错线程未到达预期窗口", System.nanoTime() < deadline);
            Thread.sleep(1L);
        }
    }

    private static void awaitQuietly(CountDownLatch latch, double seconds) {
        try {
            latch.await((long) (seconds * 1000D), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
