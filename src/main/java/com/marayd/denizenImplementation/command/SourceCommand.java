package com.marayd.denizenImplementation.command;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
// TODO:
public class SourceCommand {
//
//    public SourceCommand() {
//        setName("source");
//        setSyntax("source [name] type:[type] play:[play]");
//    }
//
//    @Override
//    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
//        ElementTag name = null;
//        ElementTag type = null;
//        ElementTag play = null;
//
//        for (Argument arg : scriptEntry) {
//            // Обработка аргумента 'name'
//            if (arg.matchesPrefix("create") && !scriptEntry.hasObject("name")) {
//                name = arg.asType(ElementTag.class);
//                scriptEntry.addObject("name", name);
//            }
//            // Обработка аргумента 'type'
//            else if (arg.matchesPrefix("type") && !scriptEntry.hasObject("type")) {
//                type = arg.asType(ElementTag.class);
//                scriptEntry.addObject("type", type);
//            }
//            // Обработка аргумента 'play'
//            else if (arg.matchesPrefix("play") && !scriptEntry.hasObject("play")) {
//                play = arg.asType(ElementTag.class);
//                scriptEntry.addObject("play", play);
//            }
//
//            else if (arg.matchesPrefix())
//            // Если аргумент не найден, отчет о необработанном аргументе
//            else {
//                arg.reportUnhandled();
//            }
//        }
//    }
    public SourceCommand() {
        return;
    }
}
