package org.nerix.survieinteract.client.effects;

public class BitsEffectManager {

    private static boolean active = false;
    private static float radius = 0f;

    private static int ticksLeft = 0;
    private static int totalTicks = 0;

    public static void activate(float newRadius, int durationTicks) {
        active = true;
        radius = newRadius;
        ticksLeft = durationTicks;
        totalTicks = durationTicks;
    }

    public static void tick() {
        if (!active) return;

        ticksLeft--;
        if (ticksLeft <= 0) {
            active = false;
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static float getRadius() {
        return radius;
    }

    public static float getFadeFactor() {
        if (!active) return 0f;
        if (totalTicks <= 0) return 1f;

        float progress = 1f - (ticksLeft / (float) totalTicks);

        // Fade-in : 0 → 0.2
        if (progress < 0.2f) {
            return progress / 0.2f;
        }

        // Fade-out : 0.8 → 1.0
        if (progress > 0.8f) {
            return (1f - progress) / 0.2f;
        }

        // Pleine intensité
        return 1f;
    }
}
