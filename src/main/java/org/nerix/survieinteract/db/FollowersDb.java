package org.nerix.survieinteract.db;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.sql.*;

public class FollowersDb {

    private static Path DB_PATH;

    public static void init() {
        DB_PATH = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("twitch_followers.db");

        try (Connection c = getConnection();
             Statement st = c.createStatement()) {

            // Au cas où la table n’existe pas (dev / autre machine)
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS followers (
                    id TEXT PRIMARY KEY,
                    username VARCHAR(255)
                )
            """);

        } catch (Exception e) {
            System.out.println("[SurvieInteract] ERREUR DB init followers: " + e.getMessage());
        }
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + DB_PATH.toString());
    }

    /**
     * @return true si le username est déjà connu (case-insensitive)
     */
    public static boolean isKnownFollower(String username) {
        if (username == null || username.isBlank())
            return false;

        String u = username.trim();

        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM followers WHERE lower(username) = lower(?) LIMIT 1"
             )) {

            ps.setString(1, u);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (Exception e) {
            System.out.println("[SurvieInteract] ERREUR DB isKnownFollower: " + e.getMessage());
            return false;
        }
    }

    /**
     * @return true si on vient de l’inscrire (nouveau follow), false s’il existait déjà
     */
    public static boolean registerFollowerIfNew(String username) {
        if (username == null || username.isBlank())
            return false;

        String u = username.trim();

        if (isKnownFollower(u)) {
            return false;
        }

        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO followers(username) VALUES (?)"
             )) {

            ps.setString(1, u);
            ps.executeUpdate();
            return true;

        } catch (Exception e) {
            System.out.println("[SurvieInteract] ERREUR DB registerFollowerIfNew: " + e.getMessage());
            return false;
        }
    }
}
