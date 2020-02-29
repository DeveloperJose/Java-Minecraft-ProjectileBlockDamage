package io.github.developerjose.projectileblockdamage;

import org.bukkit.Location;

public class BlockDamageInfo {
    public int mID;
    public int mDamage;
    public Location mLocation;

    public BlockDamageInfo(int id, int damage, Location location) {
        this.mID = id;
        this.mDamage = damage;
        this.mLocation = location;
    }

    public boolean isSameLocation(Location other){
        // Location equals() checks: X, Y, Z, pitch, yaw
        // Vector equals() checks only X, Y, and Z
        return mLocation.toVector().equals(other.toVector());
    }
}
