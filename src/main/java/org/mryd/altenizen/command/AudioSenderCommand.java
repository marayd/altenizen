package org.mryd.altenizen.command;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultNull;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import org.checkerframework.checker.units.qual.A;
import org.mryd.altenizen.Altenizen;
import org.mryd.altenizen.plasmo.PlayerAudioSender;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.provider.ArrayAudioFrameProvider;
import su.plo.voice.api.server.audio.source.AudioSender;
import su.plo.voice.api.server.audio.source.ServerAudioSource;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.mryd.altenizen.plasmo.PlayerAudioSender.createSender;

public class AudioSenderCommand extends AbstractCommand {

    public static final Map<String, AudioSender> audioSenders = new HashMap<>();
    private static final Map<AudioSender, ArrayAudioFrameProvider> frameProviders = new HashMap<>();

    private static final PlasmoVoiceServer voiceServer = Altenizen.getPLASMO_VOICE_ADDON().getVoice();

    public AudioSenderCommand() {
        setName("plasmoaudiosender");
        setSyntax("audio_sender [action:create|start|stop|delete|add] [source:source_name] [bytes:<ElementTag>] [distance:<#>]");
        autoCompile();
    }

    @SuppressWarnings("unused")
    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgPrefixed @ArgName("action") ElementTag actionTag,
                                   @ArgPrefixed @ArgName("name") ElementTag nameTag,
                                   @ArgPrefixed @ArgName("source") ElementTag sourceTag,
                                   @ArgPrefixed @ArgName("bytes") ElementTag bytesTag,
                                   @ArgPrefixed @ArgName("typebytes") ElementTag typeBytesTag,
                                   @ArgDefaultNull @ArgPrefixed @ArgName("distance") ElementTag typeTag,
                                   @ArgDefaultNull @ArgPrefixed @ArgName("entity") EntityTag entity) {
        String action = actionTag.asLowerString();
        String name = nameTag.asString();
        String typeBytes = typeBytesTag.asString();
        int distance = typeTag.asInt();
        var source = SourceCommand.sources.get(sourceTag.asString());
        byte[] bytes = Base64.getDecoder().decode(bytesTag.asString());
        boolean isEncrypted = false;
        boolean isOpusEncoded = false;

        switch (typeBytes) {
            case "encrypted" -> isEncrypted = true;
            case "unencrypted" -> isEncrypted = false;
            case "opus" -> isOpusEncoded = true;
            case "raw" -> isEncrypted = false;
            default -> {
                isEncrypted = false;
                isOpusEncoded = false;
            }
        }

        if (source == null) {
            throw new IllegalArgumentException("Source '" + sourceTag.asString() + "' does not exist.");
        }
        
        switch (action) {
            case "create" -> createAudioSender(name, source, distance);
            case "start" -> startAudioSender(name);
            case "stop" -> stopAudioSender(name);
            case "delete" -> deleteAudioSender(name);
            case "add" -> addBytesToAudioSender(source, name, bytes, isEncrypted, isOpusEncoded);
            default -> Debug.echoError("Invalid action: " + action);
        }
    }

    private static void createAudioSender(String name, ServerAudioSource<?> source, int distance) {
        ArrayAudioFrameProvider frameProvider = new ArrayAudioFrameProvider(voiceServer, false);
        AudioSender audioSender = createSender(source, frameProvider, (short) distance);
        audioSenders.put(name, audioSender);
        frameProviders.put(audioSender, frameProvider);
    }

    private static void startAudioSender(String name) {
        AudioSender audioSender = audioSenders.get(name);
        if (audioSender != null) {
            audioSender.start();
        } else {
            throw new IllegalArgumentException("Audio sender with name '" + name + "' does not exist.");
        }
    }

    private static void stopAudioSender(String name) {
        AudioSender audioSender = audioSenders.get(name);
        if (audioSender != null) {
            audioSender.stop();
        } else {
            throw new IllegalArgumentException("Audio sender with name '" + name + "' does not exist.");
        }
    }

    private static void deleteAudioSender(String name) {
        AudioSender audioSender = audioSenders.remove(name);
        if (audioSender != null) {
            ArrayAudioFrameProvider frameProvider = frameProviders.remove(audioSender);
            if (frameProvider != null) {
                frameProvider.close();
            }
            audioSender.stop();
        } else {
            throw new IllegalArgumentException("Audio sender with name '" + name + "' does not exist.");
        }
    }

    private static void addBytesToAudioSender(ServerAudioSource<?> source, String name, byte[] bytes, boolean isEncrypted, boolean isOpusEncoded) {
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null.");
        }
        AudioSender audioSender = audioSenders.get(name);
        if (audioSender == null) {
            throw new IllegalArgumentException("Audio sender with name '" + name + "' does not exist.");
        }

        ArrayAudioFrameProvider frameProvider = frameProviders.get(audioSender);
        if (frameProvider == null) {
            throw new IllegalArgumentException("Audio sender '" + name + "' has no frame provider.");
        }

        short[] samples = PlayerAudioSender.convertBytesToShorts(bytes);
        if (isEncrypted) {
            try {
                addFrame(frameProvider, bytes);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else if (isOpusEncoded) {
            frameProvider.addEncodedFrame(bytes);
        } else {
            frameProvider.addSamples(samples);
        }


        audioSender.start();
    }


    // The hell starts here
    // Good luck to anyone who tries to maintain this code in the future. (im totally sure that it'll be me)
    private static void addFrame(Object providerInstance, byte[] frame) throws IllegalAccessException {
        Class<?> clazz = providerInstance.getClass();
        Field framesField = null;
        Class<?> current = clazz;
        while (current != null) {
            try {
                framesField = current.getDeclaredField("frames");
                break;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        if (framesField == null) {
            throw new RuntimeException("Field 'frames' not found");
        }

        framesField.setAccessible(true);

        @SuppressWarnings("unchecked")
        Collection<byte[]> framesCollection = (Collection<byte[]>) framesField.get(providerInstance);

        framesCollection.add(frame);
    }

}
