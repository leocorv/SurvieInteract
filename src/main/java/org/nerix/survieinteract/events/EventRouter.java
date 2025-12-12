package org.nerix.survieinteract.events;

import com.google.gson.JsonObject;
import org.nerix.survieinteract.ConfigManager;

import java.util.HashMap;
import java.util.Map;

public class EventRouter {

    private static final Map<String, EventHandler> handlers = new HashMap<>();

    public static void register(String eventType, EventHandler handler) {
        handlers.put(eventType, handler);
    }

    public static void dispatch(JsonObject json) {
        String type = json.get("event_type").getAsString().toLowerCase();

        boolean isGift = json.has("is_gift") && json.get("is_gift").getAsBoolean();
        if (isGift) {
            return;
        }

        EventHandler handler = handlers.get(type);

        if (!ConfigManager.isEventEnabled(type)) {
            System.out.println("[SurvieInteract] Event " + type + " ignoré (désactivé)");
            return;
        }

        if (handler != null) {
            handler.handle(json);
        } else {
            System.out.println("[SurvieInteract] Aucun handler pour type: " + type);
        }
    }
}
