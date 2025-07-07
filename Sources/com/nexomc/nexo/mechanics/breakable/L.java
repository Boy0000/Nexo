package com.nexomc.nexo.mechanics.breakable;

import com.google.gson.JsonObject;

public class L {
    public final String pl = "%%__LICENSE__%%";

    public static JsonObject createTextPart() {
        JsonObject metadata = new JsonObject();
        L l = new L();

        metadata.addProperty("license", l.pl);

        return metadata;
    }
}
