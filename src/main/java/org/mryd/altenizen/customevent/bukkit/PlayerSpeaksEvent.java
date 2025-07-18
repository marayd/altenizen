package org.mryd.altenizen.customevent.bukkit;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import su.plo.voice.api.server.event.audio.source.PlayerSpeakEvent;

@Getter
public class PlayerSpeaksEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final byte[] bytes;
    private final PlayerSpeakEvent plasmoEvent;

    public PlayerSpeaksEvent(Player player, byte[] bytes, PlayerSpeakEvent plasmoEvent) {
        super(true);
        this.player = player;
        this.bytes = bytes;
        this.plasmoEvent = plasmoEvent;
    }
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
