package blocamlimb.nhx100218.modernui.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.network.ServerStatusResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ServerMotdServiceTest {

    private File configRoot;

    @Before
    public void setUp() throws Exception {
        configRoot = Files.createTempDirectory("modernui-motd").toFile();
        File mini = new File(configRoot, "MiniMOTD");
        assertTrue(mini.mkdirs());
        Files.write(new File(mini, "main.conf").toPath(),
                ("motd-enabled = true\n"
                        + "icon-enabled = false\n"
                        + "motds = [\n"
                        + "  {\n"
                        + "    line1 = \"<gradient:#ff0000:#0000ff>AB</gradient>\"\n"
                        + "    line2 = \"\"\n"
                        + "  }\n"
                        + "]\n").getBytes(StandardCharsets.UTF_8));
        MiniMotdConfig.initialize(configRoot);
        ServerMotdService.setEnabledOverride(Boolean.TRUE);
        ServerMotdService.invalidate();
    }

    @After
    public void tearDown() {
        ServerMotdService.clearEnabledOverride();
        MiniMotdConfig.initialize(configRoot == null ? new File(".") : configRoot);
        ServerMotdService.invalidate();
    }

    @Test
    public void modernProtocolCarriesExactRgbComponent() {
        ServerStatusResponse response = ServerMotdService.apply(null, new ServerStatusResponse(), 800);
        JsonObject description = ServerMotdService.directDescription(response);
        assertNotNull("1.16+ must receive a component description", description);
        JsonArray extra = description.getAsJsonArray("extra");
        assertEquals(2, extra.size());
        assertEquals("A", extra.get(0).getAsJsonObject().get("text").getAsString());
        assertEquals("#ff0000", extra.get(0).getAsJsonObject().get("color").getAsString());
        assertEquals("B", extra.get(1).getAsJsonObject().get("text").getAsString());
        assertEquals("#0000ff", extra.get(1).getAsJsonObject().get("color").getAsString());
        assertTrue("legacy fallback must stay available",
                response.func_151317_a().getUnformattedText().contains("\u00a7r"));
    }

    @Test
    public void legacyProtocolKeepsLegacyDescriptionOnly() {
        ServerStatusResponse response = ServerMotdService.apply(null, new ServerStatusResponse(), 5);
        assertNull(ServerMotdService.directDescription(response));
        assertTrue(response.func_151317_a().getUnformattedText().contains("\u00a7r"));
    }

    @Test
    public void legacyPingUsesMiniMotdContent() {
        String legacy = ServerMotdService.legacyMotd(null);
        assertTrue(legacy.startsWith(LegacyMotdText.MARKER));
        assertTrue(legacy.contains("\u00a7"));
        assertTrue(legacy.contains("A"));
        assertTrue(legacy.contains("B"));
    }

    @Test
    public void disabledMiniMotdDoesNotEnableYamlFallback() throws Exception {
        File mini = new File(configRoot, "MiniMOTD");
        Files.write(new File(mini, "main.conf").toPath(),
                ("motd-enabled = false\nmotds = [{ line1 = \"&bX\", line2 = \"\" }]\n")
                        .getBytes(StandardCharsets.UTF_8));
        MiniMotdConfig.invalidate();
        ServerMotdService.clearEnabledOverride();
        assertFalse(ServerMotdService.isEnabled());
    }
}
