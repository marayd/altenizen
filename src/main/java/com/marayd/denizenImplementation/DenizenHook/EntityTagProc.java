package com.marayd.denizenImplementation.DenizenHook;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import org.bukkit.attribute.Attribute;
import com.marayd.denizenImplementation.DenizenImplementation;

import java.util.Objects;

public class EntityTagProc {
    private static final DenizenImplementation plugin = DenizenImplementation.instance;

    public static void start() {
        if (EntityTag.tagProcessor != null) {
            EntityTag.tagProcessor.registerMechanism("entity_scale", false, ElementTag.class, (object, mechanism, value) -> {
                Objects.requireNonNull(object.getLivingEntity().getAttribute(Attribute.GENERIC_SCALE)).setBaseValue(value.asDouble());
            });
            EntityTag.tagProcessor.registerMechanism("entity_jump_strength", false, ElementTag.class, (object, mechanism, value) -> {
                Objects.requireNonNull(object.getLivingEntity().getAttribute(Attribute.GENERIC_JUMP_STRENGTH)).setBaseValue(value.asDouble());
            });
        }
        else {
            plugin.getLogger().severe("Entity TagProcessor is null.");
        }

    }
}
