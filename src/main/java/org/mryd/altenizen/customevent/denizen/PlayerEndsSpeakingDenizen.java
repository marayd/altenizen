package org.mryd.altenizen.customevent.denizen;

import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.QueueTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import org.mryd.altenizen.Altenizen;
import org.mryd.altenizen.customevent.bukkit.PlayerEndsSpeaking;
import org.mryd.altenizen.plasmo.PlasmoVoiceAddon;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class PlayerEndsSpeakingDenizen extends BukkitScriptEvent implements Listener {
    public PlayerEndsSpeakingDenizen() {}

    private Event event;
    private static PlayerTag player;
    private static byte[] bytes;
    private static UUID activationId;

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
        return switch (name) {
            case "player" -> player;
            case "bytes" -> new ElementTag(Base64.getEncoder().encodeToString(bytes));
            case "activation" -> new ElementTag(Altenizen.PLASMO_VOICE_ADDON.getVoice().getActivationManager().getActivationById(activationId).get().getName());
            default -> super.getContext(name);
        };
    }

    @Override
    @SuppressWarnings("removal")
    public boolean applyDetermination(ScriptPath path, ObjectTag determinationObj) {
        String determination = determinationObj.toString();
        if (determinationObj instanceof ElementTag) {
            if (CoreUtilities.toLowerCase(determination).startsWith("save")) {
                String savePath = determination.substring("save".length()).trim();
                try {
                    PlasmoVoiceAddon.saveToWav(bytes, savePath);
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
    public void onPlayerSpeakEnds(PlayerEndsSpeaking event) {
        player = new PlayerTag(event.getPlayer());
        bytes = event.getBytes();
        activationId = event.getActivationId();
        Bukkit.getScheduler().runTask(Altenizen.instance, () -> fire(event));
    }

}
