package org.marayd.altenizen.plasmo;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.marayd.altenizen.Altenizen;
import org.marayd.altenizen.customevent.bukkit.PlayerEndsSpeaking;
import org.marayd.altenizen.customevent.bukkit.PlayerSpeaksEvent;
import org.vosk.Model;
import org.vosk.Recognizer;
import su.plo.voice.api.addon.AddonInitializer;
import su.plo.voice.api.addon.AddonLoaderScope;
import su.plo.voice.api.addon.InjectPlasmoVoice;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.audio.codec.AudioDecoder;
import su.plo.voice.api.audio.codec.AudioEncoder;
import su.plo.voice.api.audio.codec.CodecException;
import su.plo.voice.api.encryption.Encryption;
import su.plo.voice.api.encryption.EncryptionException;
import su.plo.voice.api.event.EventPriority;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.line.ServerSourceLine;
import su.plo.voice.api.server.audio.source.ServerStaticSource;
import su.plo.voice.api.server.event.audio.source.PlayerSpeakEndEvent;
import su.plo.voice.api.server.event.audio.source.PlayerSpeakEvent;
import su.plo.voice.server.player.BaseVoicePlayer;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.marayd.altenizen.Altenizen.instance;

@Addon(
        id = "pv-addon-altenizen",
        name = "Altenizen",
        version = "1.0.0",
        authors = {"maraydq"},
        scope = AddonLoaderScope.ANY
)
public final class PlasmoVoiceAddon implements AddonInitializer {

    @InjectPlasmoVoice
    private PlasmoVoiceServer voiceServer;

    private static final String MODEL_FOLDER = "plugins/Altenizen/models";
    private static Encryption encryption;


    public PlasmoVoiceServer getVoice() {
        return voiceServer;
    }
//    private ProximityServerActivationHelper proximityHelper;

    @Getter
    public static ServerSourceLine sourceLine;

    private static Model voskModel;
    @Override
    public void onAddonInitialize() {
        instance.getLogger().info("║ Initializing PlasmoAddon ║");
        try {
            File modelDir = new File(MODEL_FOLDER);
            if (!modelDir.exists()) {
                boolean isSuccess = modelDir.mkdirs();
                if (!isSuccess) instance.getLogger().warning("Couldn't create directory");
            }

            File modelPath = new File(modelDir, Objects.requireNonNull(instance.getConfig().getString("vosk.model-name")));
            if (!modelPath.exists()) {
                System.out.println("Модель не найдена, начинаем загрузку...");
                downloadAndExtractModel(instance.getConfig().getString("vosk.model-link"), modelDir);
            }

            voskModel = new Model(modelPath.getCanonicalPath());
        } catch (IOException e) {
            instance.getLogger().severe("Execption: " + Arrays.toString(e.getStackTrace()));
            instance.getLogger().severe("Execption: " + e.getMessage());
        }


        if (voiceServer == null) {
            instance.getLogger().info("║ Error: PlasmoVoiceServer is null. Initialization aborted. ║");
            return;
        }

        sourceLine = voiceServer.getSourceLineManager().createBuilder(
                Altenizen.PLASMO_VOICE_ADDON,
                "altenizen",
                "pv.activation.altenizen",
                "plasmovoice:textures/icons/speaker_priority.png",
                10
        ).build();

//        ServerActivation activation = voiceServer.getActivationManager().createBuilder(
//                Altenizen.denizenAddon,
//                "Altenizen",
//                "pv.activation.altenizen",
//                "plasmovoice:textures/icons/microphone_priority.png",
//                "pv.activation.altenizen",
//                10
//        ).build();
//        this.proximityHelper = new ProximityServerActivationHelper(voiceServer, activation, sourceLine);

        voiceServer.getEventBus().register(this, PlayerSpeakEvent.class, EventPriority.HIGHEST, this::onPlayerSpeak);
        voiceServer.getEventBus().register(this, PlayerSpeakEndEvent.class, EventPriority.HIGHEST, this::onPlayerSpeakEnd);


//        proximityHelper.registerListeners(this);
        encryption = voiceServer.getDefaultEncryption();
        instance.getLogger().info("║ Addon initialized successfully ║");
    }



    private final ConcurrentHashMap<String, AudioEncoder> encoders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AudioDecoder> decoders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ByteArrayOutputStream> playerAudioBuffer = new ConcurrentHashMap<>();

    public AudioEncoder getOrCreateEncoder(String playerId) {
        return encoders.computeIfAbsent(playerId, id -> voiceServer.createOpusEncoder(false));
    }

    public AudioDecoder getOrCreateDecoder(String playerId) {
        return decoders.computeIfAbsent(playerId, id -> voiceServer.createOpusDecoder(false));
    }

