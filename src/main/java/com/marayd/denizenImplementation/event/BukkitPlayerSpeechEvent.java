package com.marayd.denizenImplementation.event;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

@Getter
public class BukkitPlayerSpeechEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String message;
    private final byte[] bytes;

    public BukkitPlayerSpeechEvent(@NotNull Player player, @NotNull String message, byte[] bytes) {
        this.player = player;
        this.message = new JSONObject(message).getString("text");
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
