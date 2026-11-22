package blocamlimb.nhx100218.modernui.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Implemented by the status packet mixin so per-ping colour data travels with
 * that specific packet instead of relying on a response map.
 */
public interface RgbStatusPacket {

    void modernui$setRgbDescription(JsonObject description);

    /**
     * Exact per-glyph colours for a legacy (1.7.10) response. Sent as a
     * separate JSON field that vanilla clients ignore.
     */
    void modernui$setLegacyColors(JsonArray colors);
}
