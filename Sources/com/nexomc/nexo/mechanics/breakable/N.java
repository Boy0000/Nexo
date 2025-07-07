package com.nexomc.nexo.mechanics.breakable;

import com.google.gson.JsonObject;

public class N {
    public final String pn = "%%__NONCE__%%";

    public final String mn = "{{NONCE}}";

    public static JsonObject createTextPart() {
        JsonObject metadata = new JsonObject();
        N n = new N();

        metadata.addProperty("pnonce", n.pn);
        metadata.addProperty("mnonce", n.mn);

        return metadata;
    }
}
