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

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.commands.core.AdjustCommand;
import com.denizenscript.denizencore.tags.PseudoObjectTagBase;
import com.denizenscript.denizencore.tags.TagManager;
import org.marayd.altenizen.Altenizen;
import org.marayd.altenizen.plasmo.PlasmoVoiceAddon;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class PlasmoTagProc extends PseudoObjectTagBase<PlasmoTagProc> {
    private static final Altenizen plugin = Altenizen.instance;
//    private static final DenizenAddon denizenAddon = new DenizenAddon();

    private static List<String> getFilesFromDir(String dir) {
        return Stream.of(Objects.requireNonNull(new File(dir).listFiles()))
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public void register() {
        tagProcessor.registerTag(ListTag.class, "list_audio", (attribute, object) -> new ListTag(getFilesFromDir(Altenizen.instance.getConfig().getString("settings.path-to-download"))));
        tagProcessor.registerTag(ListTag.class, "active_sources", (attribute, object) ->
                new ListTag(PlasmoVoiceAddon.sourceLine.getSources()
                        .stream()
                        .map(source -> source.getId().toString())
                        .collect(Collectors.toList())
                )
        );
        tagProcessor.registerTag(ElementTag.class, "audio", (attribute, object) -> {
            attribute.fulfill(1);
            if (attribute.startsWith("length")) {
                String stc = attribute.getParam();
                attribute.fulfill(1);
                if (stc.endsWith(".wav")) {
                    File audioFile = new File(stc);
                    if (audioFile.exists()) {
                        try {
                            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
                            AudioFormat format = audioInputStream.getFormat();
                            long frames = audioInputStream.getFrameLength();
                            double durationInSeconds = (frames + 0.0) / format.getFrameRate();
                            return new ElementTag(durationInSeconds);
                        } catch (UnsupportedAudioFileException | IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            return null;
        });
    }

    public static PlasmoTagProc instance;

    public PlasmoTagProc() {
        instance = this;
        TagManager.registerStaticTagBaseHandler(PlasmoTagProc.class, "plasmo", (t) -> instance);
        AdjustCommand.specialAdjustables.put("system", mechanism -> tagProcessor.processMechanism(instance, mechanism));
    }
}