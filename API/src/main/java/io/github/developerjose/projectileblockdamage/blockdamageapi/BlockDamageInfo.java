package io.github.developerjose.projectileblockdamage.blockdamageapi;

import org.bukkit.block.Block;

public class BlockDamageInfo {
    /**
     * Unique identifier used in the Packet for each crack
     */
    private static int ID = Integer.MIN_VALUE;

    public Block mBlock;
    public int mDamage;
    public long mDamageStartTimeMillis;
    public int mID;

    public BlockDamageInfo(Block block, int damage) {
        this.mBlock = block;
        this.mDamage = damage;
        this.mDamageStartTimeMillis = System.currentTimeMillis();
        this.mID = ID;
        ID++;
    }
}
