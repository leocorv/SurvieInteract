package org.nerix.survieinteract.events.sub;

import com.google.gson.JsonObject;
import org.nerix.survieinteract.events.EventHandler;

public class SubEvent implements EventHandler {

    @Override
    public void handle(JsonObject json) {
        System.out.println("[SurvieInteract] SUB event reçu: " + json);
        // actions à implémenter plus tard
    }
}
