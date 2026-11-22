package blocamlimb.nhx100218.modernui.uilib.client.motd;

import com.google.gson.JsonArray;

/**
 * Exact MOTD colours attached to a server-list entry.
 *
 * <p>The server list entry only sees {@code ServerData}, so the custom status
 * field is copied here by the {@code FMLClientHandler} bridge.</p>
 */
public interface ModernServerData {

    void modernui$setLegacyMotdColors(JsonArray colors);

    JsonArray modernui$getLegacyMotdColors();
}
