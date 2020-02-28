package io.github.developerjose.projectileblockdamage;

import org.bukkit.block.Block;
import org.bukkit.util.BlockVector;

public class BlockDamageInfo {
    public int mID;
    public int mDamage;
    public BlockVector mPosition;

    public BlockDamageInfo(int mID, int mDamage, BlockVector mPosition) {
        this.mID = mID;
        this.mDamage = mDamage;
        this.mPosition = mPosition;
    }

    public BlockDamageInfo(int mID, int mDamage, Block b) {
        this(mID, mDamage, b.getLocation().toVector().toBlockVector());
    }

    public BlockDamageInfo(Block b) {
        this(-1, -1, b);
    }
}