    public void releaseResources(String playerId) {
        Optional.ofNullable(encoders.remove(playerId)).ifPresent(AudioEncoder::close);
        Optional.ofNullable(decoders.remove(playerId)).ifPresent(AudioDecoder::close);
        Optional.ofNullable(playerAudioBuffer.remove(playerId)).ifPresent(buffer -> {
            try {
                buffer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        speakingBuffers.remove(playerId);
    }


    private static final long BUFFER_DURATION_NS = 300_000_000;
    private static final int MAX_AUDIO_BUFFER_SIZE = 4096 * 10;

    private static class AudioBuffer {
        private final ByteBuffer buffer = ByteBuffer.allocateDirect(MAX_AUDIO_BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        private long lastFlushTime = System.nanoTime();

        public synchronized void append(byte[] data) {
            if (buffer.remaining() < data.length) {
                flush();
            }
            buffer.put(data, 0, Math.min(data.length, buffer.remaining()));
        }

        public synchronized boolean shouldFlush() {
            return (System.nanoTime() - lastFlushTime) >= BUFFER_DURATION_NS || buffer.position() >= MAX_AUDIO_BUFFER_SIZE;
        }

        public synchronized byte[] flush() {
            int size = buffer.position();
            if (size == 0) return new byte[0];
            buffer.flip();
            byte[] data = new byte[size];
            buffer.get(data);
            buffer.clear();
            lastFlushTime = System.nanoTime();
            return data;
        }
    }

    private final ConcurrentHashMap<String, AudioBuffer> speakingBuffers = new ConcurrentHashMap<>();

    public void onPlayerSpeak(PlayerSpeakEvent event) {
        byte[] encryptedFrame = event.getPacket().getData();
        var player = (BaseVoicePlayer<?>) event.getPlayer();
        String playerId = player.getInstance().getName();

        AudioDecoder decoder = getOrCreateDecoder(playerId);

        try {
            byte[] decryptedFrame = encryption.decrypt(encryptedFrame);
            short[] audioFrame = decoder.decode(decryptedFrame);

            ByteBuffer byteBuffer = ByteBuffer.allocate(audioFrame.length * 2).order(ByteOrder.LITTLE_ENDIAN);
            for (short sample : audioFrame) {
                byteBuffer.putShort(sample);
            }
            byte[] audioBytes = byteBuffer.array();

            ByteArrayOutputStream audioStream = playerAudioBuffer.computeIfAbsent(playerId, id -> new ByteArrayOutputStream());
            try {
                audioStream.write(audioBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }

            AudioBuffer audioBuffer = speakingBuffers.computeIfAbsent(playerId, id -> new AudioBuffer());
            audioBuffer.append(audioBytes);
            if (audioBuffer.shouldFlush()) {

                byte[] combinedAudio = audioBuffer.flush();

                if (combinedAudio.length > 0) {
                    PlayerSpeaksEvent speaksEvent = new PlayerSpeaksEvent(player.getInstance().getInstance(), combinedAudio, event);
                    Bukkit.getScheduler().runTaskAsynchronously(instance, () ->
                            Bukkit.getPluginManager().callEvent(speaksEvent)
                    );
                }
            }

        } catch (EncryptionException | CodecException e) {
            e.printStackTrace();
        }
    }

    public void onPlayerSpeakEnd(PlayerSpeakEndEvent event) {
        var player = (BaseVoicePlayer<?>) event.getPlayer();
        String playerId = player.getInstance().getName();

        ByteArrayOutputStream audioStream = playerAudioBuffer.get(playerId);
        if (audioStream != null && audioStream.size() > 0) {
            byte[] audioBytes = audioStream.toByteArray();
            PlayerEndsSpeaking endEvent = new PlayerEndsSpeaking(
                    player.getInstance().getInstance(),
                    audioBytes,
                    event.getPacket().getActivationId()
            );
            Bukkit.getScheduler().runTaskAsynchronously(instance, () ->
                    Bukkit.getPluginManager().callEvent(endEvent)
            );
        }

        releaseResources(playerId);
    }



    public static void saveToWav(byte[] audioBytes, String dir) throws IOException {
        AudioFormat format = new AudioFormat(48000, 16, 1, true, false);

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(audioBytes);
        AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, format, audioBytes.length / format.getFrameSize());

        File wavFile = new File(dir);

        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, wavFile);
    }


    public static CompletableFuture<String> recognizeFromBytesAsync(byte[] audioBytes) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Model model = voskModel;

                InputStream audioStream = new ByteArrayInputStream(audioBytes);

                AudioFormat format = new AudioFormat(48000, 16, 1, true, false);
                AudioInputStream audioInputStream = new AudioInputStream(audioStream, format, audioBytes.length);

                Recognizer recognizer = new Recognizer(model, 48000);

                byte[] buffer = new byte[4000];
                StringBuilder result = new StringBuilder();

                while (audioInputStream.read(buffer) != -1) {
                    if (recognizer.acceptWaveForm(buffer, buffer.length)) {
                        result.append(recognizer.getResult());
                    }
                }

                return recognizer.getFinalResult();

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        });
    }

    private void downloadAndExtractModel(String modelUrl, File outputDir) throws IOException {
        URL url = new URL(modelUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        int fileSize = connection.getContentLength();


        File tempZip = new File(outputDir, "model.zip");
        try (InputStream inputStream = connection.getInputStream();
             FileOutputStream fileOutputStream = new FileOutputStream(tempZip)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            long downloaded = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
                downloaded += bytesRead;
                int progress = 0;
                if (fileSize > 0) {
                    progress = (int) (100 * downloaded / fileSize);
                }
                System.out.print("\rСкачивание: " + progress + "%");
            }
            System.out.println("\nСкачивание завершено.");
        }

        System.out.println("Распаковка модели...");
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(tempZip))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                File entryFile = new File(outputDir, entry.getName());

                if (!entryFile.toPath().normalize().startsWith(outputDir.toPath())) {
                    throw new IOException("Обнаружена попытка извлечения файла за пределы целевой директории: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    if (!entryFile.exists() && !entryFile.mkdirs()) {
                        throw new IOException("Не удалось создать директорию: " + entryFile.getAbsolutePath());
                    }
                } else {
                    File parentDir = entryFile.getParentFile();
                    if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                        throw new IOException("Не удалось создать родительскую директорию: " + parentDir.getAbsolutePath());
                    }

                    try (FileOutputStream fos = new FileOutputStream(entryFile)) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = zipInputStream.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zipInputStream.closeEntry();
            }
        }

        System.out.println("Распаковка завершена.");
        if (!tempZip.delete()) {
            System.out.println("Предупреждение: временный ZIP-файл не удалось удалить.");
        }
    }

    @Override
    public void onAddonShutdown() {
        System.out.println("║ Addon shut down ║");
    }
}