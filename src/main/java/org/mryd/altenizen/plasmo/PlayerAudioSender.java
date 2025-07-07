package org.mryd.altenizen.plasmo;

import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.provider.ArrayAudioFrameProvider;
import su.plo.voice.api.server.audio.source.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public final class PlayerAudioSender {
    public static HashMap<ServerAudioSource<?>, AudioSender> sources = new HashMap<>();

    public static void playAudioFromBytes(PlasmoVoiceServer voiceServer, ServerAudioSource<?> source, byte[] audioData_, Short distance) {
        CompletableFuture.runAsync(() -> {
            try {
                byte[] audioData = audioData_;

                if (audioData.length % 2 != 0) {
                    audioData = Arrays.copyOf(audioData, audioData.length + 1);
                }

                short[] samples = convertBytesToShorts(audioData);


                getFrameProvider(voiceServer, source, distance, samples);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void playAudioFromFile(PlasmoVoiceServer voiceServer, ServerAudioSource<?> source, String filePath, Short distance) {
        CompletableFuture.runAsync(() -> {
            try {
                short[] samples = loadAudioFile(filePath).get();
                getFrameProvider(voiceServer, source, distance, samples);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void getFrameProvider(PlasmoVoiceServer voiceServer, ServerAudioSource<?> source, Short distance, short[] samples) {
        ArrayAudioFrameProvider frameProvider = new ArrayAudioFrameProvider(voiceServer, false);
        frameProvider.addSamples(samples);

        AudioSender audioSender = createSender(source, frameProvider, distance);
        audioSender.start();
        sources.put(source, audioSender);

        audioSender.onStop(() -> {
            frameProvider.close();
            source.remove();
            sources.remove(source);
        });
    }

    public static AudioSender createSender(ServerAudioSource<?> source, ArrayAudioFrameProvider provider, Short distance) {
        return switch (source) {
            case ServerDirectSource direct -> direct.createAudioSender(provider);
            case ServerStaticSource stat when distance != null -> stat.createAudioSender(provider, distance);
            case ServerEntitySource entity when distance != null -> entity.createAudioSender(provider, distance);
            case null, default -> throw new IllegalArgumentException("Unsupported source type or missing distance");
        };
    }

    private static short[] convertBytesToShorts(byte[] audioData) {
        int shortArrayLength = audioData.length / 2;
        short[] samples = new short[shortArrayLength];

        for (int i = 0; i < shortArrayLength; i++) {
            int byteIndex = i * 2;

            if (byteIndex + 1 >= audioData.length) {
                System.err.println("[WARNING] Incomplete sample at index " + i + " (insufficient bytes), skipping.");
                break;
            }

            int low = audioData[byteIndex] & 0xFF;
            int high = audioData[byteIndex + 1] << 8;
            samples[i] = (short) (high | low);
        }

        return samples;
    }

    private static CompletableFuture<short[]> loadAudioFile(String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File file = new File(filePath);
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);

                AudioFormat targetFormat = new AudioFormat(48000, 16, 1, true, false);
                AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);

                int frameSize = targetFormat.getFrameSize();
                int numFrames = (int) convertedStream.getFrameLength();
                short[] samples = new short[numFrames];

                byte[] buffer = new byte[frameSize];
                for (int i = 0; i < numFrames; i++) {
                    if (convertedStream.read(buffer) != frameSize) break;

                    int low = buffer[0] & 0xFF;
                    int high = buffer[1] << 8;
                    samples[i] = (short) (high | low);
                }

                return samples;
            } catch (IOException | UnsupportedAudioFileException e) {
                throw new RuntimeException(e);
            }
        });
    }
}