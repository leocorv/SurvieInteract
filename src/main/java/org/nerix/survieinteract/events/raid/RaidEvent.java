package org.nerix.survieinteract.events.raid;

import com.google.gson.JsonObject;
import org.nerix.survieinteract.events.EventHandler;

public class RaidEvent implements EventHandler {

    @Override
    public void handle(JsonObject json) {
        System.out.println("[SurvieInteract] Raid event reçu: " + json);
        // actions à implémenter plus tard
    }
}
