package io.github.developerjose.projectileblockdamage;

import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.PacketPlayOutBlockBreakAnimation;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockVector;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Inspired by https://bukkit.org/threads/projectile-block-damage.484909/
 *
 * @author DeveloperJose
 */
public class ProjectileBlockDamagePlugin extends JavaPlugin implements Listener {
    /**
     * Unique identifier used in the Packet for each crack
     */
    private static int ID = Integer.MIN_VALUE;

    /**
     * Contains the time a block was cracked (key) and the packet to fix it (value)
     * Uses ConcurrentSkipListMap to avoid ConcurrentModificationException which happen with TreeMap
     */
    private Map<Long, PacketPlayOutBlockBreakAnimation> mDamagedBlocks = new ConcurrentSkipListMap<Long, PacketPlayOutBlockBreakAnimation>();

    @Override
    public void onEnable() {
        super.onEnable();

        // Config
        saveDefaultConfig();

        // Events
        getServer().getPluginManager().registerEvents(this, this);

        // Runnable to remove cracked blocks
        int regenSeconds = getConfig().getInt("regeneration");
        final int regenMillis = regenSeconds * 1000;
        int periodTicks = regenSeconds * 20;
        new BukkitRunnable() {
            public void run() {
                // Check how long the blocks have been cracked
                List<PacketPlayOutBlockBreakAnimation> blocksToRemove = new ArrayList();
                long currentTime = System.currentTimeMillis();
                for (long damageStartTime : mDamagedBlocks.keySet()) {
                    // If more time has passed than the allowed regen, prepare to delete the crack
                    if (currentTime - damageStartTime > regenMillis) {
                        blocksToRemove.add(mDamagedBlocks.get(damageStartTime));
                        mDamagedBlocks.remove(damageStartTime);
                    }
                    // Since the values are ordered, then if we reach a time within the
                    // allowed time frame the rest must also be within the allowed time
                    else
                        break;
                }

                // Send crack fix packets to players
                World w = getServer().getWorlds().get(0);
                for (Player p : w.getPlayers())
                    for (PacketPlayOutBlockBreakAnimation packet : blocksToRemove)
                        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
            }
        }.runTaskTimer(this, 0, periodTicks);
    }

    private void crackBlock(Block b) {
        if (b.getType() == Material.AIR)
            return;

        World w = getServer().getWorlds().get(0);
        int crackDamage = getConfig().getInt("damage");
        BlockPosition bp = new BlockPosition(b.getX(), b.getY(), b.getZ());
        ID++;

        // Add to damaged blocks tracking list, to fix later
        mDamagedBlocks.put(System.currentTimeMillis(), new PacketPlayOutBlockBreakAnimation(ID, bp, -1));

        // Prepare packet and send to all players
        PacketPlayOutBlockBreakAnimation packet = new PacketPlayOutBlockBreakAnimation(ID, bp, crackDamage);
        for (Player p : w.getPlayers())
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent ev) {
        Projectile pr = ev.getEntity();
        ProjectileSource src = pr.getShooter();

        // Ignore projectiles not shot by players
        if (!(src instanceof Player))
            return;

        // Check if the projectile is one of the allowed ones
        boolean bArrow = (pr instanceof Arrow) && (getConfig().getBoolean("arrow"));
        boolean bEgg = (pr instanceof Egg) && (getConfig().getBoolean("egg"));
        boolean bSnowball = (pr instanceof Snowball) && (getConfig().getBoolean("snowball"));
        if (bArrow || bEgg || bSnowball)
            crackBlock(ev.getHitBlock());
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent ev) {
        // Check if we crack blocks on explosion
        if (!getConfig().getBoolean("explosion"))
            return;

        // Get the nearby blocks next to the exploded ones to crack them
        World w = ev.getLocation().getWorld();

        Set<BlockVector> set = new HashSet<BlockVector>();
        int rx = getConfig().getInt("x-radius");
        int ry = getConfig().getInt("y-radius");
        int rz = getConfig().getInt("z-radius");
        for (Block b : ev.blockList()) {
            for (int x = -rx; x <= rx; x++)
                for (int y = -ry; y <= ry; y++)
                    for (int z = -rz; z <= rz; z++)
                        set.add(new BlockVector(b.getLocation().add(x, y, z).toVector()));
        }

        // Crack em
        for (BlockVector v : set)
            crackBlock(w.getBlockAt(v.toLocation(w)));

    }
}
