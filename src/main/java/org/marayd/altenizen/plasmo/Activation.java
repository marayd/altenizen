package org.marayd.altenizen.plasmo;

import lombok.Getter;
import org.marayd.altenizen.Altenizen;
import su.plo.voice.api.server.audio.capture.ProximityServerActivationHelper;
import su.plo.voice.api.server.audio.capture.ServerActivation;

@Getter
public class Activation {
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
