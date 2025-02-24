package com.marayd.denizenImplementation.command;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.Utilities;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.marayd.denizenImplementation.DenizenImplementation;
import org.bukkit.Bukkit;
import su.plo.slib.api.server.entity.McServerEntity;
import su.plo.slib.api.server.position.ServerPos3d;
import su.plo.slib.api.server.world.McServerWorld;
import su.plo.voice.api.server.audio.source.*;
import su.plo.voice.api.server.player.VoiceServerPlayer;
import com.marayd.denizenImplementation.PlasmoHook.PlayerAudioSender;

import java.util.Optional;
import java.util.UUID;

import static com.marayd.denizenImplementation.DenizenImplementation.denizenAddon;
import static com.marayd.denizenImplementation.DenizenImplementation.instance;
import static com.marayd.denizenImplementation.PlasmoHook.DenizenAddon.sourceLine;
import static com.marayd.denizenImplementation.PlasmoHook.PlayerAudioSender.sources;

public class PlasmoHookCommand extends AbstractCommand implements Holdable {
    public PlasmoHookCommand() {
        setName("plasmo");
        setSyntax("plasmo (take|send|stop|playonloc,playonentity) path:<path> location:<loc> distance:<distance> [id:<id>]");
        // setRequiredArguments(2, 2);
        isProcedural = true;
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        boolean actionSet = false;

        for (Argument arg : scriptEntry) {
            String action = null;

            if (arg.matchesPrefix("path") && !scriptEntry.hasObject("path")) {
                scriptEntry.addObject("path", arg.asElement());
            } else if (arg.matchesPrefix("id") && !scriptEntry.hasObject("id")) {
                scriptEntry.addObject("id", arg.asElement());
            } else if (arg.matchesPrefix("location") && !scriptEntry.hasObject("location")) {
                scriptEntry.addObject("location", arg.asType(LocationTag.class));
            } else if (arg.matchesPrefix("distance") && !scriptEntry.hasObject("distance")) {
                scriptEntry.addObject("distance", arg.asElement());
            } else if (arg.matchesPrefix("entity") && !scriptEntry.hasObject("entity")) {
                scriptEntry.addObject("entity", arg.asType(EntityTag.class));
            } else if (!actionSet) {
                if (arg.matches("stop")) {
                    action = "stop";
                } else if (arg.matches("take")) {
                    action = "take";
                } else if (arg.matches("send")) {
                    action = "send";
                } else if (arg.matches("playonloc")) {
                    action = "playonloc";
                } else if (arg.matches("playonentity")) {
                    action = "playonentity";
                }

                if (action != null) {
                    scriptEntry.addObject("action", action);
                    actionSet = true;
                } else {
                    arg.reportUnhandled();
                }
            } else {
                arg.reportUnhandled();
            }
        }

        validateArguments(scriptEntry);
    }

