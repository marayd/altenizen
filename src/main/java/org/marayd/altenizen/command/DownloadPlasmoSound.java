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

package org.marayd.altenizen.command;

import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import org.marayd.altenizen.Altenizen;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class DownloadPlasmoSound extends AbstractCommand implements Holdable {
    public DownloadPlasmoSound() {
        setName("download_plasmo_sound");
        setSyntax("download_plasmo_sound [url:<[url]>] [name:<[name]>] ");
        setRequiredArguments(2, 2);
        isProcedural = true;
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) {
        for (Argument arg : scriptEntry) {
            if (arg.matchesPrefix("url") && !scriptEntry.hasObject("url")) {
                scriptEntry.addObject("url", arg.asElement());
            } else if (arg.matchesPrefix("name") && !scriptEntry.hasObject("name")) {
                scriptEntry.addObject("name", arg.asElement());
            } else {
                arg.reportUnhandled();
            }
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        String url = scriptEntry.getObject("url").toString();
        String name = scriptEntry.getObject("name").toString();

        // Асинхронная загрузка
        CompletableFuture.runAsync(() -> {
            try {
                downloadAudioFromInternet(url, name);
            } catch (Exception e) {
                e.printStackTrace();
                Altenizen.instance.getLogger().severe("Failed to download and process audio: " + e.getMessage());
            }
        }).thenRun(() -> {
            scriptEntry.setFinished(true); // Завершаем команду после обработки
        });
    }

    /**
     * Processes audio to 48kHz sample rate, mono channel, and signed 16-bit PCM format.
     *
     * @param inputFile  The input audio file.
     * @param outputFile The file to save the processed audio.
     * @throws IOException If an error occurs during file operations.
     */
    public static void processAudio(File inputFile, File outputFile) throws IOException {
        File intermediateFile = null;
        try (AudioInputStream inputStream = AudioSystem.getAudioInputStream(inputFile)) {
            AudioFormat targetFormat = new AudioFormat(
                    48000.0f,
                    16,
                    1,
                    true,
                    false
            );

            try (AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, inputStream)) {
                AudioSystem.write(convertedStream, AudioFileFormat.Type.WAVE, outputFile);
            } catch (IllegalArgumentException e) {
                throw new UnsupportedAudioFileException("Unsupported conversion to the target audio format.");
            }
        } catch (UnsupportedAudioFileException e) {
            // Convert unsupported audio file to WAV
            System.out.println("Attempting to convert unsupported audio file to WAV...");
            intermediateFile = new File(inputFile.getParent(), "intermediate.wav");

            // Retry processing with the WAV file
            processAudio(intermediateFile, outputFile);
        } finally {
            if (intermediateFile != null && intermediateFile.exists() && !intermediateFile.delete()) {
                System.err.println("Failed to delete intermediate file: " + intermediateFile.getAbsolutePath());
            }
        }
    }

    /**
     * Downloads an audio file from the internet, processes it, and saves it to the specified directory.
     *
     * @param url  The URL to the audio file.
     * @param name The name of the processed file (saved in plugins/Altenizen/.saved_audio).
     */
    public static void downloadAudioFromInternet(String url, String name) {
        String dir = Altenizen.instance.getConfig().getString("settings.path-to-download");
        if (dir == null) {
            Altenizen.instance.getLogger().severe("Check config! There is no 'settings.path-to-download' setting!");
            return;
        }
        File outputDir = new File(dir);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            System.err.println("Failed to create directory: " + outputDir.getAbsolutePath());
            return;
        }
        long maxFileSize = Altenizen.instance.getConfig().getInt("settings.max-size") * 1024L; // 5 MB in bytes

        File downloadedFile = new File(outputDir, "temp_" + name);
        File processedFile = new File(outputDir, name);
        try {
            URL fileUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) fileUrl.openConnection();
            connection.setRequestMethod("HEAD");  // Use HEAD request to get metadata only
            connection.connect();

            int contentLength = connection.getContentLength(); // Get the size of the file
            if (contentLength == -1) {
                System.err.println("Could not determine file size from URL: " + url);
                return;
            }
            try (BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
                 FileOutputStream fileOutputStream = new FileOutputStream(downloadedFile)) {
                byte[] dataBuffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }

                // Check the file size after download
                long fileSizeInBytes = downloadedFile.length();
                if (fileSizeInBytes > maxFileSize) {
                    System.err.println("Downloaded file exceeds the 5 MB limit. Deleting the file.");
                    if (downloadedFile.exists() && !downloadedFile.delete()) {
                        System.err.println("Failed to delete oversized file: " + downloadedFile.getAbsolutePath());
                    }
                    return; // Exit the method if the file size is too large
                }

                processAudio(downloadedFile, processedFile);

            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (downloadedFile.exists() && !downloadedFile.delete()) {
                    System.err.println("Failed to delete temporary file: " + downloadedFile.getAbsolutePath());
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

}
