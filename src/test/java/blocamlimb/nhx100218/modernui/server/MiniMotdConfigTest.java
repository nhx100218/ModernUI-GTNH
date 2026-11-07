package blocamlimb.nhx100218.modernui.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MiniMotdConfigTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void readsMiniMotdMainConf() throws Exception {
        File configDir = temporaryFolder.newFolder("config");
        File miniDir = new File(configDir, "MiniMOTD");
        assertTrue(miniDir.mkdirs());
        File main = new File(miniDir, "main.conf");
        Files.write(main.toPath(), ("motd-enabled = true\n"
                + "icon-enabled = false\n"
                + "motds = [\n"
                + "  {\n"
                + "    line1 = \"&bHello\"\n"
                + "    line2 = \"&7World\"\n"
                + "    icon = \"random\"\n"
                + "  }\n"
                + "]\n").getBytes(StandardCharsets.UTF_8));

        MiniMotdConfig.initialize(configDir);
        MiniMotdConfig.Data data = MiniMotdConfig.current();

        assertNotNull(data);
        assertTrue(data.enabled);
        assertEquals(1, data.motds.size());
        assertEquals("&bHello", data.motds.get(0).line1);
        assertTrue(LegacyMotdText.hasVisibleText(LegacyMotdText.render(data.motds.get(0).line1)));
    }
}
