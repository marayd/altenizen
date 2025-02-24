package com.marayd.denizenImplementation.command;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.BracedCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.marayd.denizenImplementation.VelocityHook.VelocityHook;

import java.util.List;
import java.util.Map;

public class VelocityCommand extends AbstractCommand implements Holdable {
    public VelocityCommand() {
        setName("velocity");
        setSyntax("velocity <server> { - <[command]> }");
        setRequiredArguments(2, 2);
        isProcedural = true;
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("servers")) {
                scriptEntry.addObject("servers", arg.asType(ListTag.class));
            }
            else if (arg.matches("{")) {
                break;
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("servers")) {
            throw new InvalidArgumentsException("Must define servers to run the script on.");
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        ListTag servers = scriptEntry.getObjectTag("servers");
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), servers);
        }

        List<ScriptEntry> bracedCommandsList = BracedCommand.getBracedCommandsDirect(scriptEntry, scriptEntry);
        if (bracedCommandsList == null || bracedCommandsList.isEmpty()) {
            Debug.echoError("Empty subsection - did you forget a ':'?");
            return;
        }
        StringBuilder toSend = new StringBuilder();
        for (ScriptEntry entry : bracedCommandsList) {
            toSend.append(stringify(entry)).append("\n");
        }

        for (String server : servers) {
            VelocityHook.executeBackendCommand(server, String.valueOf(toSend));
        }
    }



    public static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
    }

    public static String stringify(Object obj) {
        if (obj instanceof ScriptEntry) {
            ScriptEntry entry = (ScriptEntry) obj;
            if (!(entry.internal.yamlSubcontent instanceof List)) {
                return escape(entry.internal.originalLine);
            }
            StringBuilder result = new StringBuilder();
            result.append(escape(entry.internal.originalLine)).append("\r");
            for (Object subCommand : (List<Object>) entry.internal.yamlSubcontent) {
                result.append(stringify(subCommand)).append("\r");
            }
            return result.toString();
        }
        else if (obj instanceof Map) {
            Map<Object, Object> valueMap = (Map<Object, Object>) obj;
            StringBuilder result = new StringBuilder();
            Object cmdLine = valueMap.keySet().toArray()[0];
            result.append(escape(cmdLine.toString())).append("\r");
            List<Object> inside = (List<Object>) valueMap.get(cmdLine);
            for (Object subCommand : inside) {
                result.append(stringify(subCommand)).append("\r");
            }
            return result.toString();
        }
        else {
            return escape(obj.toString());
        }
    }

}
