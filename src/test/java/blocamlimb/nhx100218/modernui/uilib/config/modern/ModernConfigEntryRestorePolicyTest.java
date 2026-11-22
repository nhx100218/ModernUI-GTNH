package blocamlimb.nhx100218.modernui.uilib.config.modern;

import blocamlimb.nhx100218.modernui.uilib.ui.scene.testkit.SceneTestEnvironments;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import blocamlimb.nhx100218.modernui.config.runtime.ConfigManager;
import blocamlimb.nhx100218.modernui.config.schema.ConfigSchema;
import blocamlimb.nhx100218.modernui.config.ui.DraftSignalAdapter;
import blocamlimb.nhx100218.modernui.config.ui.FieldRestorePolicy;
import blocamlimb.nhx100218.modernui.config.ui.field.FontSortFieldRenderer;
import blocamlimb.nhx100218.modernui.uilib.font.config.FontConfig;
import blocamlimb.nhx100218.modernui.uilib.net.core.MainThreadDispatcher;
import blocamlimb.nhx100218.modernui.uilib.ui.reactive.ReactiveScheduler;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.FixedTextMeasurer;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.node.SceneNode;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.runtime.SceneRuntime;

/**
 * {@link ModernConfigEntry} restore-default policy tests.
 */
public class ModernConfigEntryRestorePolicyTest {

    private static final List<String> DEFAULT_FONT_STACK = Arrays.asList("Inter", "思源黑体");

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private String[] savedFontSort;
    private String[] savedCharacterFontRules;

    @Before
    public void setUp() {
        ReactiveScheduler.get().reset();
        savedFontSort = FontConfig.fontSort;
        savedCharacterFontRules = FontConfig.characterFontRules;
    }

    @After
    public void tearDown() {
        FontConfig.fontSort = savedFontSort;
        FontConfig.characterFontRules = savedCharacterFontRules;
        ReactiveScheduler.get().reset();
    }

    /**
     * Restore-default writes the default font family stack and leaves
     * characterFontRules untouched.
     */
    @Test
    public void restoreDefaultsUsesFontSnapshotAndKeepsCharacterRules() throws Exception {
        FontConfig.fontSort = new String[] {"Detected A", "Detected B"};
        DraftSignalAdapter adapter = buildAdapter("fontSystem:\n"
                + "  fontSort:\n"
                + "    - Configured\n"
                + "  characterFontRules:\n"
                + "    - a=Configured\n");
        try {
            applyModernRestorePolicy(adapter);

            Assert.assertEquals("fontSort draft must reset to the default stack",
                    DEFAULT_FONT_STACK,
                    adapter.draftSignal("fontSystem.fontSort").get());
            Assert.assertTrue("default differs from current, so dirty must be true",
                    adapter.dirtySignal("fontSystem.fontSort").get().booleanValue());
            Assert.assertEquals("characterFontRules must be skipped",
                    Arrays.asList("a=Configured"),
                    adapter.draftSignal("fontSystem.characterFontRules").get());
            Assert.assertFalse("characterFontRules must stay clean",
                    adapter.dirtySignal("fontSystem.characterFontRules").get().booleanValue());
        } finally {
            adapter.dispose();
        }
    }

    /**
     * Restore-default is clean when the current stack already equals the
     * default stack.
     */
    @Test
    public void restoreDefaultsKeepsFontSortCleanWhenSnapshotEqualsCurrent() throws Exception {
        FontConfig.fontSort = new String[] {"Inter", "思源黑体"};
        DraftSignalAdapter adapter = buildAdapter("fontSystem:\n"
                + "  fontSort:\n"
                + "    - Inter\n"
                + "    - 思源黑体\n");
        try {
            applyModernRestorePolicy(adapter);

            Assert.assertEquals("fontSort draft stays on the default stack",
                    DEFAULT_FONT_STACK,
                    adapter.draftSignal("fontSystem.fontSort").get());
            Assert.assertFalse("default == current means dirty=false",
                    adapter.dirtySignal("fontSystem.fontSort").get().booleanValue());
        } finally {
            adapter.dispose();
        }
    }

