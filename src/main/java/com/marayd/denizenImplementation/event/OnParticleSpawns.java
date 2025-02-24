package com.marayd.denizenImplementation.event;

import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.marayd.denizenImplementation.DenizenImplementation;

public class OnParticleSpawns extends BukkitScriptEvent implements Listener {

    public OnParticleSpawns() {
    }

    // Add event-specific fields
    private Event event; // Replace with your specific Bukkit event class, or use custom logic.
    private static LocationTag location;
    private static ElementTag particle;
    private static PlayerTag player;
    @Override
    public boolean couldMatch(ScriptPath path) {
        // Adjust matching logic based on event name or arguments
        return path.eventLower.startsWith("particle spawns");
    }

    @Override
    public boolean matches(ScriptPath path) {
        // Add logic to determine if this specific script path should trigger
        return super.matches(path);
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        // Replace with your logic for providing player or NPC data
        return new BukkitScriptEntryData(player, null);
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("location")) {
            return location;
        }
        else if (name.equals("particle")) {
            return particle;
        }
        else if (name.equals("player")) {
            return player;
        }

        return super.getContext(name);
    }

    @EventHandler
    public void onPlayerSpeak(OnParticleSpawnsEvent event) {
        player = new PlayerTag(event.getPlayer());
        location = new LocationTag(event.getLocation().getWorld(), event.getLocation().getX(), event.getLocation().getY(), event.getLocation().getZ());
        particle = new ElementTag(event.getParticle().name());
        Bukkit.getScheduler().runTask(DenizenImplementation.instance, () -> fire(event));
    }

}
