package org.mryd.altenizen.command;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import org.json.JSONObject;
import org.mryd.altenizen.plasmo.PlasmoVoiceAddon;

import java.util.Base64;

public class VoskCommand extends AbstractCommand implements Holdable {

    public VoskCommand() {
        setName("vosk");
        setSyntax("vosk [bytes:<bytes>]");
        setRequiredArguments(1, 1);
        isProcedural = false;
        autoCompile();
    }

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgPrefixed @ArgName("bytes") ElementTag actionTag) {
        byte[] bytes = Base64.getDecoder().decode(actionTag.asString());
        PlasmoVoiceAddon.recognizeFromBytesAsync(bytes).thenAccept(result -> {
            scriptEntry.saveObject("recognized_text", new ElementTag(new JSONObject(result).getString("text")));
            scriptEntry.setFinished(true);
        }).exceptionally(ex -> {
            Debug.echoError(ex);
            scriptEntry.setFinished(true);
            return null;
        });
    }


}
