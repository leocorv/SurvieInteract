package org.nerix.survieinteract.events;

import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;

public class EventRouter {

    private static final Map<String, EventHandler> handlers = new HashMap<>();

    public static void register(String eventType, EventHandler handler) {
        handlers.put(eventType, handler);
    }

    public static void dispatch(JsonObject json) {
        String type = json.get("event_type").getAsString().toLowerCase();
        EventHandler handler = handlers.get(type);

        if (handler != null) {
            handler.handle(json);
        } else {
            System.out.println("[SurvieInteract] Aucun handler pour type: " + type);
        }
    }
}
