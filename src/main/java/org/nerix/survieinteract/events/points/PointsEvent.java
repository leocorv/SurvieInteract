package org.nerix.survieinteract.events.points;

import com.google.gson.JsonObject;
import org.nerix.survieinteract.events.EventHandler;

public class PointsEvent implements EventHandler {

    @Override
    public void handle(JsonObject json) {
        System.out.println("[SurvieInteract] Points event reçu: " + json);
        // actions à implémenter plus tard
    }
}
