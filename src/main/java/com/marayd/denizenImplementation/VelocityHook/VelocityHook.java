package com.marayd.denizenImplementation.VelocityHook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import com.marayd.denizenImplementation.DenizenImplementation;

import java.nio.charset.StandardCharsets;

import static org.bukkit.Bukkit.getServer;

public class VelocityHook implements PluginMessageListener {
    private static final String VELOCITY_CHANNEL_EXECUTE_COMMAND = "altenizen:backend_command_execute";
    private static final String VELOCITY_CHANNEL_SERVER_LIST_REQUEST = "altenizen:server_list_request";
    private static final String VELOCITY_CHANNEL_SERVER_LIST_RESPONSE = "altenizen:server_list_response";
    private static DenizenImplementation plugin;
    public void hookLoad(DenizenImplementation instance) {
        plugin = instance;
        getServer().getMessenger().registerOutgoingPluginChannel(instance, VELOCITY_CHANNEL_EXECUTE_COMMAND);
        getServer().getMessenger().registerOutgoingPluginChannel(instance, VELOCITY_CHANNEL_SERVER_LIST_REQUEST);
        getServer().getMessenger().registerIncomingPluginChannel(instance, VELOCITY_CHANNEL_SERVER_LIST_RESPONSE, this);
        // Request server list from Velocity when the plugin is enabled
        requestServerList();
    }
    /**
     * Requests the list of servers from Velocity.
     */
    public static void requestServerList() {
        // Send the request through the server list request channel
        Bukkit.getServer().sendPluginMessage(plugin, VELOCITY_CHANNEL_SERVER_LIST_REQUEST, new byte[0]);
        plugin.getLogger().info("Requested server list from Velocity.");
    }

    /**
     * Sends a command to be executed on the proxy server.
     *
     * @param command The command to execute on the proxy.
     */
    public static void executeProxyCommand(String command) {
        byte[] commandData = command.getBytes(StandardCharsets.UTF_8);
        plugin.getServer().sendPluginMessage(plugin, VELOCITY_CHANNEL_EXECUTE_COMMAND, commandData);
        plugin.getLogger().info("Sent command to proxy: " + command);
    }

    /**
     * Sends a command to be executed on a specified backend server via the proxy.
     *
     * @param serverName The target server name.
     * @param command The command to execute on the backend server.
     */
    public static void executeBackendCommand(String serverName, String command) {
        String message = serverName + "," + command;
        byte[] commandData = message.getBytes(StandardCharsets.UTF_8);
        Bukkit.getServer().sendPluginMessage(plugin, VELOCITY_CHANNEL_EXECUTE_COMMAND, commandData);
        plugin.getLogger().info("Sent command to backend server [" + serverName + "]: " + command);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (channel.equals(VELOCITY_CHANNEL_SERVER_LIST_RESPONSE)) {
            // Handle server list response from Velocity
            String serverList = new String(message, StandardCharsets.UTF_8);
            plugin.getLogger().info("Received server list from Velocity: " + serverList);
        }
    }

}
