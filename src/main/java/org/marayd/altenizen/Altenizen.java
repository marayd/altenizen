package org.marayd.altenizen;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.ScriptEvent;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.marayd.altenizen.processors.EntityTagProc;
import org.marayd.altenizen.processors.PlasmoTagProc;
import org.marayd.altenizen.command.DeleteAudioCommand;
import org.marayd.altenizen.command.DownloadPlasmoSound;
import org.marayd.altenizen.command.PlasmoHookCommand;
import org.marayd.altenizen.customevent.denizen.PlayerEndsSpeakingDenizen;
import org.marayd.altenizen.customevent.denizen.PlayerSpeaksEventDenizen;
import org.marayd.altenizen.plasmo.DenizenAddon;
import su.plo.voice.api.server.PlasmoVoiceServer;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

public final class Altenizen extends JavaPlugin {

    @Getter
    public static Altenizen instance;

    @Getter
    public static final DenizenAddon denizenAddon = new DenizenAddon();

    @Override
    public void onEnable() {
        instance = this;
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        long uptimeSec = runtimeMXBean.getUptime() / 1000;

        if (uptimeSec > 120) {
            for (int i = 0; i < 5; i++) getLogger().warning("DON'T USE PLUGMAN OR /RELOAD. IT WILL BREAK ENTIRE PLUGIN.");
        }

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
        PlasmoVoiceServer.getAddonsLoader().unload(denizenAddon);
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
        DenizenCore.commandRegistry.registerCommand(DownloadPlasmoSound.class);
        DenizenCore.commandRegistry.registerCommand(DeleteAudioCommand.class);

        getLogger().info("║ Initializing Denizen Tags ║");
        new PlasmoTagProc();

        getLogger().info("║ Initializing Denizen Events ║");
        ScriptEvent.registerScriptEvent(PlayerSpeaksEventDenizen.class);
        ScriptEvent.registerScriptEvent(PlayerEndsSpeakingDenizen.class);

        getLogger().info("║ Hooking PlasmoVoice ║");
        PlasmoVoiceServer.getAddonsLoader().load(denizenAddon);
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
