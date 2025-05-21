package org.marayd.altenizen.customevent.denizen;

import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.QueueTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.marayd.altenizen.Altenizen;
import org.marayd.altenizen.customevent.bukkit.PlayerSpeaksEvent;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class PlayerSpeaksEventDenizen extends BukkitScriptEvent implements Listener {
    public PlayerSpeaksEventDenizen() {}

    private Event event;
    private static PlayerTag player;
    private static byte[] bytes;
    private static UUID activationId;
    public List<QueueTag> waitForQueues = new ArrayList<>();
    @Override
    public boolean couldMatch(ScriptPath path) {
        return path.eventLower.startsWith("player speaks");
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
            case "activation" -> new ElementTag(Altenizen.PLASMO_VOICE_ADDON.getVoice().getActivationManager().getActivationById(activationId).get().getName());
            case "bytes" -> new ElementTag(Base64.getEncoder().encodeToString(bytes));
            default -> super.getContext(name);
        };
    }

    @EventHandler
    public void onPlayerSpeakEnds(PlayerSpeaksEvent event) {
        player = new PlayerTag(event.getPlayer());
        bytes = event.getBytes();
        activationId = event.getPlasmoEvent().getPacket().getActivationId();
        Bukkit.getScheduler().runTask(Altenizen.instance, () -> fire(event));
    }

}
