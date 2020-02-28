package io.github.developerjose.projectileblockdamage;

import net.minecraft.server.v1_15_R1.BlockPosition;
import net.minecraft.server.v1_15_R1.PacketPlayOutBlockBreakAnimation;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class NMS_1_15_R1 implements NMSInterface {
    @Override
    public void sendBlockBreak(Player p, BlockDamageInfo b) {
        BlockPosition bp = new BlockPosition(b.mPosition.getBlockX(), b.mPosition.getBlockY(), b.mPosition.getBlockZ());
        PacketPlayOutBlockBreakAnimation packet = new PacketPlayOutBlockBreakAnimation(b.mID, bp, b.mDamage);
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
    }
}
