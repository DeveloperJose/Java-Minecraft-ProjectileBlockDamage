package io.github.developerjose.projectileblockdamage;

import org.bukkit.Server;
import org.bukkit.entity.Player;

public interface NMSInterface {
    void sendBlockBreak(Server server, BlockDamageInfo bdInfo);
}
