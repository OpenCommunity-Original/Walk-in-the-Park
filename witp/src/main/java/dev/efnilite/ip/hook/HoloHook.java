package dev.efnilite.ip.hook;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.Registry;
import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.ip.leaderboard.Score;
import dev.efnilite.ip.mode.Mode;
import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;

public class HoloHook {

    /**
     * Initializes this hook.
     */
    public static void init() {
        try {
            Class.forName("me.filoghost.holographicdisplays.api.HolographicDisplaysAPI");
        } catch (Exception ex) {
            IP.logging().severe("##");
            IP.logging().severe("## IP only supports Holographic Displays v3.0.0 or higher!");
            IP.logging().severe("## This hook will now be disabled.");
            IP.logging().severe("##");
            return;
        }

        HolographicDisplaysAPI.get(IP.getPlugin()).registerGlobalPlaceholder("ip_leaderboard", 100, argument -> {

            if (argument == null) {
                return "?";
            }

            // {ip_leaderboard: default, score, #1}
            String[] split = argument.replace(" ", "").split(",");

            Mode mode = Registry.getMode(split[0].toLowerCase());

            if (mode == null) {
                return "?";
            }

            Leaderboard leaderboard = mode.getLeaderboard();

            String type = split[1].toLowerCase();
            String rank = split[2].replace("#", "");

            Score score = leaderboard.getScoreAtRank(Integer.parseInt(rank));

            if (score == null) {
                return "?";
            }

            return switch (type) {
                case "score" -> Integer.toString(score.score());
                case "name" -> score.name();
                case "time" -> score.time();
                case "difficulty" -> score.difficulty();
                default -> "?";
            };
        });
    }
}