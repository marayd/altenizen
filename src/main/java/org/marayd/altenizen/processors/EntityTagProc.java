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

package org.marayd.altenizen.processors;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import org.bukkit.attribute.Attribute;
import org.marayd.altenizen.Altenizen;

import java.util.Objects;

public class EntityTagProc {
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
