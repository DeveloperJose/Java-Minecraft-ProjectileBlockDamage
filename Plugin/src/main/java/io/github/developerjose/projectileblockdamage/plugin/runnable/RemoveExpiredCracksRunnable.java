package io.github.developerjose.projectileblockdamage.plugin.runnable;

import io.github.developerjose.projectileblockdamage.blockdamageapi.BlockDamageInfo;
import io.github.developerjose.projectileblockdamage.plugin.ProjectileBlockDamagePlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockVector;

import java.util.Map;

import static org.bukkit.Bukkit.getServer;

public class RemoveExpiredCracksRunnable extends BukkitRunnable {
    private ProjectileBlockDamagePlugin mPlugin;

    public RemoveExpiredCracksRunnable(ProjectileBlockDamagePlugin plugin) {
        mPlugin = plugin;
    }

    public void run() {
        // Check how long the blocks have been cracked
        final long maxCrackDuration = mPlugin.mPreferences.getRegenMillis();
        final long currentTime = System.currentTimeMillis();

        for (final Map.Entry<BlockVector, BlockDamageInfo> entry : mPlugin.mDamagedBlocks.entrySet()) {
            BlockDamageInfo bdInfo = entry.getValue();
            long crackDuration = currentTime - bdInfo.mDamageStartTimeMillis;

            if (crackDuration >= maxCrackDuration) {
                // Get block damage information and remove it from the map
                mPlugin.mDamagedBlocks.remove(entry.getKey());

                // Fix the crack
                bdInfo.mDamage = -1;
            }
            mPlugin.mAPI.sendBlockBreak(getServer(), bdInfo);
        }
    }
}
