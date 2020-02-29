package io.github.developerjose.projectileblockdamage.blockdamageapi;

import org.bukkit.Server;

public interface NMSInterface {
    void sendBlockBreak(Server server, BlockDamageInfo bdInfo);
}
