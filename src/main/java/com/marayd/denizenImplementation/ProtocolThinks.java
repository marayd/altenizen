package com.marayd.denizenImplementation;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.marayd.denizenImplementation.event.OnParticleSpawnsEvent;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.List;

public class ProtocolThinks {
    public static void initParticlesEvent(ProtocolManager protocolManager) {
        protocolManager.addPacketListener(new PacketAdapter(DenizenImplementation.instance, PacketType.Play.Server.WORLD_PARTICLES) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                double x = (double) packet.getModifier().read(0);
                double y = (double) packet.getModifier().read(1);
                double z = (double) packet.getModifier().read(2);
                Player player = event.getPlayer();
                Particle type = packet.getNewParticles().read(0).getParticle();
                Bukkit.getPluginManager().callEvent(new OnParticleSpawnsEvent(type, x, y, z, player));
            }
        });

        protocolManager.addPacketListener(new PacketAdapter(DenizenImplementation.instance, PacketType.Play.Server.WORLD_EVENT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                StructureModifier<Object> modifier = packet.getModifier();
                List<Object> values = modifier.getValues();

            }
        });


    }
}
