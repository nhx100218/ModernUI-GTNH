package blocamlimb.nhx100218.modernui.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 可变配置的默认实现
 */
public class DefaultMutableConfig implements MutableConfig {

    private static final Logger LOG = LogManager.getLogger("ModernUI/DefaultMutableConfig");

    private final ConfigFormat format;
    private final ConfigSource source;
    private final Map<ConfigFormat, ConfigWriter> writers;
    private final List<ConfigChangeListener> listeners;

    private Map<String, Object> data;
    private boolean dirty;
    /** 加载期原始带注释 ConfigNode 树：未修改时直接 round-trip，已修改时作为注释归位来源（只读） */
    private ConfigNode originalNode;
    /**
     * 本次会话中已被显式删除（{@link #remove(String)} / {@link #clear()}）的加载期快照子树。
     *
     * <p>注释按「路径位置」归位，因此「这个位置被删过」必须被记住：否则同键重建或 clear 后重建
     * 会把已删除子树的注释重新贴到新值上，与「新出现的键没有注释」自相矛盾。这里只按 identity
     * 记录被删除的快照节点，重建时同路径命中即视为该位置的快照注释已失效；不另存注释副本，
     * 也不修改只读的 {@link #originalNode}。</p>
     */
    private final Set<ConfigNode> revokedOriginals =
            Collections.newSetFromMap(new IdentityHashMap<ConfigNode, Boolean>());
    /** 是否仍处于未修改的原始状态：true 时 asImmutable/save 直接用 originalNode，保留注释 */
    private boolean pristine;

    /**
     * 从已有配置节点创建可变配置
     * 
     * @param node 配置节点
     * @param format 配置格式
     * @param source 配置源（可为 null）
     */
    public DefaultMutableConfig(ConfigNode node, ConfigFormat format, ConfigSource source) {
        this.format = format;
        this.source = source;
        this.writers = new HashMap<ConfigFormat, ConfigWriter>();
        this.listeners = new CopyOnWriteArrayList<ConfigChangeListener>();
        this.dirty = false;

        // 注册默认写入器
        writers.put(ConfigFormat.JSON, new JsonConfigWriter());
        writers.put(ConfigFormat.YAML, new YamlConfigWriter());

        // 转换为可变数据结构
        this.data = convertToMutableMap(node);
        // 保留原始带注释的 ConfigNode 树，未修改时 round-trip 可保留注释
        this.originalNode = node;
        this.pristine = true;
    }

    /**
     * 创建空的可变配置
     * 
     * @param format 配置格式
     * @param source 配置源（可为 null）
     */
    public DefaultMutableConfig(ConfigFormat format, ConfigSource source) {
        this(NullConfigNode.INSTANCE, format, source);
        this.data = new LinkedHashMap<String, Object>();
    }

