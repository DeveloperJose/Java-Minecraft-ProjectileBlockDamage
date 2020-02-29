package io.github.developerjose.projectileblockdamage;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Manages the user preferences set in the config.yml file
 * Used for better readability
 */
public class PreferencesManager {
    private FileConfiguration mConfig;

    public PreferencesManager(FileConfiguration config) {
        mConfig = config;
    }

    /******************** BASIC CONFIG ********************/
    public int getMaxCrackedBlocks() {
        return mConfig.getInt("max-blocks");
    }

    public boolean isDamageBreakAllowed() {
        return mConfig.getBoolean("allow-damage-break");
    }

    /******************** Allows ********************/
    public boolean areArrowsAllowed() {
        return mConfig.getBoolean("allow-arrow");
    }

    public boolean areEggsAllowed() {
        return mConfig.getBoolean("allow-egg");
    }

    public boolean areSnowballsAllowed() {
        return mConfig.getBoolean("allow-snowball");
    }

    public boolean areExplosionsAllowed() {
        return mConfig.getBoolean("allow-explosion");
    }

    /******************** Damages ********************/
    public int getArrowDamage() {
        return mConfig.getInt("damage-arrow");
    }


    public int getEggDamage() {
        return mConfig.getInt("damage-egg");
    }


    public int getSnowballDamage() {
        return mConfig.getInt("damage-snowball");
    }


    public int getExplosionDamage() {
        return mConfig.getInt("damage-explosion");
    }

    /********************* REGEN ********************/
    public int getRegenSeconds() {
        return mConfig.getInt("regen-seconds");
    }

    public int getRegenCheckTicks() {
        return mConfig.getInt("regen-check-ticks");
    }

    public int getRegenMillis() {
        return getRegenSeconds() * 1000;
    }
}
