package io.github.developerjose.projectileblockdamage.plugin.runnable;

import io.github.developerjose.projectileblockdamage.blockdamageapi.BlockDamageInfo;
import io.github.developerjose.projectileblockdamage.plugin.ProjectileBlockDamagePlugin;
import org.bukkit.scheduler.BukkitRunnable;

import static org.bukkit.Bukkit.getServer;

public class RemoveExpiredCracksRunnable extends BukkitRunnable {
    private ProjectileBlockDamagePlugin mPlugin;

    public RemoveExpiredCracksRunnable(ProjectileBlockDamagePlugin plugin) {
        mPlugin = plugin;
    }

    public void run() {
        final int maxCrackDuration = mPlugin.mPreferences.getRegenMillis();

        // Check how long the blocks have been cracked
        long currentTime = System.currentTimeMillis();

        for (long damageStartTime : mPlugin.mDamagedBlocks.keySet()) {
            long crackDuration = currentTime - damageStartTime;
            if (crackDuration >= maxCrackDuration) {
                // Get block damage information and remove it from the map
                BlockDamageInfo bdInfo = mPlugin.mDamagedBlocks.get(damageStartTime);
                mPlugin.mDamagedBlocks.remove(damageStartTime);

                // Fix the crack
                bdInfo.mDamage = -1;
                mPlugin.mAPI.sendBlockBreak(getServer(), bdInfo);
            } else {
                break;
            }
        }
    }
}
