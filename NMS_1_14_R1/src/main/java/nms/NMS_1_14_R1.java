package nms;

import io.github.developerjose.projectileblockdamage.blockdamageapi.BlockDamageInfo;
import io.github.developerjose.projectileblockdamage.blockdamageapi.NMSInterface;
import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.PacketPlayOutBlockBreakAnimation;
import org.bukkit.Server;
import org.bukkit.craftbukkit.v1_14_R1.CraftServer;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;

public class NMS_1_14_R1 implements NMSInterface {
    @Override
    public void sendBlockBreak(Server server, BlockDamageInfo bdInfo) {
        // CraftBukkit
        CraftWorld craftWorld = (CraftWorld) bdInfo.mLocation.getWorld();
        CraftServer craftServer = (CraftServer) server;

        // Packet Preparation
        BlockPosition bp = new BlockPosition(bdInfo.mLocation.getBlockX(), bdInfo.mLocation.getBlockY(), bdInfo.mLocation.getBlockZ());
        PacketPlayOutBlockBreakAnimation packet = new PacketPlayOutBlockBreakAnimation(bdInfo.mID, bp, bdInfo.mDamage);

        // Packet Sending
        craftServer.getHandle().sendPacketNearby(
                null,
                bdInfo.mLocation.getBlockX(), bdInfo.mLocation.getBlockY(), bdInfo.mLocation.getBlockZ(),
                64, // Distance
                craftWorld.getHandle().getWorldProvider().getDimensionManager(),
                packet);
    }
}
