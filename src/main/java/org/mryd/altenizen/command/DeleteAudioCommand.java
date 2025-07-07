package org.mryd.altenizen.command;

import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import org.mryd.altenizen.Altenizen;

import java.io.File;
import java.util.Objects;

public class DeleteAudioCommand extends AbstractCommand implements Holdable {
    public DeleteAudioCommand() {
        setName("delete_audio");
        setSyntax("delete_audio name:<[name]>");
        isProcedural = true;
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) {
        for (Argument arg : scriptEntry) {
            if (arg.matchesPrefix("name") && !scriptEntry.hasObject("name")) {
                scriptEntry.addObject("name", arg.asElement());
            } else {
                arg.reportUnhandled();
            }
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        String name = scriptEntry.getObject("name").toString();
        if (scriptEntry.hasObject("name")) {
            System.out.println(Objects.requireNonNull(Altenizen.instance.getConfig().getString("settings.default-path")) + "/" + name);
            File audio = new File(Objects.requireNonNull(Altenizen.instance.getConfig().getString("settings.default-path")) + name);
            if (audio.delete()) {
                scriptEntry.saveObject("success", new ElementTag(true));
            }
            else {
                scriptEntry.saveObject("success", new ElementTag(false));
            }

        }
    }

}
