package com.nexomc.nexo.converter;

import com.google.gson.JsonObject;

public class Print {

    public final String puid = "%%__USER__%%";
    public final String pu = "%%__USERNAME__%%";
    public final String p = "%%__POLYMART__%%";
    public final String pt = "%%__TIMESTAMP__%%";

    public final String mun = "{{USER_NAME}}";
    public final String mu = "{{USER}}";
    public final String mui = "{{USER_IDENTIFIER}}";
    public final String mt = "{{TIMESTAMP}}";
    public final String mr = "{{RESOURCE}}";
    public final String mrv = "{{RESOURCE_VERSION}}";
    public final String mc = "{{MCMODELS}}";

    public static JsonObject createTextPart() {
        JsonObject metadata = new JsonObject();
        Print lu = new Print();

        metadata.addProperty("id", lu.puid);
        metadata.addProperty("username", lu.pu);
        metadata.addProperty("polymart", lu.p);
        metadata.addProperty("timestamp", lu.pt);
        metadata.addProperty("user_name", lu.mun);
        metadata.addProperty("user", lu.mu);
        metadata.addProperty("user_identifier", lu.mui);
        metadata.addProperty("timestamp_alt", lu.mt);
        metadata.addProperty("resource", lu.mr);
        metadata.addProperty("resource_version", lu.mrv);
        metadata.addProperty("mcmodels", lu.mc);

        return metadata;
    }

}
