package com.marayd.denizenImplementation.DenizenHook;

import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.marayd.denizenImplementation.DenizenImplementation;

public class PlayerTagProc {


    private static final DenizenImplementation plugin = DenizenImplementation.instance;
    public static void start() {
        if (PlayerTag.tagProcessor != null) {
            plugin.getLogger().info("║ Registering tag for: " + ElementTag.class.getSimpleName() + " ║");
        } else {
            plugin.getLogger().severe("Tag processor or ElementTag is null.");
        }

    }
}
