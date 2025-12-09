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

        float fadeSpan = 0.05f; // fade sur 5% du temps

        // Fade-in
        if (progress < fadeSpan) {
            return progress / fadeSpan;
        }

        // Fade-out
        if (progress > 1f - fadeSpan) {
            return (1f - progress) / fadeSpan;
        }

        return 1f;
    }

}
