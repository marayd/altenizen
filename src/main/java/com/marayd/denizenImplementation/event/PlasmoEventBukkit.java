package com.marayd.denizenImplementation.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlasmoEventBukkit extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    public PlasmoEventBukkit() {}

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }


}
