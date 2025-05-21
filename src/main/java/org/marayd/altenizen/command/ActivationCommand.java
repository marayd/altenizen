package org.marayd.altenizen.command;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import org.marayd.altenizen.Altenizen;
import org.marayd.altenizen.plasmo.Activation;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.capture.ServerActivation;

import java.util.*;

public class ActivationCommand extends AbstractCommand implements Holdable {

    public ActivationCommand() {
        setName("plasmoactivation");
        setSyntax("plasmoactivation create name:<name> translation:<translation> texture:<texture> permission:<permission> weight:<weight>");
        isProcedural = true;
    }

    private static final Set<String> REQUIRED_ARGS = Set.of("name", "translation", "texture", "permission", "weight");

    @Override
    public void parseArgs(ScriptEntry entry) throws InvalidArgumentsException {
        String action = null;

        for (Argument arg : entry) {
            if (arg.matches("create") && action == null) {
                action = "create";
                entry.addObject("action", new ElementTag(action));
                continue;
            }

            if (arg.matches("delete") && action == null) {
                action = "delete";
                entry.addObject("action", new ElementTag(action));
                continue;
            }

            for (String key : REQUIRED_ARGS) {
                if (arg.matchesPrefix(key) && !entry.hasObject(key)) {
                    entry.addObject(key, arg.asElement());
                    break;
                }
            }

            if (arg.matchesPrefix("name") && !entry.hasObject("name")) {
                entry.addObject("name", arg.asElement());
            }

            if (!arg.hasPrefix() || (!REQUIRED_ARGS.contains(arg.getPrefix().toString().toLowerCase()) && !"name".equalsIgnoreCase(arg.getPrefix().toString()))) {
                arg.reportUnhandled();
            }
        }

        if (action == null) throw new InvalidArgumentsException("You must specify an action: 'create' or 'delete'.");

        switch (action) {
            case "create" -> {
                for (String key : REQUIRED_ARGS) {
                    if (!entry.hasObject(key)) throw new InvalidArgumentsException("Missing required argument: " + key);
                }
            }
            case "delete" -> {
                if (!entry.hasObject("name")) throw new InvalidArgumentsException("Missing required argument: name for delete.");
            }
        }

    }

    @Override
    public void execute(ScriptEntry entry) {
        String action = entry.getElement("action").asString();

        switch (action.toLowerCase()) {
            case "create" -> handleCreate(entry);
            case "delete" -> handleDelete(entry);
        }


        entry.setFinished(true);
    }

    private void handleDelete(ScriptEntry entry) {
        String name = entry.getElement("name").asString();
        PlasmoVoiceServer voiceServer = Altenizen.PLASMO_VOICE_ADDON.getVoice();

        voiceServer.getActivationManager().unregister(name);
    }


    private void handleCreate(ScriptEntry entry) {
        try {
            String name = entry.getElement("name").asString();
            String translation = entry.getElement("translation").asString();
            String texture = entry.getElement("texture").asString();
            String permission = entry.getElement("permission").asString();
            int weight = entry.getElement("weight").asInt();

            PlasmoVoiceServer voiceServer = Altenizen.PLASMO_VOICE_ADDON.getVoice();

            ServerActivation serverActivation = voiceServer.getActivationManager().createBuilder(
                    Altenizen.PLASMO_VOICE_ADDON,
                    name,
                    translation,
                    texture,
                    permission,
                    weight
            ).build();
            new Activation(serverActivation, name);
        } catch (Exception ex) {
            Debug.echoError(entry, "Failed to create Plasmo activation: " + ex.getMessage());
        }
    }
}
