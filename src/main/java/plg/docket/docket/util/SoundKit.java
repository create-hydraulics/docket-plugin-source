package plg.docket.docket.util;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import plg.docket.docket.report.ScanReport;

public final class SoundKit {
    private SoundKit() {}

    public static void joinAlert(Player player, ScanReport report) {
        if (report.countErrors() > 0) {
            play(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.7f);
        } else if (report.countWarnings() > 0) {
            play(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 0.9f);
        } else {
            play(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
        }
    }

    public static void reportOpen(Player player, ScanReport report) {
        if (report.countErrors() > 0) {
            play(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.4f, 0.8f);
        } else {
            play(player, Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.2f);
        }
    }

    public static void pageTurn(Player player) {
        play(player, Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.4f);
    }

    public static void scanStart(Player player) {
        play(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
    }

    public static void scanComplete(Player player, ScanReport report) {
        if (report.countErrors() > 0) {
            play(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.7f);
        } else if (report.countWarnings() > 0) {
            play(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.0f);
        } else {
            play(player, Sound.UI_TOAST_IN, 0.9f, 1.1f);
        }
    }

    public static void reload(Player player) {
        play(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1.8f);
    }

    public static void denied(Player player) {
        play(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
    }

    private static void play(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, SoundCategory.MASTER, volume, pitch);
    }
}