    private void validateArguments(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        String action = scriptEntry.getObject("action") != null ? scriptEntry.getObject("action").toString() : null;

        if (action == null) {
            throw new InvalidArgumentsException("Must specify a valid action (take, send, stop, playonloc, playonentity) with required arguments.");
        }

        switch (action.toLowerCase()) {
            case "send":
                if (!scriptEntry.hasObject("path")) {
                    throw new InvalidArgumentsException("Action 'send' requires a 'path' argument.");
                }
                break;
            case "stop":
                if (!scriptEntry.hasObject("id")) {
                    throw new InvalidArgumentsException("Action 'stop' requires an 'id' argument.");
                }
                break;
            case "playonloc":
                if (!scriptEntry.hasObject("location") && !scriptEntry.hasObject("distance")) {
                    throw new InvalidArgumentsException("Action 'playonloc' requires a 'location' and 'distance' argument.");
                }
                break;
            case "take":
                // No specific validations for 'take'
                break;
            case "playonentity":
                if (!scriptEntry.hasObject("entity") && !scriptEntry.hasObject("distance") && !scriptEntry.hasObject("path")) {
                    throw new InvalidArgumentsException("Action 'playonentity' requires 'entity' & 'distance' & 'path' argument.");
                }
                break;
            default:
                throw new InvalidArgumentsException("Unsupported action: " + action);
        }
    }
    @Override
    public void execute(ScriptEntry scriptEntry) {
        PlayerTag playerTag = Utilities.getEntryPlayer(scriptEntry);


        String action = scriptEntry.getObject("action").toString();

        VoiceServerPlayer voicePlayer = denizenAddon.getVoice().getPlayerManager()
                .getPlayerByName(playerTag.getName())
                .orElseThrow(() -> new IllegalStateException("Player not found"));

        if ("send".equalsIgnoreCase(action)) {
            String path = scriptEntry.getObject("path").toString();
            ServerDirectSource source = sourceLine.createDirectSource(voicePlayer, false);

            scriptEntry.saveObject("audio_id", new ElementTag(String.valueOf(source.getId())));
            try {
                System.out.println("Starting playback...");
                PlayerAudioSender.playSoundToPlayer(denizenAddon.getVoice(), source, DenizenImplementation.instance.getConfig().getString("settings.default-path") + "/" + path);

                System.out.println("Playback initiated successfully. DBUG: " + source);
            } catch (Exception e) {
                System.err.println("Error initiating playback: " + e.getMessage());
                throw new RuntimeException(e);
            }
        } else if ("take".equalsIgnoreCase(action)) {
            instance.getLogger().severe("==== DON'T USE \"PLASMO TAKE\" IT'S ON DEVELOPMENT ====");
            instance.getLogger().severe("==== If you want to send audio use instead \"PLASMO SEND\" ====");
            instance.getLogger().severe("==== If you want to SAVE audio use on player ends speak event ====");
            throw new UnsupportedOperationException("This method is not implemented yet.");
        } else if ("playonloc".equalsIgnoreCase(action)) {
            LocationTag location = scriptEntry.getObjectTag("location");
            String path = scriptEntry.getObject("path").toString();
            String distance = scriptEntry.getObject("distance").toString();

            McServerWorld world = denizenAddon.getVoice().getMinecraftServer()
                    .getWorlds()
                    .stream()
                    .filter(w -> w.getName().equals(location.getWorldName()))
                    .findAny()
                    .orElseThrow(() -> new IllegalStateException("World not found."));

            ServerPos3d position = new ServerPos3d(world, location.x(), location.y(), location.z());
            ServerStaticSource source = sourceLine.createStaticSource(position, false);
            source.setIconVisible(false);
            scriptEntry.saveObject("audio_id", new ElementTag(String.valueOf(source.getId())));
            PlayerAudioSender.playSoundOnLocation(denizenAddon.getVoice(), source, DenizenImplementation.instance.getConfig().getString("settings.default-path") + "/" + path, Short.parseShort(distance));
        }
        else if ("stop".equalsIgnoreCase(action)) {
            Optional<ServerAudioSource<?>> source = sourceLine.getSourceById(UUID.fromString(scriptEntry.getObjectTag("id").toString()));

            if (source.isPresent()) {  // Check if the Optional has a value
                AudioSender audioSender = sources.get(source.get());
                if (audioSender != null) {  // Check if the key exists in the HashMap
                    audioSender.stop();
                } else {
                    System.err.println("No AudioSender found for the provided ServerDirectSource.");
                }
            } else {
                System.err.println("No ServerAudioSource found with the provided ID.");
            }
        }
        else if ("playonentity".equalsIgnoreCase(action)) {
            EntityTag denizenEntity = scriptEntry.getObjectTag("entity");
            Object bukkitEntity = denizenEntity.getBukkitEntity();
            McServerEntity entity = denizenAddon.getVoice().getMinecraftServer().getEntityByInstance(bukkitEntity);
            ServerEntitySource source = sourceLine.createEntitySource(entity, false);
            scriptEntry.saveObject("audio_id", new ElementTag(String.valueOf(source.getId())));
            String path = scriptEntry.getObject("path").toString();
            PlayerAudioSender.playSoundOnEntity(denizenAddon.getVoice(), source, DenizenImplementation.instance.getConfig().getString("settings.default-path") + "/" + path, (short) 15);
        }
    }

}
