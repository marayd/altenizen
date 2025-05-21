package org.marayd.altenizen.customevent.bukkit;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
public class PlayerEndsSpeaking extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final byte[] bytes;
    private final UUID activationId;

    public PlayerEndsSpeaking(Player player, byte[] bytes, UUID uuid) {
        super(true);
        this.player = player;
        this.bytes = bytes;
        this.activationId = uuid;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
