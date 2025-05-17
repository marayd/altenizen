/*
 * Copyright (c) mryd - https://mryd.org/
 * All rights reserved.
 *
 * This file is part of the Altenizen project: https://github.com/marayd/altenizen
 *
 * Custom Proprietary License:
 * This source code is the exclusive property of the Author (mryd).
 * Access to this code is provided for viewing purposes only.
 *
 * You MAY NOT:
 * - Use, compile, run, or execute this code.
 * - Modify, distribute, or reproduce any part of this code.
 * - Create forks or derivative works.
 * - Use this code for commercial purposes.
 *
 * No rights or licenses are granted by default. By accessing this file,
 * you acknowledge and agree to the terms of the proprietary license:
 * https://github.com/marayd/altenizen/blob/main/License.md
 *
 * For permissions or inquiries, contact the Author directly.
 */

package org.marayd.altenizen.customevent.denizen;

import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.QueueTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import org.marayd.altenizen.Altenizen;
import org.marayd.altenizen.customevent.bukkit.PlayerEndsSpeaking;
import org.marayd.altenizen.plasmo.PlasmoVoiceAddon;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerEndsSpeakingDenizen extends BukkitScriptEvent implements Listener {
    public PlayerEndsSpeakingDenizen() {}

    private Event event;
    private static PlayerTag player;
    private static ElementTag message;
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
            case "phrase" -> message;
            case "activation" -> new ElementTag(Altenizen.PLASMO_VOICE_ADDON.getVoice().getActivationManager().getActivationById(activationId).get().getName());
            default -> super.getContext(name);
        };
    }

    @Override
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
        message = new ElementTag(event.getMessage());
        bytes = event.getBytes();
        activationId = event.getActivationId();
        Bukkit.getScheduler().runTask(Altenizen.instance, () -> fire(event));
    }

}
