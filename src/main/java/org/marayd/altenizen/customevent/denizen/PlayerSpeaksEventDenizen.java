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

import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import org.marayd.altenizen.customevent.bukkit.PlayerSpeaksEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.marayd.altenizen.Altenizen;
import su.plo.slib.api.server.position.ServerPos3d;
import su.plo.slib.api.server.world.McServerWorld;
import su.plo.voice.api.server.audio.source.ServerStaticSource;

import static org.marayd.altenizen.Altenizen.PLASMO_VOICE_ADDON;
import static org.marayd.altenizen.plasmo.PlasmoVoiceAddon.sourceLine;

public class PlayerSpeaksEventDenizen extends BukkitScriptEvent implements Listener {

    public PlayerSpeaksEventDenizen() {
    }

    // Add event-specific fields
    private Event event; // Replace with your specific Bukkit event class, or use custom logic.
    private static PlayerTag player;
    private static byte[] bytes;
    @Override
    public boolean couldMatch(ScriptPath path) {
        // Adjust matching logic based on event name or arguments
        return path.eventLower.startsWith("player speaks");
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
    public boolean applyDetermination(ScriptPath path, ObjectTag determinationObj) {
        String determination = determinationObj.toString();
        if (determinationObj instanceof ElementTag) {
            try {
                String locationData = determination.substring(2).trim();
                String[] parts = locationData.split(",");
                if (parts.length >= 5) { // Allow additional parameters
                    double x = Double.parseDouble(parts[0]);
                    double y = Double.parseDouble(parts[1]);
                    double z = Double.parseDouble(parts[2]);
                    String worldName = parts[3];
                    short distance = Short.parseShort(parts[parts.length - 1]);

                    McServerWorld worldPlasmo = PLASMO_VOICE_ADDON.getVoice().getMinecraftServer()
                            .getWorlds()
                            .stream()
                            .filter(w -> w.getName().equals(worldName))
                            .findAny()
                            .orElseThrow(() -> new IllegalStateException("World not found."));
                    ServerPos3d position = new ServerPos3d(worldPlasmo, x, y, z);

                    ServerStaticSource serverStaticSource = sourceLine.createStaticSource(position, false);

                    System.out.println(determinationObj);
//                    PlayerAudioSender.playSoundOnLocation(denizenAddon.getVoice(), serverStaticSource, bytes, distance);
                    return true;
                } else {
                    throw new IllegalArgumentException("Invalid data format: " + determination);
                }
            } catch (Exception e) {
                System.err.println("Error processing determination string: " + e.getMessage());
            }
        }

        return super.applyDetermination(path, determinationObj);
    }


    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("player")) {
            return player;
        }
        return super.getContext(name);
    }

    @EventHandler
    public void onPlayerSpeak(PlayerSpeaksEvent event) {
        player = new PlayerTag(event.getPlayer());
        this.event = event;
        bytes = event.getBytes();
        Bukkit.getScheduler().runTask(Altenizen.instance, () -> fire(event));
    }

}
