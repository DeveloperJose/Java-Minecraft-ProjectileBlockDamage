package io.github.developerjose.projectileblockdamage;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockVector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Inspired by https://bukkit.org/threads/projectile-block-damage.484909/
 *
 * @author DeveloperJose
 * <p>
 * TODO: Better radius configuration
 * TODO: Interfaces for easier version updating
 */
public class ProjectileBlockDamagePlugin extends JavaPlugin implements Listener {
    /**
     * Unique identifier used in the Packet for each crack
     */
    private static int ID = Integer.MIN_VALUE;

    /**
     * Contains the time a block was cracked (key) and the information about that block
     * Uses ConcurrentSkipListMap to avoid ConcurrentModificationException which happens with TreeMap
     */
    private Map<Long, BlockDamageInfo> mDamagedBlocks = new ConcurrentSkipListMap<>();

    /**
     *
     */
    private NMSInterface mAPI;

    /**
     *
     */
    private World mWorld;

    @Override
    public void onEnable() {
        super.onEnable();
        // Versioning
        mAPI = new NMS_1_15_R1();

        // Variables
        mWorld = getServer().getWorlds().get(0);

        // Config
        saveDefaultConfig();

        // Events
        getServer().getPluginManager().registerEvents(this, this);

        // Runnable to remove cracked blocks
        new BukkitRunnable() {
            public void run() {
                List<BlockDamageInfo> blocksToRemove = getExpiredBlocks();

                // If there are no blocks to remove, just stop the method
                if (blocksToRemove.isEmpty())
                    return;

                // Send block crack fix packets to players
                for (Player p : mWorld.getPlayers())
                    for (BlockDamageInfo b : blocksToRemove) {
                        b.mDamage = -1;
                        mAPI.sendBlockBreak(p, b);
                    }
            }
        }.runTaskTimer(this, 0, getRegenCheckTicks());
    }

    private List<BlockDamageInfo> getExpiredBlocks() {
        final int regenMillis = getRegenMillis();

        // Check how long the blocks have been cracked
        List<BlockDamageInfo> expiredBlocks = new ArrayList();
        long currentTime = System.currentTimeMillis();

        for (long damageStartTime : mDamagedBlocks.keySet()) {
            // If more time has passed than the allowed regen, prepare to delete the crack
            if (currentTime - damageStartTime > regenMillis) {
                expiredBlocks.add(mDamagedBlocks.get(damageStartTime));
                mDamagedBlocks.remove(damageStartTime);
            }
            // Since the values are ordered, then if we reach a time within the
            // allowed time frame the rest must also be within the allowed time
            else
                break;
        }
        return expiredBlocks;
    }

    private void crackBlock(Block b) {
        if (b.getType() == Material.AIR)
            return;

        BlockVector bVector = b.getLocation().toVector().toBlockVector();

        // Check if this block was previously cracked
        BlockDamageInfo oDamageInfo = null;
        for (BlockDamageInfo storedDamage : mDamagedBlocks.values()) {
            if (storedDamage.mPosition.equals(bVector)) {
                oDamageInfo = storedDamage;
                break;
            }
        }

        // If not previously cracked, then add to damaged blocks tracking list to fix later
        if (oDamageInfo == null) {
            oDamageInfo = new BlockDamageInfo(ID, 0, bVector);
            long damageStartTime = System.currentTimeMillis();
            mDamagedBlocks.put(damageStartTime, oDamageInfo);
            ID++;
        }

        // Damage the block, cap at 9
        oDamageInfo.mDamage += getBlockCrackDamage();
        if (oDamageInfo.mDamage > 9)
            oDamageInfo.mDamage = 9;

        // Send to all players
        for (Player p : mWorld.getPlayers())
            mAPI.sendBlockBreak(p, oDamageInfo);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent ev) {
        Projectile projectile = ev.getEntity();
        ProjectileSource src = projectile.getShooter();

        // Ignore projectiles not shot by players
        boolean shotByPlayer = src instanceof Player;
        if (!shotByPlayer)
            return;

        // Check if the projectile is one of the allowed ones
        boolean bArrow = (projectile instanceof Arrow) && (areArrowsAllowed());
        boolean bEgg = (projectile instanceof Egg) && (areEggsAllowed());
        boolean bSnowball = (projectile instanceof Snowball) && (areSnowballsAllowed());
        if (bArrow || bEgg || bSnowball)
            crackBlock(ev.getHitBlock());
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent ev) {
        // Check if we crack blocks on explosions
        if (!areExplosionsAllowed())
            return;

        ev.setCancelled(true);
        ev.getLocation().getBlock().setType(Material.GOLD_BLOCK);
        // TODO: Better settings?
//        Set<BlockVector> set = new HashSet<BlockVector>();
//        int rx = getConfig().getInt("x-radius");
//        int ry = getConfig().getInt("y-radius");
//        int rz = getConfig().getInt("z-radius");
//        for (Block b : ev.blockList())
//        for (int x = -1; x < 1; x++)
//            for (int y = -1; y < 1; y++)
//                for (int z = -1; z < 1; z++) {
//                    Location loc2 = b.getLocation().add(x, y, z);
//                    if (ev.getLocation().distance(loc2) > rx)
//                        set.add(new BlockVector(loc2.toVector()));
//                }
//
//
//        // Crack em
//        for (BlockVector v : set)
//            crackBlock(w.getBlockAt(v.toLocation(w)));

    }

    public int getRegenMillis() {
        return getRegenSeconds() * 1000;
    }

    public int getRegenSeconds() {
        return getConfig().getInt("regen-seconds", 20);
    }

    public int getRegenCheckTicks() {
        return getConfig().getInt("regen-check-ticks", 30);
    }

    public int getBlockCrackDamage() {
        return getConfig().getInt("damage");
    }

    public boolean areExplosionsAllowed() {
        return getConfig().getBoolean("explosion");
    }

    public boolean areArrowsAllowed() {
        return getConfig().getBoolean("arrow");
    }

    public boolean areEggsAllowed() {
        return getConfig().getBoolean("egg");
    }

    public boolean areSnowballsAllowed() {
        return getConfig().getBoolean("snowball");
    }
}
