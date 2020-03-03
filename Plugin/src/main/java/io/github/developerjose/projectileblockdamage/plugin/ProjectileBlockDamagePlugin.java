package io.github.developerjose.projectileblockdamage.plugin;

import io.github.developerjose.projectileblockdamage.blockdamageapi.BlockDamageInfo;
import io.github.developerjose.projectileblockdamage.blockdamageapi.NMSInterface;
import io.github.developerjose.projectileblockdamage.nms.NMS_1_15_R1;
import io.github.developerjose.projectileblockdamage.plugin.runnable.RemoveExpiredCracksRunnable;
import nms.NMS_1_14_R1;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.BlockVector;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Initial request from https://bukkit.org/threads/projectile-block-damage.484909/
 * Inspiration and some ideas taken from http://www.spigotmc.org/resources/block-damage.19958/
 *
 * @author DeveloperJose
 */
public class ProjectileBlockDamagePlugin extends JavaPlugin implements Listener {
    /**
     * Contains the position of a cracked block (key) and the information about that block
     */
    public Map<BlockVector, BlockDamageInfo> mDamagedBlocks = new ConcurrentHashMap<>();

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
        // Variables
        mPreferences = new PreferencesManager(getConfig());

        // NMS Version Management
        if (!setupNMS()) return;

        // Config
        saveDefaultConfig();

        // Events
        getServer().getPluginManager().registerEvents(this, this);

        // Runnable to remove cracked blocks
        new RemoveExpiredCracksRunnable(this).runTaskTimer(this, 0, mPreferences.getRegenCheckTicks());
    }

    private boolean setupNMS() {
        String version = getServer().getClass().getPackage().getName().replace('.', ',').split(",")[3];
        if (version.equals("v1_15_R1")) {
            mAPI = new NMS_1_15_R1();
        } else if (version.equals("v1_14_R1")) {
            mAPI = new NMS_1_14_R1();
        } else {
            getLogger().info(String.format("This Minecraft version (%s) is not supported. Disabling Plugin.", version));
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
        return true;
    }

    @Override
    public void onDisable() {
        super.onDisable();

        // Clear cracked blocks map. No need to send packets as clients will
        // automatically clear them after a little while
        mDamagedBlocks.clear();
    }

    private boolean isDisallowedBlock(Block b) {
        // Don't crack air, water, lava, and passable blocks (signs, flowers, tall grass)
        return (b.isEmpty() || b.isLiquid() || b.isPassable()
                // Don't crack bedrock
                || b.getType() == Material.BEDROCK
        );
    }

    private void crackBlock(Block block, int damage) {
        if (isDisallowedBlock(block))
            return;

        // Don't crack more blocks if we are at full capacity
        if (mDamagedBlocks.size() > mPreferences.getMaxCrackedBlocks())
            return;

        // Hashable position vector
        BlockVector blockVector = block.getLocation().toVector().toBlockVector();

        // Check if this block was previously cracked
        BlockDamageInfo oDamageInfo = mDamagedBlocks.get(blockVector);

        // If not previously cracked, then add to damaged blocks tracking list to fix later
        if (oDamageInfo == null) {
            oDamageInfo = new BlockDamageInfo(block, 0);
            mDamagedBlocks.put(blockVector, oDamageInfo);
        }

        // Damage the block
        oDamageInfo.mDamage += damage;
        if (oDamageInfo.mDamage > 9) {
            // The block is damaged above the limit, break if allowed in the config
            if (mPreferences.isDamageBreakAllowed())
                block.breakNaturally();

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

        // Check if explosions destroy blocks
        if (!mPreferences.explosionsDestroyBlocks())
            ev.blockList().clear();

        // Optimization: Store the (hash-safe) BlockVector positions of the blocks
        // which will be blown up inside of a HashSet for use in the radius check
        Set<BlockVector> set = new HashSet<>();
        for (Block b : ev.blockList())
            set.add(b.getLocation().toVector().toBlockVector());

        // We will only crack blocks that are within the volume of a sphere
        // Mathematics: https://www.geeksforgeeks.org/check-whether-a-point-lies-inside-a-sphere-or-not/
        int cx = ev.getLocation().getBlockX();
        int cy = ev.getLocation().getBlockY();
        int cz = ev.getLocation().getBlockZ();
        int radius = mPreferences.getExplosionRadius();
        int radiusSquared = radius * radius;

        for (int x = cx - radius; x < cx + radius; x++) {
            for (int y = cy - radius; y < cy + radius; y++) {
                for (int z = cz - radius; z < cz + radius; z++) {
                    // Don't consider disallowed blocks, no loops so O(1) time
                    Block currentBlock = ev.getLocation().getWorld().getBlockAt(x, y, z);
                    if (isDisallowedBlock(currentBlock))
                        continue;

                    // Don't consider blocks which are going to be blown up
                    // Because of our earlier optimization, this check will run in O(1) time
                    BlockVector currentBlockVector = new BlockVector(x, y, z);
                    if (set.contains(currentBlockVector))
                        continue;

                    int deltaXSquared = (x - cx) * (x - cx);
                    int deltaYSquared = (y - cy) * (y - cy);
                    int deltaZSquared = (z - cz) * (z - cz);

                    // Outside of radius
                    if (deltaXSquared + deltaYSquared + deltaZSquared >= radiusSquared)
                        continue;

                    // Inside of radius
                    crackBlock(currentBlock, mPreferences.getExplosionDamage());
                }
            }
        }
    }
}
