package com.marayd.denizenImplementation.DenizenHook;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.commands.core.AdjustCommand;
import com.denizenscript.denizencore.tags.PseudoObjectTagBase;
import com.denizenscript.denizencore.tags.TagManager;
import com.marayd.denizenImplementation.DenizenImplementation;
import com.marayd.denizenImplementation.PlasmoHook.DenizenAddon;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlasmoTagProc extends PseudoObjectTagBase<PlasmoTagProc> {
    private static final DenizenImplementation plugin = DenizenImplementation.instance;
//    private static final DenizenAddon denizenAddon = new DenizenAddon();

    private static List<String> getFilesFromDir(String dir) {
        return Stream.of(Objects.requireNonNull(new File(dir).listFiles()))
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public void register() {
        tagProcessor.registerTag(ListTag.class, "list_audio", (attribute, object) -> new ListTag(getFilesFromDir(DenizenImplementation.instance.getConfig().getString("settings.path-to-download"))));
        tagProcessor.registerTag(ListTag.class, "active_sources", (attribute, object) ->
                new ListTag(DenizenAddon.sourceLine.getSources()
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