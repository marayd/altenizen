package com.marayd.denizenImplementation.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class OnParticleSpawnsEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private Particle particle;
    private Double x;
    private Double y;
    private Double z;
    private Player player;

    public OnParticleSpawnsEvent(Particle particle, Double x, Double y, Double z, Player player) {
        this.particle = particle;
        this.x = x;
        this.y = y;
        this.z = z;
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public Particle getParticle() {
        return particle;
    }

    public Location getLocation() {
        return new Location(Bukkit.getWorld("world"), x, y, z);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
