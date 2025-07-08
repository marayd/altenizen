package org.mryd.altenizen.command;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultNull;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import org.mryd.altenizen.plasmo.PlayerAudioSender;
import su.plo.voice.api.server.audio.provider.ArrayAudioFrameProvider;
import su.plo.voice.api.server.audio.source.AudioSender;
import su.plo.voice.api.server.audio.source.ServerAudioSource;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.mryd.altenizen.Altenizen.PLASMO_VOICE_ADDON;
import static org.mryd.altenizen.plasmo.PlayerAudioSender.createSender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AudioSenderCommand extends AbstractCommand {

    private static final Logger log = LoggerFactory.getLogger(AudioSenderCommand.class);

    public static final Map<String, AudioSender> audioSenders = new HashMap<>();
    private static final Map<AudioSender, ArrayAudioFrameProvider> frameProviders = new HashMap<>();

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
                                   @ArgDefaultNull @ArgPrefixed @ArgName("bytes") ElementTag bytesTag,
                                   @ArgDefaultNull @ArgPrefixed @ArgName("typebytes") ElementTag typeBytesTag,
                                   @ArgDefaultNull @ArgPrefixed @ArgName("distance") ElementTag typeTag,
                                   @ArgDefaultNull @ArgPrefixed @ArgName("entity") EntityTag entity) {

        String action = actionTag.asLowerString();
        String name = nameTag.asString();
        String typeBytes = typeBytesTag.asString();
        int distance = typeTag.asInt();
        var source = SourceCommand.sources.get(sourceTag.asString());

        log.info("Executing action '{}' for audio sender '{}', source '{}', distance '{}'", action, name, sourceTag.asString(), distance);

        if (source == null) {
            log.error("Source '{}' does not exist.", sourceTag.asString());
            throw new IllegalArgumentException("Source '" + sourceTag.asString() + "' does not exist.");
        }

        byte[] bytes = new byte[0];
        boolean isEncrypted = true;
        boolean isOpusEncoded = true;

        if (bytesTag != null) {
            bytes = Base64.getDecoder().decode(bytesTag.asString());
            log.info("Decoded {} bytes from base64", bytes.length);
        }

        if (typeBytes != null) {
            switch (typeBytes) {
                case "encrypted" -> {
                    isEncrypted = true;
                    log.info("Byte type is 'encrypted'");
                }
                case "opus" -> {
                    isOpusEncoded = true;
                    log.info("Byte type is 'opus'");
                }
                default -> log.info("Byte type is 'raw'");
            }
        }

        try {
            switch (action) {
                case "create" -> createAudioSender(name, source, distance);
                case "start" -> startAudioSender(name);
                case "stop" -> stopAudioSender(name);
                case "delete" -> deleteAudioSender(name);
                case "add" -> addBytesToAudioSender(source, name, bytes, isEncrypted, isOpusEncoded);
                default -> log.error("Invalid action: {}", action);
            }
        } catch (Exception e) {
            log.error("Error executing action '{}': {}", action, e.getMessage(), e);
            throw e;
        }
    }

    private static void createAudioSender(String name, ServerAudioSource<?> source, int distance) {
        log.info("Creating audio sender '{}' with distance '{}'", name, distance);
        ArrayAudioFrameProvider frameProvider = new ArrayAudioFrameProvider(PLASMO_VOICE_ADDON.getVoice(), false);
        AudioSender audioSender = createSender(source, frameProvider, (short) distance);
        audioSenders.put(name, audioSender);
        frameProviders.put(audioSender, frameProvider);
        log.info("Audio sender '{}' created successfully", name);
    }

    private static void startAudioSender(String name) {
        log.info("Starting audio sender '{}'", name);
        AudioSender audioSender = audioSenders.get(name);
        if (audioSender != null) {
            audioSender.start();
            log.info("Audio sender '{}' started", name);
        } else {
            log.error("Audio sender '{}' does not exist", name);
            throw new IllegalArgumentException("Audio sender with name '" + name + "' does not exist.");
        }
    }

    private static void stopAudioSender(String name) {
        log.info("Stopping audio sender '{}'", name);
        AudioSender audioSender = audioSenders.get(name);
        if (audioSender != null) {
            audioSender.stop();
            log.info("Audio sender '{}' stopped", name);
        } else {
            log.error("Audio sender '{}' does not exist", name);
            throw new IllegalArgumentException("Audio sender with name '" + name + "' does not exist.");
        }
    }

    private static void deleteAudioSender(String name) {
        log.info("Deleting audio sender '{}'", name);
        AudioSender audioSender = audioSenders.remove(name);
        if (audioSender != null) {
            ArrayAudioFrameProvider frameProvider = frameProviders.remove(audioSender);
            if (frameProvider != null) {
                frameProvider.close();
                log.info("Frame provider for '{}' closed", name);
            }
            audioSender.stop();
            log.info("Audio sender '{}' deleted", name);
        } else {
            log.error("Audio sender '{}' does not exist", name);
            throw new IllegalArgumentException("Audio sender with name '" + name + "' does not exist.");
        }
    }

    private static void addBytesToAudioSender(ServerAudioSource<?> source, String name, byte[] bytes, boolean isEncrypted, boolean isOpusEncoded) {
        log.info("Adding bytes to audio sender '{}': encrypted={}, opus={}", name, isEncrypted, isOpusEncoded);
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null.");
        }

        AudioSender audioSender = audioSenders.get(name);
        if (audioSender == null) {
            log.error("Audio sender '{}' does not exist", name);
            throw new IllegalArgumentException("Audio sender with name '" + name + "' does not exist.");
        }

        ArrayAudioFrameProvider frameProvider = frameProviders.get(audioSender);
        if (frameProvider == null) {
            log.error("Audio sender '{}' has no frame provider", name);
            throw new IllegalArgumentException("Audio sender '" + name + "' has no frame provider.");
        }

        if (isEncrypted) {
            try {
                addFrame(frameProvider, bytes);
                log.info("Encrypted frame added to '{}'", name);
            } catch (IllegalAccessException e) {
                log.error("Failed to add encrypted frame to '{}': {}", name, e.getMessage(), e);
                throw new RuntimeException(e);
            }
        } else if (isOpusEncoded) {
            frameProvider.addEncodedFrame(bytes);
            log.info("Opus-encoded frame added to '{}'", name);
        } else {
            short[] samples = PlayerAudioSender.convertBytesToShorts(bytes);
            frameProvider.addSamples(samples);
            log.info("Raw PCM samples added to '{}'", name);
        }

        audioSender.start();
        log.info("Audio sender '{}' started (after adding frame)", name);
    }

    private static void addFrame(Object providerInstance, byte[] frame) throws IllegalAccessException {
        log.info("Accessing private frame collection to add encrypted frame...");
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
            log.error("Field 'frames' not found in class hierarchy");
            throw new RuntimeException("Field 'frames' not found");
        }

        framesField.setAccessible(true);

        @SuppressWarnings("unchecked")
        Collection<byte[]> framesCollection = (Collection<byte[]>) framesField.get(providerInstance);

        framesCollection.add(frame);
        log.info("Encrypted frame added to private 'frames' collection");
    }
}
