package com.marayd.denizenImplementation.event;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class OnPlayerSpeakEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final byte[] bytes;

    public OnPlayerSpeakEvent(Player player, byte[] bytes) {
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
