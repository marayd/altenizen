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

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import org.json.JSONObject;
import org.marayd.altenizen.plasmo.PlasmoVoiceAddon;

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
