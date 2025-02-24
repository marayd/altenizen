package com.marayd.denizenImplementation;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.marayd.denizenImplementation.DenizenHook.ElemenTagProc;
import com.marayd.denizenImplementation.command.DeleteAudioCommand;
import com.marayd.denizenImplementation.event.*;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import su.plo.voice.api.server.PlasmoVoiceServer;
import com.marayd.denizenImplementation.DenizenHook.EntityTagProc;
import com.marayd.denizenImplementation.DenizenHook.PlasmoTagProc;
import com.marayd.denizenImplementation.DenizenHook.PlayerTagProc;
import com.marayd.denizenImplementation.PlasmoHook.DenizenAddon;
import com.marayd.denizenImplementation.command.DownloadPlasmoSound;
import com.marayd.denizenImplementation.command.PlasmoHookCommand;

public final class DenizenImplementation extends JavaPlugin {

    @Getter
    public static DenizenImplementation instance;

    public static DenizenAddon denizenAddon = new DenizenAddon();
    FileConfiguration config = getConfig();

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("║ ■=■=■=■=■=■=■=■=■=■=■=■=■=■=■=■=■=■=■=■ ║");
        getLogger().info("║ Altenizen Implementation ║");
        initConfig();
        showProgress("Initializing config", 0);
        long startTime = System.currentTimeMillis();
        Auth.AUTH_KEY = getConfig().getString("key");
        if (!Auth.authorizeServer()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        // Initialize config
        showProgress("Starting ProtocolLib", 20);
        if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
            ProtocolThinks.initParticlesEvent(protocolManager);
            ScriptEvent.registerScriptEvent(OnParticleSpawns.class);
        } else {
            getLogger().warning("║ ProtocolLib is not installed, some features will be disabled! ║");
        }

        // Check for Denizen plugin
        showProgress("Checking for Denizen plugin", 40);
        if (!Bukkit.getPluginManager().isPluginEnabled("Denizen")) {
            getLogger().severe("║ Denizen is not installed! ║");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Register Denizen hooks
        showProgress("Registering Denizen hooks", 60);
        registerDenizen();

        showProgress("Finalizing setup", 80);
        getLogger().info("║ Enabled success ║");

        long endTime = System.currentTimeMillis();
        double timeTaken = (endTime - startTime) / 1000.0;

        showProgress("Complete", 100);
        getLogger().info("║ Startup completed in " + timeTaken + " seconds ║");
        getLogger().info("║ ■=■=■=■=■=■=■=■=■=■=■=■=■=■=■=■=■=■=■=■ ║");
    }

    private void initConfig() {
        config.addDefault("key", "your-key-here");
        config.options().copyDefaults(true);
        saveConfig();

        config.addDefault("settings.path-to-download", "plugins/Altenizen/.saved-audio");
        config.addDefault("settings.default-path", "plugins/Altenizen/.saved-audio");
        config.addDefault("settings.max-size", 4096);
        saveConfig();
    }

    private void registerDenizen() {
        PlayerTagProc.start();
        EntityTagProc.start();
        ElemenTagProc.start();
        getLogger().info("║ Successfully started Denizen hook ║");

        if (Bukkit.getPluginManager().isPluginEnabled("PlasmoVoice")) {
            getLogger().info("║ Initializing Denizen Commands ║");
            DenizenCore.commandRegistry.registerCommand(PlasmoHookCommand.class);
            DenizenCore.commandRegistry.registerCommand(DownloadPlasmoSound.class);
            DenizenCore.commandRegistry.registerCommand(DeleteAudioCommand.class);
            getLogger().info("║ Initializing Denizen tags ║ ");
            new PlasmoTagProc();
            getLogger().info("║ Initializing Denizen events ║ ");
            ScriptEvent.registerScriptEvent(OnEntityMoves.class);
            ScriptEvent.registerScriptEvent(OnPlayerSpeaks.class);
            ScriptEvent.registerScriptEvent(PlayerSpeechEvent.class);
            getLogger().info("║ Hooking PlasmoVoice ║");
            PlasmoVoiceServer.getAddonsLoader().load(denizenAddon);
        }
    }

    private void showProgress(String message, int percentage) {
        StringBuilder progressBar = new StringBuilder("║ [");
        int progress = percentage / 10;
        for (int i = 0; i < 10; i++) {
            if (i < progress) {
                progressBar.append("#");
            } else {
                progressBar.append("-");
            }
        }
        progressBar.append("] ").append(percentage).append("% - ").append(message).append(" ║");
        getLogger().info(progressBar.toString());
    }

    @Override
    public void onDisable() {
        PlasmoVoiceServer.getAddonsLoader().unload(denizenAddon);
    }

}
