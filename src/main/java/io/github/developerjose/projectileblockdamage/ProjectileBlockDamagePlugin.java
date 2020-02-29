package io.github.developerjose.projectileblockdamage;

import io.github.developerjose.projectileblockdamage.runnable.RemoveExpiredCracksRunnable;
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

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Inspired by https://bukkit.org/threads/projectile-block-damage.484909/
 *
 * @author DeveloperJose
 * <p>
 * TODO: Better radius configuration
 * TODO: Interfaces for easier version
 * TODO: Different damage values for each projectile?
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
    public Map<Long, BlockDamageInfo> mDamagedBlocks = new ConcurrentSkipListMap<>();

    /**
     * The NMS interface for this specific Minecraft server version
     */
    public NMSInterface mAPI;

    /**
     * The main world of the server.
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
        new RemoveExpiredCracksRunnable(this).runTaskTimer(this, 0, getRegenCheckTicks());
    }

    @Override
    public void onDisable() {
        super.onDisable();

        // Clear cracked blocks map. No need to send packets as clients will
        // automatically clear them after a little while
        mDamagedBlocks.clear();
    }


    private void crackBlock(Block b) {
        if (b.getType() == Material.AIR)
            return;

        // Check if this block was previously cracked
        BlockDamageInfo oDamageInfo = null;
        for (BlockDamageInfo storedDamage : mDamagedBlocks.values()) {
            if (storedDamage.isSameLocation(b.getLocation())) {
                oDamageInfo = storedDamage;
                break;
            }
        }

        // If not previously cracked, then add to damaged blocks tracking list to fix later
        if (oDamageInfo == null) {
            oDamageInfo = new BlockDamageInfo(ID, 0, b.getLocation());
            long damageStartTime = System.currentTimeMillis();
            mDamagedBlocks.put(damageStartTime, oDamageInfo);
            ID++;
        }

        // Damage the block
        oDamageInfo.mDamage += getBlockCrackDamage();
        if (oDamageInfo.mDamage > 9) {
            // The block is damaged above the limit, break if allowed in the config
            if (isDamageBreakAllowed()) {
                oDamageInfo.mLocation.getBlock().breakNaturally();
                return;
            }
            // Cap at 9 if not allowed to break
            oDamageInfo.mDamage = 9;
        }

        // Send packet update
        mAPI.sendBlockBreak(getServer(), oDamageInfo);
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
        return getConfig().getBoolean("allow-explosion");
    }

    public boolean areArrowsAllowed() {
        return getConfig().getBoolean("allow-arrow");
    }

    public boolean areEggsAllowed() {
        return getConfig().getBoolean("allow-egg");
    }

    public boolean areSnowballsAllowed() {
        return getConfig().getBoolean("allow-snowball");
    }

    public boolean isDamageBreakAllowed() {
        return getConfig().getBoolean("allow-damage-break");
    }
}