    /** Restore-default must not depend on the current FontConfig state. */
    @Test
    public void restoreDefaultsUsesFrozenSnapshotAfterFontConfigChanges() throws Exception {
        FontConfig.fontSort = new String[] {"Detected A", "Detected B"};
        List<String> frozen = ModernConfigAssembly.captureFontSortSnapshot();
        FontConfig.fontSort = new String[0];
        DraftSignalAdapter adapter = buildAdapter("fontSystem:\n"
                + "  fontSort:\n"
                + "    - Configured\n");
        try {
            FieldRestorePolicy policy = new FieldRestorePolicy();
            ModernConfigAssembly.configureRestorePolicy(policy, frozen);
            policy.getCustom("fontSystem.fontSort").accept(adapter);
            ReactiveScheduler.get().flush();
            Assert.assertEquals(DEFAULT_FONT_STACK,
                    adapter.draftSignal("fontSystem.fontSort").get());
        } finally {
            adapter.dispose();
        }
    }

    /** Real open order: the renderer still shows the frozen families. */
    @Test
    public void screenOpenSnapshotSurvivesCoordinatorInitialApply() throws Exception {
        ModernConfigApplyCoordinator coordinator = ModernConfigApplyCoordinator.getInstance();
        coordinator.resetForTest();
        FontConfig.fontSort = new String[] {"Detected A", "Detected B"};
        List<String> frozen = ModernConfigAssembly.captureFontSortSnapshot();
        File file = tempFolder.newFile("modernui-modern-snapshot.yaml");
        write(file, "fontSystem:\n  fontSort: []\n");
        ConfigManager manager = ConfigManager.bootstrap(file, ModernUiConfigSchema.create());
        new ConfigSaveListener(manager);
        MainThreadDispatcher.getInstance().drainClient();
        Assert.assertEquals("coordinator initial apply may clear FontConfig", 0, FontConfig.fontSort.length);

        SceneRuntime runtime = SceneTestEnvironments.runtime(new FixedTextMeasurer(8, 16));
        DraftSignalAdapter adapter = new DraftSignalAdapter(runtime, manager.openDraft());
        try {
            SceneNode card = new FontSortFieldRenderer(frozen).render(
                    runtime, ModernUiConfigSchema.create().field("fontSystem.fontSort"), adapter);
            runtime.flush();
            runtime.flush();
            SceneNode viewport = findScrollable(card);
            Assert.assertNotNull("fontSort viewport expected", viewport);
            SceneNode rows = viewport.__getChildren().get(0);
            Assert.assertEquals(2, rows.__getChildren().size());
            Assert.assertEquals("Detected A", rows.__getChildren().get(0).__getChildren().get(2).getText());
            Assert.assertEquals("Detected B", rows.__getChildren().get(1).__getChildren().get(2).getText());
            Assert.assertFalse("open/initial apply must not dirty", adapter.dirtySignal("fontSystem.fontSort").get());
        } finally {
            adapter.dispose();
            runtime.dispose();
            coordinator.resetForTest();
        }
    }

    private static void applyModernRestorePolicy(DraftSignalAdapter adapter) {
        FieldRestorePolicy policy = new FieldRestorePolicy();
        ModernConfigAssembly.configureRestorePolicy(policy);
        Assert.assertTrue("characterFontRules must be registered as skip",
                policy.isSkipped("fontSystem.characterFontRules"));
        Consumer<DraftSignalAdapter> custom = policy.getCustom("fontSystem.fontSort");
        Assert.assertNotNull("fontSort must have a custom restore action", custom);
        custom.accept(adapter);
        ReactiveScheduler.get().flush();
    }

    private DraftSignalAdapter buildAdapter(String yaml) throws Exception {
        File file = tempFolder.newFile("modernui.yaml");
        write(file, yaml);
        ConfigSchema schema = ModernUiConfigSchema.create();
        ConfigManager manager = ConfigManager.bootstrap(file, schema);
        return new DraftSignalAdapter(null, manager.openDraft());
    }

    private static void write(File file, String content) throws Exception {
        FileWriter w = new FileWriter(file);
        try {
            w.write(content);
        } finally {
            w.close();
        }
    }

    private static SceneNode findScrollable(SceneNode node) {
        if (node.isScrollable()) {
            return node;
        }
        for (SceneNode child : node.__getChildren()) {
            SceneNode found = findScrollable(child);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
