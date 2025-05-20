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

package org.marayd.altenizen.command;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.Utilities;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultNull;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import su.plo.slib.api.server.entity.McServerEntity;
import su.plo.slib.api.server.position.ServerPos3d;
import su.plo.slib.api.server.world.McServerWorld;
import su.plo.voice.api.server.audio.source.ServerAudioSource;
import su.plo.voice.api.server.audio.source.ServerDirectSource;
import su.plo.voice.api.server.audio.source.ServerEntitySource;
import su.plo.voice.api.server.audio.source.ServerStaticSource;
import su.plo.voice.api.server.player.VoiceServerPlayer;

import java.util.HashMap;

import static org.marayd.altenizen.Altenizen.PLASMO_VOICE_ADDON;
import static org.marayd.altenizen.plasmo.PlasmoVoiceAddon.sourceLine;

public class SourceCommand extends AbstractCommand implements Holdable {

    public static final HashMap<String, ServerAudioSource<?>> sources = new HashMap<>();

    public SourceCommand() {
        setName("plasmosource");
        setSyntax("plasmosource [action:<create/delete>] [name:<name>] [type:<type>] [entity:<entity>] [location:<location>]");
        setRequiredArguments(2, -1);
        isProcedural = false;
        autoCompile();
    }

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgPrefixed @ArgName("action") ElementTag actionTag,
                                   @ArgPrefixed @ArgName("name") ElementTag nameTag,
                                   @ArgDefaultNull @ArgPrefixed @ArgName("type") ElementTag typeTag,
                                   @ArgDefaultNull @ArgPrefixed @ArgName("entity") EntityTag entity,
                                   @ArgDefaultNull @ArgPrefixed @ArgName("location") LocationTag location) {
        String action = actionTag.asLowerString();
        String name = nameTag.asString();

        switch (action) {
            case "create" -> createSource(scriptEntry, name, typeTag, entity, location);
            case "delete" -> deleteSource(name);
            default -> Debug.echoError("Invalid action: " + action);
        }
    }

    private static void createSource(ScriptEntry scriptEntry, String name, ElementTag typeTag, EntityTag entity, LocationTag location) {
        if (typeTag == null) {
            Debug.echoError("Missing 'type' argument for 'create' action.");
            return;
        }

        String type = typeTag.asLowerString();
        ServerAudioSource<?> source;

        try {
            source = switch (type) {
                case "direct" -> createDirectSource(scriptEntry);
                case "location" -> createStaticSource(location);
                case "entity" -> createEntitySource(entity);
                default -> throw new IllegalArgumentException("Unsupported source type: " + type);
            };
        } catch (Exception e) {
            Debug.echoError("Failed to create source: " + e.getMessage());
            return;
        }

        sources.put(name, source);
        Debug.echoDebug(scriptEntry, "Created source: " + name + " of type: " + type);
    }

    private static void deleteSource(String name) {
        ServerAudioSource<?> source = sources.remove(name);
        if (source != null) {
            source.remove();
            Debug.echoDebug(null, "Deleted source: " + name);
        } else {
            Debug.echoError("No source found with name: " + name);
        }
    }

    private static ServerDirectSource createDirectSource(ScriptEntry scriptEntry) {
        PlayerTag player = Utilities.getEntryPlayer(scriptEntry);
        if (player == null) throw new IllegalStateException("No player context found");

        VoiceServerPlayer voicePlayer = PLASMO_VOICE_ADDON.getVoice().getPlayerManager()
                .getPlayerByName(player.getName())
                .orElseThrow(() -> new IllegalStateException("Voice player not found: " + player.getName()));

        return sourceLine.createDirectSource(voicePlayer, false);
    }

    private static ServerStaticSource createStaticSource(LocationTag loc) {
        if (loc == null) throw new IllegalArgumentException("Location is required for 'location' type");

        McServerWorld world = PLASMO_VOICE_ADDON.getVoice().getMinecraftServer()
                .getWorlds().stream()
                .filter(w -> w.getName().equalsIgnoreCase(loc.getWorldName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("World not found: " + loc.getWorldName()));

        ServerStaticSource source = sourceLine.createStaticSource(
                new ServerPos3d(world, loc.x(), loc.y(), loc.z()), false);
        source.setIconVisible(false);
        return source;
    }

    private static ServerEntitySource createEntitySource(EntityTag ent) {
        if (ent == null) throw new IllegalArgumentException("Entity is required for 'entity' type");

        McServerEntity serverEntity = PLASMO_VOICE_ADDON.getVoice().getMinecraftServer()
                .getEntityByInstance(ent.getBukkitEntity());

        return sourceLine.createEntitySource(serverEntity, false);
    }
}
