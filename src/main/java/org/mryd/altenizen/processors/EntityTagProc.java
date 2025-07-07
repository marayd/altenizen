package org.mryd.altenizen.processors;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import org.bukkit.attribute.Attribute;
import org.mryd.altenizen.Altenizen;

import java.util.Objects;

public final class EntityTagProc {
    private static final Altenizen plugin = Altenizen.instance;

    public static void start() {
        if (EntityTag.tagProcessor != null) {
            EntityTag.tagProcessor.registerMechanism("entity_scale", false, ElementTag.class, (object, mechanism, value) -> {
                Objects.requireNonNull(object.getLivingEntity().getAttribute(Attribute.SCALE)).setBaseValue(value.asDouble());
            });
            EntityTag.tagProcessor.registerMechanism("entity_jump_strength", false, ElementTag.class, (object, mechanism, value) -> {
                Objects.requireNonNull(object.getLivingEntity().getAttribute(Attribute.JUMP_STRENGTH)).setBaseValue(value.asDouble());
            });
        }
        else {
            plugin.getLogger().severe("Entity TagProcessor is null.");
        }

    }
}
