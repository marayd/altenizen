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

package org.marayd.altenizen;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.ScriptEvent;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.marayd.altenizen.command.*;
import org.marayd.altenizen.processors.EntityTagProc;
import org.marayd.altenizen.processors.PlasmoTagProc;
import org.marayd.altenizen.customevent.denizen.PlayerEndsSpeakingDenizen;
import org.marayd.altenizen.customevent.denizen.PlayerSpeaksEventDenizen;
import org.marayd.altenizen.plasmo.PlasmoVoiceAddon;
import su.plo.voice.api.server.PlasmoVoiceServer;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.HttpURLConnection;
import java.net.URL;

public final class Altenizen extends JavaPlugin {

    @Getter
    public static Altenizen instance;

    @Getter
    public static final PlasmoVoiceAddon PLASMO_VOICE_ADDON = new PlasmoVoiceAddon();

    private void requireLicenseAcceptance() {
        String licenseUrl = "https://raw.githubusercontent.com/marayd/altenizen/refs/heads/main/License.md";
        try {
            URL url = new URL(licenseUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("License fetch failed with response code: " + responseCode);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            getLogger().info("====== License ======");
            while ((line = reader.readLine()) != null) {
                getLogger().info(line);
            }
            getLogger().info("====== End of License ======");
            reader.close();

        } catch (Exception e) {
            getLogger().severe("Unable to load license from remote server.");
            getLogger().severe("Error: " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }




    @Override
    public void onEnable() {
        instance = this;
        requireLicenseAcceptance();

        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        long uptimeSec = runtimeMXBean.getUptime() / 1000;

        if (uptimeSec > 120)
            for (int i = 0; i < 5; i++)
                getLogger().warning("DON'T USE PLUGMAN OR /RELOAD. IT WILL BREAK ENTIRE PLUGIN.");

        getLogger().info("║ ■=■=■=■=■=■=■=■=■=■=■=■=■=■=■=■=■=■=■=■");
        getLogger().info("║ Altenizen");

        long startTime = System.currentTimeMillis();

        showProgress("Initializing config", 0);
        initConfig();

        showProgress("Checking for Denizen plugin", 40);
        if (isPluginAvailable("Denizen")) {
            getLogger().severe("║ Denizen is not installed! ║");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        showProgress("Registering Denizen hooks", 60);
        registerDenizen();

        showProgress("Finalizing setup", 80);

        double timeTaken = (System.currentTimeMillis() - startTime) / 1000.0;
        showProgress("Complete", 100);
        getLogger().info("║ Startup completed in " + timeTaken + " seconds");
        getLogger().info("╔══════════════════════════════════════════════════════╗");
        getLogger().info("║                   ✨ Created by MaraydDev ✨         ║");
        getLogger().info("║            🌐 Visit: https://mryd.org/               ║");
        getLogger().info("║             Thank you for using this plugin!         ║");
        getLogger().info("╚══════════════════════════════════════════════════════╝");
        getLogger().info("║ ■=■=■=■=■=■=■=■=■=■=■=■=■=■=■=■=■=■=■=■");
    }

    @Override
    public void onDisable() {
        PlasmoVoiceServer.getAddonsLoader().unload(PLASMO_VOICE_ADDON);
    }

    private void initConfig() {
        FileConfiguration config = getConfig();

        config.addDefault("key", "your-key-here");
        config.addDefault("settings.path-to-download", "plugins/Altenizen/.saved-audio");
        config.addDefault("settings.default-path", "plugins/Altenizen/.saved-audio");
        config.addDefault("settings.max-size", 4096);

        config.options().copyDefaults(true);
        saveConfig();
    }

    private void registerDenizen() {
        EntityTagProc.start();
        getLogger().info("║ Successfully started Denizen hook ║");

        if (isPluginAvailable("PlasmoVoice")) return;

        getLogger().info("║ Initializing Denizen Commands ║");
        DenizenCore.commandRegistry.registerCommand(PlasmoHookCommand.class);
        DenizenCore.commandRegistry.registerCommand(SourceCommand.class);
        DenizenCore.commandRegistry.registerCommand(DownloadPlasmoSound.class);
        DenizenCore.commandRegistry.registerCommand(DeleteAudioCommand.class);
        DenizenCore.commandRegistry.registerCommand(ActivationCommand.class);
        DenizenCore.commandRegistry.registerCommand(VoskCommand.class);

        getLogger().info("║ Initializing Denizen Tags ║");
        new PlasmoTagProc();

        getLogger().info("║ Initializing Denizen Events ║");
        ScriptEvent.registerScriptEvent(PlayerSpeaksEventDenizen.class);
        ScriptEvent.registerScriptEvent(PlayerEndsSpeakingDenizen.class);

        getLogger().info("║ Hooking PlasmoVoice ║");
        PlasmoVoiceServer.getAddonsLoader().load(PLASMO_VOICE_ADDON);
    }

    private boolean isPluginAvailable(String pluginName) {
        return !Bukkit.getPluginManager().isPluginEnabled(pluginName);
    }

    private void showProgress(String message, int percentage) {
        StringBuilder progressBar = new StringBuilder("║ [");
        int progressUnits = percentage / 10;
        for (int i = 0; i < 10; i++) {
            progressBar.append(i < progressUnits ? "#" : "-");
        }
        progressBar.append("] ").append(percentage).append("% - ").append(message).append(" ║");
        getLogger().info(progressBar.toString());
    }
}
