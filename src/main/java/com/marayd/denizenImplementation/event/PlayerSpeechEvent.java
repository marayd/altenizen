package com.marayd.denizenImplementation.event;

import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.QueueTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.marayd.denizenImplementation.DenizenImplementation;
import com.marayd.denizenImplementation.PlasmoHook.DenizenAddon;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlayerSpeechEvent extends BukkitScriptEvent implements Listener {
    public PlayerSpeechEvent() {}

    private Event event;
    private static PlayerTag player;
    private static ElementTag message;
    private static byte[] bytes;
    public List<QueueTag> waitForQueues = new ArrayList<>();
    @Override
    public boolean couldMatch(ScriptPath path) {
        return path.eventLower.startsWith("player ends speak");
    }

    @Override
    public boolean matches(ScriptPath path) {
        return super.matches(path);
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(player, null);
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("player")) {
            return player;
        }
        else if (name.equals("phrase")) {
            return message;
        }

        return super.getContext(name);
    }

    @Override
    public boolean applyDetermination(ScriptPath path, ObjectTag determinationObj) {
        String determination = determinationObj.toString();
        if (determinationObj instanceof ElementTag) {
            if (CoreUtilities.toLowerCase(determination).startsWith("save")) {
                String savePath = determination.substring("save".length()).trim();
                try {
                    DenizenAddon.saveToWav(bytes, savePath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return true;
            }
        }
        if (QueueTag.matches(determination)) {
            QueueTag newQueue = QueueTag.valueOf(determination, getTagContext(path));
            if (newQueue != null && newQueue.getQueue() != null) {
                waitForQueues.add(newQueue);
            }
            return true;
        }
        return super.applyDetermination(path, determinationObj);
    }

    @EventHandler
    public void onPlayerSpeakEnds(BukkitPlayerSpeechEvent event) {
        player = new PlayerTag(event.getPlayer());
        message = new ElementTag(event.getMessage());
        bytes = event.getBytes();
        Bukkit.getScheduler().runTask(DenizenImplementation.instance, () -> fire(event));
    }

}
