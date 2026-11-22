package blocamlimb.nhx100218.modernui.server;

import com.google.gson.JsonObject;

/**
 * Implemented by the status packet mixin so the RGB description chosen for a
 * ping travels with that specific packet instead of relying on a response map.
 */
public interface RgbStatusPacket {

    void modernui$setRgbDescription(JsonObject description);
}
