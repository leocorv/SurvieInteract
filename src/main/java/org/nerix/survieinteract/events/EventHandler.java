package org.nerix.survieinteract.events;

import com.google.gson.JsonObject;

public interface EventHandler {
    void handle(JsonObject json);
}
