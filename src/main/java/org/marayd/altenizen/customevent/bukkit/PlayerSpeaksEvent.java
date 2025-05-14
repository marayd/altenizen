package org.marayd.altenizen.customevent.bukkit;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;

@Getter
public class PlayerSpeaksEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final byte[] bytes;

    public PlayerSpeaksEvent(Player player, byte[] bytes) {
        super(true);
        this.player = player;
        this.bytes = bytes;
    }
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
