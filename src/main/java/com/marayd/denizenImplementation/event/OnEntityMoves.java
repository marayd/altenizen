package com.marayd.denizenImplementation.event;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.marayd.denizenImplementation.DenizenImplementation;

public class OnEntityMoves extends BukkitScriptEvent implements Listener {

    public OnEntityMoves() {
    }

    private Event event;
    private static ObjectTag entity;

    @Override
    public boolean couldMatch(ScriptPath path) {
        return path.eventLower.startsWith("entity moves");
    }

    @Override
    public boolean matches(ScriptPath path) {
        return super.matches(path);
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(null, null);
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("entity")) {
            return entity;
        }
        return super.getContext(name);
    }

    @EventHandler
    public void onEntityMoves(EntityMoveEvent event) {
        entity = new EntityTag(event.getEntity());
        this.event = event;
        Bukkit.getScheduler().runTask(DenizenImplementation.instance, () -> fire(event));
    }

}
