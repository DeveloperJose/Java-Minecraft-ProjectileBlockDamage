package io.github.developerjose.projectileblockdamage.plugin;

import io.github.developerjose.projectileblockdamage.blockdamageapi.BlockDamageInfo;
import io.github.developerjose.projectileblockdamage.blockdamageapi.NMSInterface;
import io.github.developerjose.projectileblockdamage.nms.NMS_1_15_R1;
import io.github.developerjose.projectileblockdamage.plugin.runnable.RemoveExpiredCracksRunnable;
import org.bukkit.Material;
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
     * The user preferences as set in config.yml
     */
    public PreferencesManager mPreferences;

    @Override
    public void onEnable() {
        super.onEnable();
        // Versioning
        String version = getServer().getClass().getPackage().getName().replace('.', ',').split(",")[3];
        if (version.equals("v1_15_R1"))
            mAPI = new NMS_1_15_R1();
        else {
            getLogger().info("This Minecraft version is not supported. Disabling Plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Variables
        mPreferences = new PreferencesManager(getConfig());

        // Config
        saveDefaultConfig();

        // Events
        getServer().getPluginManager().registerEvents(this, this);

        // Runnable to remove cracked blocks
        new RemoveExpiredCracksRunnable(this).runTaskTimer(this, 0, mPreferences.getRegenCheckTicks());
    }

    @Override
    public void onDisable() {
        super.onDisable();

        // Clear cracked blocks map. No need to send packets as clients will
        // automatically clear them after a little while
        mDamagedBlocks.clear();
    }


    private void crackBlock(Block b, int damage) {
        if (b.getType() == Material.AIR)
            return;

        if (mDamagedBlocks.size() > mPreferences.getMaxCrackedBlocks())
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
        oDamageInfo.mDamage += damage;
        if (oDamageInfo.mDamage > 9) {
            // The block is damaged above the limit, break if allowed in the config
            if (mPreferences.isDamageBreakAllowed()) {
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
        boolean isArrow = (projectile instanceof Arrow) && (mPreferences.areArrowsAllowed());
        boolean isEgg = (projectile instanceof Egg) && (mPreferences.areEggsAllowed());
        boolean isSnowball = (projectile instanceof Snowball) && (mPreferences.areSnowballsAllowed());
        if (isArrow)
            crackBlock(ev.getHitBlock(), mPreferences.getArrowDamage());
        else if (isEgg)
            crackBlock(ev.getHitBlock(), mPreferences.getEggDamage());
        else if (isSnowball)
            crackBlock(ev.getHitBlock(), mPreferences.getSnowballDamage());

    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent ev) {
        // Check if we crack blocks on explosions
        if (!mPreferences.areExplosionsAllowed())
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


}
