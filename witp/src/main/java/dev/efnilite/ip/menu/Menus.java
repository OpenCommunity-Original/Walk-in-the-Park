package dev.efnilite.ip.menu;

import dev.efnilite.ip.menu.community.CommunityMenu;
import dev.efnilite.ip.menu.community.LeaderboardsMenu;
import dev.efnilite.ip.menu.community.SingleLeaderboardMenu;
import dev.efnilite.ip.menu.lobby.LobbyMenu;
import dev.efnilite.ip.menu.lobby.PlayerManagementMenu;
import dev.efnilite.ip.menu.play.PlayMenu;
import dev.efnilite.ip.menu.play.SingleMenu;
import dev.efnilite.ip.menu.play.SpectatorMenu;
import dev.efnilite.ip.menu.settings.ParkourSettingsMenu;
import dev.efnilite.ip.menu.settings.SettingsMenu;

public class Menus {

    // main
    public static MainMenu MAIN = new MainMenu();

    // play
    public static PlayMenu PLAY = new PlayMenu();
    public static SingleMenu SINGLE = new SingleMenu();
    public static SpectatorMenu SPECTATOR = new SpectatorMenu();

    // community
    public static CommunityMenu COMMUNITY = new CommunityMenu();
    public static LeaderboardsMenu LEADERBOARDS = new LeaderboardsMenu();
    public static SingleLeaderboardMenu SINGLE_LEADERBOARD = new SingleLeaderboardMenu();

    // settings
    public static SettingsMenu SETTINGS = new SettingsMenu();
    public static ParkourSettingsMenu PARKOUR_SETTINGS = new ParkourSettingsMenu();

    // lobby
    public static LobbyMenu LOBBY = new LobbyMenu();
    public static PlayerManagementMenu PLAYER_MANAGEMENT = new PlayerManagementMenu();

}
