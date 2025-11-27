package org.nerix.survieinteract.client.effects;

public class BitsEffectManager {

    private static float radius = 1f;
    private static int ticks = 0;

    public static void activate(float r, int duration) {
        radius = r;
        ticks = duration;
    }

    public static float getRadius() {
        return radius;
    }

    public static boolean isActive() {
        return ticks > 0;
    }

    public static void tick() {
        if (ticks > 0) ticks--;
    }
}
