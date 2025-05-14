package org.marayd.altenizen.plasmo;

import su.plo.voice.api.audio.codec.AudioEncoder;
import su.plo.voice.api.audio.codec.CodecException;
import su.plo.voice.api.encryption.Encryption;
import su.plo.voice.api.encryption.EncryptionException;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.provider.ArrayAudioFrameProvider;
import su.plo.voice.api.server.audio.source.AudioSender;
import su.plo.voice.api.server.audio.source.ServerAudioSource;
import su.plo.voice.api.server.audio.source.ServerDirectSource;
import su.plo.voice.api.server.audio.source.ServerStaticSource;
import su.plo.voice.api.server.audio.source.ServerEntitySource;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.sound.sampled.*;

public final class PlayerAudioSender {
    public static HashMap<ServerAudioSource<?>, AudioSender> sources = new HashMap<>();

    public static void playSoundToPlayer(PlasmoVoiceServer voiceServer, ServerDirectSource source, byte[] audioData) {
        CompletableFuture.runAsync(() -> {
            short[] samples = convertBytesToShorts(audioData);

            ArrayAudioFrameProvider frameProvider = new ArrayAudioFrameProvider(voiceServer, false);
            frameProvider.addSamples(samples);

            AudioSender audioSender = source.createAudioSender(frameProvider);
            audioSender.start();

            sources.put(source, audioSender);

            audioSender.onStop(() -> {
                frameProvider.close();
                source.remove();
                sources.remove(source);
            });
        });
    }

    public static void playSoundOnLocation(PlasmoVoiceServer voiceServer, ServerStaticSource source, byte[] audioData, short distance) {
        CompletableFuture.runAsync(() -> {
            short[] samples = convertBytesToShorts(audioData);
            AudioEncoder encoder = voiceServer.createOpusEncoder(false);
            Encryption encryption = voiceServer.getDefaultEncryption();

            try {
                byte[] encodedFrame = encoder.encode(samples);
                byte[] encryptedFrame = encryption.encrypt(encodedFrame);
                source.sendAudioFrame(encryptedFrame, encodedFrame.length, distance);
            } catch (CodecException | EncryptionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void playSoundToPlayer(PlasmoVoiceServer voiceServer, ServerDirectSource source, String audioFilePath) {
        CompletableFuture.runAsync(() -> {
            try {
                CompletableFuture<short[]> samples = loadAudioFile(audioFilePath);
                ArrayAudioFrameProvider frameProvider = new ArrayAudioFrameProvider(voiceServer, false);
                frameProvider.addSamples(samples.get());

                AudioSender audioSender = source.createAudioSender(frameProvider);
                audioSender.start();

                sources.put(source, audioSender);

                audioSender.onStop(() -> {
                    frameProvider.close();
                    source.remove();
                    sources.remove(source);
                });
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void playSoundOnLocation(PlasmoVoiceServer voiceServer, ServerStaticSource source, String audioFilePath, short distance) {
        CompletableFuture.runAsync(() -> {
            try {
                CompletableFuture<short[]> samples = loadAudioFile(audioFilePath);
                ArrayAudioFrameProvider frameProvider = new ArrayAudioFrameProvider(voiceServer, false);
                frameProvider.addSamples(samples.get());

                AudioSender audioSender = source.createAudioSender(frameProvider, distance);
                audioSender.start();

                sources.put(source, audioSender);

                audioSender.onStop(() -> {
                    frameProvider.close();
                    source.remove();
                    sources.remove(source);
                });
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void playSoundOnEntity(PlasmoVoiceServer voiceServer, ServerEntitySource source, String audioFilePath, short distance) {
        CompletableFuture.runAsync(() -> {
            try {
                CompletableFuture<short[]> samples = loadAudioFile(audioFilePath);
                ArrayAudioFrameProvider frameProvider = new ArrayAudioFrameProvider(voiceServer, false);
                frameProvider.addSamples(samples.get());

                AudioSender audioSender = source.createAudioSender(frameProvider, distance);
                audioSender.start();

                sources.put(source, audioSender);

                audioSender.onStop(() -> {
                    frameProvider.close();
                    source.remove();
                    sources.remove(source);
                });
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static short[] convertBytesToShorts(byte[] audioData) {
        int shortArrayLength = audioData.length / 2;
        short[] samples = new short[shortArrayLength];

        for (int i = 0; i < shortArrayLength; i++) {
            int byteIndex = i * 2;
            samples[i] = (short) ((audioData[byteIndex] & 0xFF) | (audioData[byteIndex + 1] << 8));
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
