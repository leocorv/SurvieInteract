package org.nerix.survieinteract.events.bits;

import com.google.gson.JsonObject;
import org.nerix.survieinteract.events.EventHandler;

public class BitsEvent implements EventHandler {

    @Override
    public void handle(JsonObject json) {
        System.out.println("[SurvieInteract] Bits event reçu: " + json);
        // actions à implémenter plus tard
    }
}