    @Override
    public MutableConfig set(String path, Object value) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }

        String[] parts = path.split("\\.");
        Map<String, Object> current = data;

        // 导航到目标位置
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            Object next = current.get(part);

            if (!(next instanceof Map)) {
                // 创建中间节点（LinkedHashMap：与加载顺序一致，重建写回时不重排既有键）
                Map<String, Object> newMap = new LinkedHashMap<String, Object>();
                current.put(part, newMap);
                current = newMap;
            } else {
                current = (Map<String, Object>) next;
            }
        }

        // 设置值
        String key = parts[parts.length - 1];
        Object oldValue = current.get(key);
        Object convertedValue = convertValue(value);
        current.put(key, convertedValue);

        // 标记为已修改
        dirty = true;
        pristine = false;

        // 触发事件
        notifyListeners(new ConfigChangeEvent(path, oldValue, convertedValue, 
                ConfigChangeEvent.ChangeType.SET));

        return this;
    }

    @Override
    public MutableConfig remove(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }

        String[] parts = path.split("\\.");
        Map<String, Object> current = data;

        // 导航到目标位置
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            Object next = current.get(part);

            if (!(next instanceof Map)) {
                return this; // 路径不存在
            }
            current = (Map<String, Object>) next;
        }

        // 移除值
        String key = parts[parts.length - 1];
        // ★ 不能用「返回值是否非 null」判断是否真的发生了删除：显式 null 键（YAML/JSON 的 `key: null`）
        // 在 data 中存的就是 Java null，删除成功与删除不存在同样返回 null。漏判会让 pristine 仍为 true，
        // 之后 asImmutable/save 回退到带旧键的 originalNode（读到的还是「键存在」），dirty 与
        // REMOVE 事件也一并漏报。存在性只由 containsKey 判定，数据层与不可变快照才同步。
        // 删除不存在的键保持无副作用（不置 dirty、不发事件）。
        boolean existed = current.containsKey(key);
        Object oldValue = current.remove(key);

        if (existed) {
            dirty = true;
            pristine = false;
            revokeOriginalPath(path);
            notifyListeners(new ConfigChangeEvent(path, oldValue, null, 
                    ConfigChangeEvent.ChangeType.REMOVE));
        }

        return this;
    }

    @Override
    public MutableConfig clear() {
        Map<String, Object> oldData = this.data;
        this.data = new LinkedHashMap<String, Object>();
        dirty = true;
        pristine = false;
        if (originalNode != null) {
            revokedOriginals.add(originalNode);
        }

        notifyListeners(new ConfigChangeEvent("", oldData, null, 
                ConfigChangeEvent.ChangeType.CLEAR));

        return this;
    }

    @Override
    public void save() throws ConfigException {
        if (source == null) {
            throw new ConfigException("No source file associated with this config");
        }
        saveTo(source);
    }

    @Override
    public void saveTo(ConfigSource target) throws ConfigException {
        ConfigWriter writer = writers.get(format);
        if (writer == null) {
            throw new ConfigException("No writer registered for format: " + format);
        }

        // 转换为不可变节点并写入
        // 未修改（pristine）时直接用原始带注释的 ConfigNode，保留注释 round-trip；
        // 已修改时从 data 重建，但按路径把原始树上的注释元数据归位到新树（见
        // convertToImmutableNode(Object, ConfigNode)），未变路径的注释同样保留。
        ConfigNode node = pristine && originalNode != null ? originalNode
                : convertToImmutableNode(data, originalNode);
        writer.write(node, target);

        dirty = false;
    }

    @Override
    public void reload() throws ConfigException {
        if (source == null) {
            throw new ConfigException("No source file associated with this config");
        }

        ConfigLoader loader = Config.getLoader(format);
        if (loader == null) {
            throw new ConfigException("No loader registered for format: " + format);
        }

        ConfigNode node = loader.load(source);
        this.data = convertToMutableMap(node);
        this.originalNode = node;
        this.revokedOriginals.clear();
        this.pristine = true;
        this.dirty = false;

        notifyListeners(new ConfigChangeEvent("", null, data, 
                ConfigChangeEvent.ChangeType.RELOAD));
    }

    @Override
    public ConfigFormat getFormat() {
        return format;
    }

    @Override
    public ConfigSource getSource() {
        return source;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void markClean() {
        this.dirty = false;
    }

    @Override
    public void addChangeListener(ConfigChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeChangeListener(ConfigChangeListener listener) {
        listeners.remove(listener);
    }

    @Override
    public ConfigNode asImmutable() {
        // 未修改时返回原始带注释的 ConfigNode，保留注释；已修改时从 data 重建，
        // 并按路径归位原始树上的注释（未变路径的注释不因无关修改而丢失）。
        if (pristine && originalNode != null) {
            return originalNode;
        }
        return convertToImmutableNode(data, originalNode);
    }

    // ConfigNode 接口实现

    @Override
    public NodeType getType() {
        return NodeType.MAP;
    }

    @Override
    public boolean isNull() {
        return data == null || data.isEmpty();
    }

    @Override
    public String asString() {
        return data.toString();
    }

    @Override
    public int asInt() throws ConfigException {
        throw new ConfigException("Cannot convert root config to int");
    }

    @Override
    public long asLong() throws ConfigException {
        throw new ConfigException("Cannot convert root config to long");
    }

    @Override
    public double asDouble() throws ConfigException {
        throw new ConfigException("Cannot convert root config to double");
    }

    @Override
    public boolean asBoolean() throws ConfigException {
        throw new ConfigException("Cannot convert root config to boolean");
    }

    @Override
    public List<ConfigNode> asList() {
        return null;
    }

    @Override
    public Map<String, ConfigNode> asMap() {
        Map<String, ConfigNode> result = new LinkedHashMap<String, ConfigNode>();
        // clear 撤销的是整棵加载期快照；asMap 同样必须阻止其子位置注释复活。
        Map<String, ConfigNode> originals = originalNode != null && !revokedOriginals.contains(originalNode)
                && originalNode.getType() == NodeType.MAP ? originalNode.asMap() : null;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            result.put(entry.getKey(), convertToImmutableNode(entry.getValue(),
                    originals != null ? originals.get(entry.getKey()) : null));
        }
        return result;
    }

    @Override
    public ConfigNode get(String path) {
        if (path == null || path.isEmpty()) {
            return this;
        }

        String[] parts = path.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (!(current instanceof Map)) {
                return NullConfigNode.INSTANCE;
            }

            Map<String, Object> map = (Map<String, Object>) current;
            current = map.get(part);

            if (current == null) {
                return NullConfigNode.INSTANCE;
            }
        }

        return convertToImmutableNode(current);
    }

    @Override
    public ConfigNode get(int index) {
        return NullConfigNode.INSTANCE;
    }

    @Override
    public boolean has(String path) {
        return !get(path).isNull();
    }

    @Override
    public int asInt(int defaultValue) {
        return defaultValue;
    }

    @Override
    public long asLong(long defaultValue) {
        return defaultValue;
    }

    @Override
    public double asDouble(double defaultValue) {
        return defaultValue;
    }

    @Override
    public boolean asBoolean(boolean defaultValue) {
        return defaultValue;
    }

    @Override
    public String asString(String defaultValue) {
        return defaultValue;
    }

    // 辅助方法

    /**
     * 转换为可变映射表
     * 
     * @param node 配置节点
     * @return 可变映射表
     */
    private Map<String, Object> convertToMutableMap(ConfigNode node) {
        if (node.getType() != NodeType.MAP) {
            return new LinkedHashMap<String, Object>();
        }

        Map<String, ConfigNode> sourceMap = node.asMap();
        if (sourceMap == null) {
            return new LinkedHashMap<String, Object>();
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, ConfigNode> entry : sourceMap.entrySet()) {
            result.put(entry.getKey(), convertToMutableValue(entry.getValue()));
        }

        return result;
    }

    /**
     * 转换为可变值
     * 
     * @param node 配置节点
     * @return 可变值
     */
    private Object convertToMutableValue(ConfigNode node) {
        if (node.isNull()) {
            return null;
        }

        switch (node.getType()) {
            case STRING:
                return node.asString();

            case NUMBER:
                Object raw = node instanceof AbstractConfigNode
                        ? ((AbstractConfigNode) node).getRawValue()
                        : null;
                if (raw instanceof Byte || raw instanceof Short
                        || raw instanceof Integer || raw instanceof Long) {
                    return Long.valueOf(((Number) raw).longValue());
                }
                if (raw instanceof Float || raw instanceof Double) {
                    return Double.valueOf(((Number) raw).doubleValue());
                }
                if (raw instanceof Number) {
                    // BigInteger/LazilyParsedNumber 等不可安全缩窄的实现保持原 Number。
                    return raw;
                }
                try {
                    return Double.valueOf(node.asDouble());
                } catch (ConfigException e) {
                    return Double.valueOf(0.0D);
                }

            case BOOLEAN:
                return node.asBoolean(false);

            case LIST:
                List<ConfigNode> sourceList = node.asList();
                List<Object> resultList = new ArrayList<Object>();
                if (sourceList != null) {
                    for (ConfigNode item : sourceList) {
                        resultList.add(convertToMutableValue(item));
                    }
                }
                return resultList;

            case MAP:
                return convertToMutableMap(node);

            default:
                return null;
        }
    }

    /**
     * 转换为不可变节点
     * 
     * @param value 值
     * @return 配置节点
     */
    private ConfigNode convertToImmutableNode(Object value) {
        return convertToImmutableNode(value, null);
    }

    /**
     * 把可变值重建为不可变节点，并按<b>路径</b>从原始树上归位注释元数据。
     *
     * <p><b>注释归属口径</b>：注释属于「加载期快照中的位置」而不是节点实例——Map 的键、List 的
     * 下标。只改某个值再保存时，未变路径上的块注释 / 内联注释 / collection 末尾注释原样保留；
     * 被删除的键其注释随位置消失；List 元素按下标对齐，注释固定在原下标，不跟随元素值移动；
     * 替换 List 时只对当前仍存在的下标归位。快照中从未出现的路径（新键、新下标）没有注释。</p>
     *
     * <p><b>删除过的位置不复活</b>：{@link #remove(String)} / {@link #clear()} 会把对应的加载期
     * 快照子树记入 {@link #revokedOriginals}，之后同一路径重建（remove 后同键再 set、clear 后
     * 重建）按全新位置处理，不带回旧注释；否则「被删除的键其注释自然消失」与「新出现的键没有
     * 注释」会在同一份快照上互相矛盾。</p>
     *
     * <p><b>为什么按路径复制而不是复用未变子树</b>：{@code data} 是修改之后的唯一事实源，
     * 直接挂原始 subtree 会让「已修改树」与 {@code originalNode} 共享可变节点（副本语义破裂）。
     * 本方法只在既有 ConfigNode 模型内搬运注释元数据，不引入并存的「注释数据库」，
     * 因而不存在两份注释互相失配的失效问题；{@code originalNode} 仍是加载期快照，
     * 整个生命周期只读。</p>
     *
     * @param value    可变值
     * @param original 同路径的原始节点（可为 null：新键/新下标/已删除位置，无注释可归位）
     * @return 配置节点
     */
    private ConfigNode convertToImmutableNode(Object value, ConfigNode original) {
        if (original != null && revokedOriginals.contains(original)) {
            // 该位置被本次会话显式删除过：快照注释已失效，重建出的新值按新位置处理
            original = null;
        }
        if (value instanceof ConfigNode) {
            // 防御分支：调用方自带的节点原样返回，绝不往外部对象上写注释
            return (ConfigNode) value;
        }

        if (value == null) {
            // 显式 null 值要承载注释时必须用独立实例：NullConfigNode.INSTANCE 是共享单例
            return copyComments(hasComment(original) ? NullConfigNode.create() : NullConfigNode.INSTANCE, original);
        }

        if (value instanceof String) {
            return copyComments(new StringConfigNode((String) value), original);
        }

        if (value instanceof Number) {
            return copyComments(new NumberConfigNode((Number) value), original);
        }

        if (value instanceof Boolean) {
            return copyComments(new BooleanConfigNode((Boolean) value), original);
        }

        if (value instanceof List) {
            List<?> list = (List<?>) value;
            List<ConfigNode> originals = original != null && original.getType() == NodeType.LIST
                    ? original.asList() : null;
            List<ConfigNode> nodes = new ArrayList<ConfigNode>(list.size());
            for (int i = 0; i < list.size(); i++) {
                ConfigNode childOriginal = originals != null && i < originals.size() ? originals.get(i) : null;
                nodes.add(convertToImmutableNode(list.get(i), childOriginal));
            }
            return copyComments(new ListConfigNode(nodes), original);
        }

        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            Map<String, ConfigNode> originals = original != null && original.getType() == NodeType.MAP
                    ? original.asMap() : null;
            // 保持加载顺序：LinkedHashMap 让「改一个值再保存」不重排未变路径，
            // 注释（随路径归位）因此仍贴在原位，而不是散落在 HashMap 的迭代顺序里。
            Map<String, ConfigNode> nodes = new LinkedHashMap<String, ConfigNode>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                nodes.put(key, convertToImmutableNode(entry.getValue(),
                        originals != null ? originals.get(key) : null));
            }
            return copyComments(new MapConfigNode(nodes), original);
        }

        // 默认转为字符串
        return copyComments(new StringConfigNode(String.valueOf(value)), original);
    }

    /**
     * 把原始节点上的注释元数据复制到重建出的节点。
     *
     * <p>末尾注释（end comment）只属于 collection，标量节点不承接——否则 Writer 会往标量行挂
     * 一条本不属于它的尾部注释。</p>
     *
     * @param target   重建出的节点
     * @param original 同路径的原始节点，可为 null
     * @return target（便于链式返回）
     */
    private static ConfigNode copyComments(ConfigNode target, ConfigNode original) {
        if (original == null || !(target instanceof AbstractConfigNode)) {
            return target;
        }
        AbstractConfigNode abs = (AbstractConfigNode) target;
        if (original.getBlockComment() != null) {
            abs.setBlockComment(original.getBlockComment());
        }
        if (original.getInlineComment() != null) {
            abs.setInlineComment(original.getInlineComment());
        }
        if ((target.getType() == NodeType.MAP || target.getType() == NodeType.LIST)
                && original.getEndComment() != null) {
            abs.setEndComment(original.getEndComment());
        }
        return target;
    }

    /** 原始节点是否携带任何注释元数据（决定显式 null 值是否需要独立空节点实例） */
    private static boolean hasComment(ConfigNode original) {
        return original != null && (original.getBlockComment() != null
                || original.getInlineComment() != null
                || original.getEndComment() != null);
    }

    /**
     * 把加载期快照中 {@code path} 位置的子树标记为「已删除」。
     *
     * <p>只按 identity 记下快照节点，不复制也不改动它；重建时同路径命中即视为快照注释失效
     * （见 {@link #convertToImmutableNode(Object, ConfigNode)}）。路径在快照中不存在则无事发生。</p>
     *
     * @param path 已确认删除的配置路径
     */
    private void revokeOriginalPath(String path) {
        ConfigNode node = originalNode;
        if (node == null) {
            return;
        }
        for (String part : path.split("\\.")) {
            if (node.getType() != NodeType.MAP) {
                return;
            }
            Map<String, ConfigNode> map = node.asMap();
            node = map == null ? null : map.get(part);
            if (node == null) {
                return;
            }
        }
        revokedOriginals.add(node);
    }

    /**
     * 转换值（用于 set 方法）
     * 
     * @param value 原始值
     * @return 转换后的值
     */
    private Object convertValue(Object value) {
        if (value instanceof ConfigNode) {
            return convertToMutableValue((ConfigNode) value);
        }
        return value;
    }

    /**
     * 通知监听器
     * 
     * @param event 变更事件
     */
    private void notifyListeners(ConfigChangeEvent event) {
        for (ConfigChangeListener listener : listeners) {
            try {
                listener.onConfigChanged(event);
            } catch (Exception e) {
                // 隔离单个监听器异常，不影响其余监听器；走日志系统而非 stderr，便于过滤与归档
                LOG.error("[DefaultMutableConfig] 配置变更监听器抛出异常，已隔离：event={}", event, e);
            }
        }
    }

    /**
     * 注册配置写入器
     * 
     * @param writer 写入器
     */
    public void registerWriter(ConfigWriter writer) {
        if (writer != null) {
            writers.put(writer.getFormat(), writer);
        }
    }
}
