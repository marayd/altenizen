package com.marayd.denizenImplementation.PlasmoHook;

import com.marayd.denizenImplementation.event.BukkitPlayerSpeechEvent;
import org.bukkit.Bukkit;
import org.vosk.Model;
import org.vosk.Recognizer;
import su.plo.slib.api.server.position.ServerPos3d;
import su.plo.slib.api.server.world.McServerWorld;
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
import su.plo.voice.api.server.audio.capture.ProximityServerActivationHelper;
import su.plo.voice.api.server.audio.capture.ServerActivation;
import su.plo.voice.api.server.audio.line.ServerSourceLine;
import su.plo.voice.api.server.audio.source.ServerStaticSource;
import su.plo.voice.api.server.event.audio.source.PlayerSpeakEndEvent;
import su.plo.voice.api.server.event.audio.source.PlayerSpeakEvent;
import su.plo.voice.api.server.player.VoicePlayer;
import su.plo.voice.api.server.player.VoiceServerPlayer;
import com.marayd.denizenImplementation.DenizenImplementation;
import com.marayd.denizenImplementation.event.OnPlayerSpeakEvent;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.marayd.denizenImplementation.DenizenImplementation.denizenAddon;
import static com.marayd.denizenImplementation.DenizenImplementation.instance;

@Addon(
        id = "pv-addon-altenizen",
        name = "Altenizen",
        version = "1.0.0",
        authors = {"maraydq"},
        scope = AddonLoaderScope.ANY
)
public final class DenizenAddon implements AddonInitializer {

    @InjectPlasmoVoice
    private PlasmoVoiceServer voiceServer;
    private static final String MODEL_URL_RU = "https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip";
    private static final String MODEL_URL_EN = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip";
    private static final String MODEL_FOLDER = "plugins/Altenizen/models";
    private final Map<VoiceServerPlayer, ByteArrayOutputStream> recordings = new HashMap<>();
    private final Map<VoicePlayer, ByteArrayOutputStream> activeRecordings = new HashMap<>();
    private static ServerStaticSource source;
    private static Encryption encryption;

    public PlasmoVoiceServer getVoice() {
        return voiceServer;
    }
    private ProximityServerActivationHelper proximityHelper;

    public static ServerSourceLine sourceLine;
    private static Model voskModel;
    @Override
    public void onAddonInitialize() {
        instance.getLogger().info("║ Initializing PlasmoAddon ║");
        try {
            File modelDir = new File(MODEL_FOLDER);
            if (!modelDir.exists()) {
                modelDir.mkdirs();
            }

            File modelPath = new File(modelDir, instance.getConfig().getString("vosk.model-name"));
            if (!modelPath.exists()) {
                System.out.println("Модель не найдена, начинаем загрузку...");
                downloadAndExtractModel(instance.getConfig().getString("vosk.model-link"), modelDir);
            }

//            voskModel = new Model(modelPath.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }


        if (voiceServer == null) {
            instance.getLogger().info("║ Error: PlasmoVoiceServer is null. Initialization aborted. ║");
            return;
        }

        sourceLine = voiceServer.getSourceLineManager().createBuilder(
                DenizenImplementation.denizenAddon,
                "Altenizen",
                "pv.activation.altenizen",
                "plasmovoice:textures/icons/speaker_priority.png",
                10
        ).build();

        ServerActivation activation = voiceServer.getActivationManager().createBuilder(
                DenizenImplementation.denizenAddon,
                "Altenizen",
                "pv.activation.altenizen",
                "plasmovoice:textures/icons/microphone_priority.png",
                "pv.activation.altenizen",
                10
        ).build();
//        voiceServer.getEventBus().register(this, PlayerSpeakEvent.class, EventPriority.HIGHEST, this::onPlayerSpeak);
//        voiceServer.getEventBus().register(this, PlayerSpeakEndEvent.class, EventPriority.HIGHEST, this::onPlayerSpeakEnd);
        McServerWorld world = denizenAddon.getVoice().getMinecraftServer()
                .getWorlds()
                .stream()
                .filter(w -> w.getName().equals("world"))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("World not found."));


        ServerPos3d position = new ServerPos3d(world, 0, 0, 0);
        source = sourceLine.createStaticSource(position, false);

        this.proximityHelper = new ProximityServerActivationHelper(voiceServer, activation, sourceLine);
        proximityHelper.registerListeners(this);
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
        AudioEncoder encoder = encoders.remove(playerId);
        if (encoder != null) encoder.close();

        AudioDecoder decoder = decoders.remove(playerId);
        if (decoder != null) decoder.close();

        ByteArrayOutputStream buffer = playerAudioBuffer.remove(playerId);
        if (buffer != null) {
            try {
                buffer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void playSoundOnLocation(ServerStaticSource source, short[] audioData, short distance, String playerId) {
        AudioEncoder encoder = getOrCreateEncoder(playerId);

        try {
            byte[] encodedFrame = encoder.encode(audioData);
            byte[] encryptedFrame = encryption.encrypt(encodedFrame);
            source.sendAudioFrame(encryptedFrame, encodedFrame.length, distance);
        } catch (CodecException | EncryptionException e) {
            throw new RuntimeException(e);
        }
    }

    public void onPlayerSpeak(PlayerSpeakEvent event) {
        byte[] encryptedFrame = event.getPacket().getData();
        var player = (BaseVoicePlayer<?>) event.getPlayer();
        var playerId = player.getInstance().getName();

        AudioDecoder decoder = getOrCreateDecoder(playerId);
        Encryption encryption = voiceServer.getDefaultEncryption();

        try {
            byte[] decryptedFrame = encryption.decrypt(encryptedFrame);
            short[] audioFrame = decoder.decode(decryptedFrame);

            playerAudioBuffer.putIfAbsent(playerId, new ByteArrayOutputStream());

            ByteBuffer buffer = ByteBuffer.allocate(audioFrame.length * 2);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            for (short sample : audioFrame) {
                buffer.putShort(sample);
            }
            byte[] audioBytes = buffer.array();
            playSoundOnLocation(source, audioFrame, (short) 16, playerId);
            playerAudioBuffer.get(playerId).write(audioBytes);

//            OnPlayerSpeakEvent onPlayerSpeaks = new OnPlayerSpeakEvent(player.getInstance().getInstance(), audioBytes);
//            Bukkit.getScheduler().runTask(instance, () -> Bukkit.getPluginManager().callEvent(onPlayerSpeaks));
        } catch (EncryptionException | CodecException | IOException e) {
            e.printStackTrace();
        }
    }

    private void onPlayerSpeakEnd(PlayerSpeakEndEvent event) {
        var player = (BaseVoicePlayer<?>) event.getPlayer();
        var playerId = player.getInstance().getName();

        ByteArrayOutputStream audioData = playerAudioBuffer.get(playerId);

        if (audioData != null && audioData.size() > 0) {
            try {
                byte[] audioBytes = audioData.toByteArray();

//                recognizeFromBytesAsync(audioBytes).thenAccept(result -> {
//                    if (result != null) {
//                        BukkitPlayerSpeechEvent bukkitEvent = new BukkitPlayerSpeechEvent(player.getInstance().getInstance(), result, audioBytes);
//                        Bukkit.getScheduler().runTask(instance, () -> Bukkit.getPluginManager().callEvent(bukkitEvent));
//                    }
//                });
            } finally {
                releaseResources(playerId);
//                playerAudioBuffer.remove(playerId);
            }
        }
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
