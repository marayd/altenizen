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
import org.marayd.altenizen.plasmo.PlayerAudioSender;
import su.plo.slib.api.server.entity.McServerEntity;
import su.plo.slib.api.server.position.ServerPos3d;
import su.plo.slib.api.server.world.McServerWorld;
import su.plo.voice.api.server.audio.source.AudioSender;
import su.plo.voice.api.server.audio.source.ServerAudioSource;
import su.plo.voice.api.server.player.VoiceServerPlayer;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.marayd.altenizen.Altenizen.PLASMO_VOICE_ADDON;
import static org.marayd.altenizen.Altenizen.instance;
import static org.marayd.altenizen.plasmo.PlasmoVoiceAddon.sourceLine;

public class PlasmoHookCommand extends AbstractCommand implements Holdable {

    public PlasmoHookCommand() {
        setName("plasmo");
        setSyntax("plasmo [action:<action>] [path:<path>] [location:<location>] [distance:<distance>] [entity:<entity>] [id:<id>] [source:<source>]");
        setRequiredArguments(1, -1);
        isProcedural = false;
        autoCompile();
    }

    public static void autoExecute(
            ScriptEntry scriptEntry,
            @ArgPrefixed @ArgName("action") ElementTag action,
            @ArgPrefixed @ArgDefaultNull @ArgName("path") ElementTag path,
            @ArgPrefixed @ArgDefaultNull @ArgName("bytes") ElementTag bytes,
            @ArgPrefixed @ArgDefaultNull @ArgName("location") LocationTag location,
            @ArgPrefixed @ArgDefaultNull @ArgName("distance") ElementTag distance,
            @ArgPrefixed @ArgDefaultNull @ArgName("entity") EntityTag entity,
            @ArgPrefixed @ArgDefaultNull @ArgName("id") ElementTag id,
            @ArgPrefixed @ArgDefaultNull @ArgName("source") ElementTag sourceName
    ) {
        PlayerTag player = Utilities.getEntryPlayer(scriptEntry);

        VoiceServerPlayer voicePlayer = PLASMO_VOICE_ADDON.getVoice().getPlayerManager()
                .getPlayerByName(player.getName()).orElse(null);

        String act = action.asLowerString();
        byte[] byteArray = null;
        if (bytes != null) {
            byteArray = Base64.getDecoder().decode(bytes.asString());
        }
        switch (act) {
            case "send" -> handleSend(path, sourceName, scriptEntry, voicePlayer, byteArray);
            case "stop" -> handleStop(id);
            case "playonloc" -> handlePlayOnLoc(path, location, distance, sourceName, scriptEntry, byteArray);
            case "playonentity" -> handlePlayOnEntity(path, entity, distance, sourceName, scriptEntry, byteArray);
            default -> throw new IllegalArgumentException("Unknown action: " + act);
        }
    }

    private static void handleSend(ElementTag path, ElementTag sourceName, ScriptEntry entry, VoiceServerPlayer player, byte[] bytes) {
        ServerAudioSource<?> source = SourceCommand.sources.get(sourceName != null ? sourceName.asString() : "");
        if (source == null) {
            source = sourceLine.createDirectSource(player, false);
        }

        entry.saveObject("audio_id", new ElementTag(source.getId().toString()));
        if (bytes != null) {
            PlayerAudioSender.playAudioFromBytes(
                    PLASMO_VOICE_ADDON.getVoice(),
                    source,
                    bytes,
                    null);
            return;
        }
        try {
            PlayerAudioSender.playAudioFromFile(
                    PLASMO_VOICE_ADDON.getVoice(),
                    source,
                    instance.getConfig().getString("settings.default-path") + "/" + path.asString(),
                    null
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to send audio: " + e.getMessage(), e);
        }
    }

    private static void handlePlayOnLoc(ElementTag path, LocationTag loc, ElementTag distance, ElementTag sourceName, ScriptEntry entry, byte[] bytes) {
        if (distance == null) {
            throw new IllegalArgumentException("Missing path/location/distance for playonloc");
        }
        McServerWorld world = null;
        try {
            world = PLASMO_VOICE_ADDON.getVoice().getMinecraftServer()
                    .getWorlds().stream()
                    .filter(w -> w.getName().equals(loc.getWorldName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("World not found"));
        } catch (Exception e) {
            // Do nothing
        }
        ServerAudioSource<?> source = SourceCommand.sources.get(sourceName != null ? sourceName.asString() : "");
        if (source == null) {
            source = sourceLine.createStaticSource(new ServerPos3d(world, loc.x(), loc.y(), loc.z()), false);
        }

        source.setIconVisible(false);
        entry.saveObject("audio_id", new ElementTag(source.getId().toString()));
        if (bytes != null) {
            PlayerAudioSender.playAudioFromBytes(
                    PLASMO_VOICE_ADDON.getVoice(),
                    source,
                    bytes,
                    Short.parseShort(distance.asString()));
            return;
        }
        PlayerAudioSender.playAudioFromFile(
                PLASMO_VOICE_ADDON.getVoice(),
                source,
                instance.getConfig().getString("settings.default-path") + "/" + path.asString(),
                Short.parseShort(distance.asString())
        );
    }

    private static void handlePlayOnEntity(ElementTag path, EntityTag ent, ElementTag distance, ElementTag sourceName, ScriptEntry entry, byte[] bytes) {
        if (distance == null) {
            throw new IllegalArgumentException("Missing path/entity/distance for playonentity");
        }
        McServerEntity serverEntity = null;
        try {
            serverEntity = PLASMO_VOICE_ADDON.getVoice().getMinecraftServer()
                    .getEntityByInstance(ent.getBukkitEntity());
        } catch (Exception e) {
            // Do nothing
        }
        ServerAudioSource<?> source = SourceCommand.sources.get(sourceName != null ? sourceName.asString() : "");
        if (source == null) {
            source = sourceLine.createEntitySource(serverEntity, false);
        }

        entry.saveObject("audio_id", new ElementTag(source.getId().toString()));
        if (bytes != null) {
            PlayerAudioSender.playAudioFromBytes(
                    PLASMO_VOICE_ADDON.getVoice(),
                    source,
                    bytes,
                    Short.parseShort(distance.asString()));
            return;
        }
        PlayerAudioSender.playAudioFromFile(
                PLASMO_VOICE_ADDON.getVoice(),
                source,
                instance.getConfig().getString("settings.default-path") + "/" + path.asString(),
                Short.parseShort(distance.asString())
        );
    }

    private static void handleStop(ElementTag id) {
        if (id == null) {
            throw new IllegalArgumentException("Missing 'id' for stop action");
        }

        UUID uuid = UUID.fromString(id.asString());
        Optional<ServerAudioSource<?>> sourceOpt = sourceLine.getSourceById(uuid);
        sourceOpt.ifPresentOrElse(
                source -> {
                    AudioSender sender = PlayerAudioSender.sources.get(source);
                    if (sender != null) sender.stop();
                },
                () -> System.err.println("Audio source not found for ID: " + uuid)
        );
    }
}
