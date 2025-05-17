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

package org.marayd.altenizen.plasmo;

import lombok.Getter;
import org.marayd.altenizen.Altenizen;
import su.plo.voice.api.server.audio.capture.ProximityServerActivationHelper;
import su.plo.voice.api.server.audio.capture.ServerActivation;

@Getter
public final class Activation {
    private final ServerActivation activation;
    private final ProximityServerActivationHelper proximityHelper;

    public Activation(ServerActivation activation, String name) {
        this.activation = activation;
        this.proximityHelper = new ProximityServerActivationHelper(
                Altenizen.PLASMO_VOICE_ADDON.getVoice(),
                activation,
                PlasmoVoiceAddon.sourceLine
        );
    }
}
