package org.marayd.altenizen.command;

import com.denizenscript.denizen.objects.*;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import org.marayd.altenizen.Altenizen;
import org.marayd.altenizen.plasmo.PlayerAudioSender;
import su.plo.slib.api.server.entity.McServerEntity;
import su.plo.slib.api.server.position.ServerPos3d;
import su.plo.slib.api.server.world.McServerWorld;
import su.plo.voice.api.server.audio.source.*;
import su.plo.voice.api.server.player.VoiceServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.marayd.altenizen.Altenizen.*;
import static org.marayd.altenizen.plasmo.DenizenAddon.sourceLine;

public class PlasmoHookCommand extends AbstractCommand implements Holdable {
    private final List<CommandArgumentDefinition> argumentDefinitions = List.of(
            new CommandArgumentDefinition("path", false, (entry, arg) -> {
                if (arg.matchesPrefix("path") && !entry.hasObject("path")) {
                    entry.addObject("path", arg.asElement());
                }
            }),
            new CommandArgumentDefinition("location", false, (entry, arg) -> {
                if (arg.matchesPrefix("location") && !entry.hasObject("location")) {
                    entry.addObject("location", arg.asType(LocationTag.class));
                }
            }),
            new CommandArgumentDefinition("distance", false, (entry, arg) -> {
                if (arg.matchesPrefix("distance") && !entry.hasObject("distance")) {
                    entry.addObject("distance", arg.asElement());
                }
            }),
            new CommandArgumentDefinition("id", false, (entry, arg) -> {
                if (arg.matchesPrefix("id") && !entry.hasObject("id")) {
                    entry.addObject("id", arg.asElement());
                }
            }),
            new CommandArgumentDefinition("entity", false, (entry, arg) -> {
                if (arg.matchesPrefix("entity") && !entry.hasObject("entity")) {
                    entry.addObject("entity", arg.asType(EntityTag.class));
                }
            })
    );

    public PlasmoHookCommand() {
        setName("plasmo");
        setSyntax("plasmo (send|stop|take|playonloc|playonentity) path:<path> location:<loc> distance:<distance> [id:<id>]");
        isProcedural = true;
    }

    @Override
    public void parseArgs(ScriptEntry entry) throws InvalidArgumentsException {
        String action = null;

        for (Argument arg : entry) {
            if (action == null && arg.matches("send", "stop", "take", "playonloc", "playonentity")) {
                action = arg.getValue();
                entry.addObject("action", new ElementTag(action));
                continue;
            }

            boolean handled = false;
            for (CommandArgumentDefinition def : argumentDefinitions) {
                def.parser().parse(entry, arg);
                if (entry.hasObject(def.name())) {
                    handled = true;
                    break;
                }
            }

            if (!handled) {
                arg.reportUnhandled();
            }
        }

        if (action == null) {
            throw new InvalidArgumentsException("You must specify an action: send, stop, take, playonloc, or playonentity.");
        }

        switch (action) {
            case "send" -> require(entry, "path");
            case "stop" -> require(entry, "id");
            case "playonloc" -> {
                require(entry, "location");
                require(entry, "distance");
                require(entry, "path");
            }
            case "playonentity" -> {
                require(entry, "entity");
                require(entry, "distance");
                require(entry, "path");
            }
        }
    }


    private void require(ScriptEntry entry, String key) throws InvalidArgumentsException {
        if (!entry.hasObject(key)) {
            throw new InvalidArgumentsException("Missing required argument: " + key);
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        String action = scriptEntry.getElement("action").asString();
        PlayerTag player = com.denizenscript.denizen.utilities.Utilities.getEntryPlayer(scriptEntry);

        VoiceServerPlayer voicePlayer = denizenAddon.getVoice().getPlayerManager()
                .getPlayerByName(player.getName())
                .orElseThrow(() -> new IllegalStateException("Voice player not found"));

        switch (action) {
            case "send" -> handleSend(scriptEntry, voicePlayer);
            case "stop" -> handleStop(scriptEntry);
            case "take" -> handleTake();
            case "playonloc" -> handlePlayOnLoc(scriptEntry);
            case "playonentity" -> handlePlayOnEntity(scriptEntry);
        }
    }

    private void handleSend(ScriptEntry entry, VoiceServerPlayer player) {
        String path = entry.getElement("path").asString();
        ServerDirectSource source = sourceLine.createDirectSource(player, false);
        entry.saveObject("audio_id", new ElementTag(source.getId().toString()));

        try {
            PlayerAudioSender.playSoundToPlayer(denizenAddon.getVoice(), source,
                    instance.getConfig().getString("settings.default-path") + "/" + path);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send audio: " + e.getMessage(), e);
        }
    }

    private void handleStop(ScriptEntry entry) {
        UUID id = UUID.fromString(entry.getElement("id").asString());
        Optional<ServerAudioSource<?>> sourceOpt = sourceLine.getSourceById(id);

        sourceOpt.ifPresentOrElse(
                source -> {
                    AudioSender sender = PlayerAudioSender.sources.get(source);
                    if (sender != null) sender.stop();
                },
                () -> System.err.println("Audio source not found for ID: " + id)
        );
    }

    private void handleTake() {
        instance.getLogger().severe("PLASMO TAKE is not implemented. Use PLASMO SEND instead.");
        throw new UnsupportedOperationException("PLASMO TAKE is under development.");
    }

    private void handlePlayOnLoc(ScriptEntry entry) {
        LocationTag loc = entry.getObjectTag("location");
        String path = entry.getElement("path").asString();
        short distance = Short.parseShort(entry.getElement("distance").asString());

        McServerWorld world = denizenAddon.getVoice().getMinecraftServer()
                .getWorlds().stream()
                .filter(w -> w.getName().equals(loc.getWorldName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("World not found"));

        ServerStaticSource source = sourceLine.createStaticSource(new ServerPos3d(world, loc.x(), loc.y(), loc.z()), false);
        source.setIconVisible(false);
        entry.saveObject("audio_id", new ElementTag(source.getId().toString()));

        PlayerAudioSender.playSoundOnLocation(denizenAddon.getVoice(), source,
                instance.getConfig().getString("settings.default-path") + "/" + path, distance);
    }

    private void handlePlayOnEntity(ScriptEntry entry) {
        EntityTag ent = entry.getObjectTag("entity");
        McServerEntity serverEntity = denizenAddon.getVoice().getMinecraftServer().getEntityByInstance(ent.getBukkitEntity());
        String path = entry.getElement("path").asString();
        short distance = Short.parseShort(entry.getElement("distance").asString());

        ServerEntitySource source = sourceLine.createEntitySource(serverEntity, false);
        entry.saveObject("audio_id", new ElementTag(source.getId().toString()));

        PlayerAudioSender.playSoundOnEntity(denizenAddon.getVoice(), source,
                instance.getConfig().getString("settings.default-path") + "/" + path, distance);
    }

    private record CommandArgumentDefinition(String name, boolean required, ArgumentParser parser) {}

    @FunctionalInterface
    private interface ArgumentParser {
        void parse(ScriptEntry entry, Argument arg) throws InvalidArgumentsException;
    }
}


