package blocamlimb.nhx100218.modernui.uilib.client.motd;

import com.google.gson.JsonArray;

/**
 * Client-side carrier for the exact MOTD colours sent in the status JSON.
 *
 * <p>1.7.10 ignores the custom field while deserializing the vanilla response;
 * this interface lets ModernUI's deserializer and {@code FMLClientHandler}
 * bridge carry it to {@code ServerData}.</p>
 */
public interface ModernStatusResponse {

    void modernui$setLegacyColors(JsonArray colors);

    JsonArray modernui$getLegacyColors();
}
