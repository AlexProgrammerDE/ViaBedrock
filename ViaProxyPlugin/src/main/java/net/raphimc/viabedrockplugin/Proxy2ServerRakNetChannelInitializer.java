/*
 * This file is part of ViaBedrock - https://github.com/RaphiMC/ViaBedrock
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.viabedrockplugin;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.connection.UserConnectionImpl;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.connection.MinecraftChannelInitializer;
import net.raphimc.netminecraft.packet.registry.PacketRegistryUtil;
import net.raphimc.viabedrock.netty.BatchLengthCodec;
import net.raphimc.viabedrock.netty.PacketEncapsulationCodec;
import net.raphimc.viabedrock.protocol.BedrockBaseProtocol;
import net.raphimc.viaprotocolhack.netty.ViaEncodeHandler;
import net.raphimc.viaprotocolhack.netty.ViaPipeline;
import net.raphimc.viaproxy.protocolhack.impl.ViaProxyViaDecodeHandler;
import net.raphimc.viaproxy.proxy.ProxyConnection;
import network.ycc.raknet.client.channel.RakNetClientChannel;

import java.util.function.Supplier;

public class Proxy2ServerRakNetChannelInitializer extends MinecraftChannelInitializer {

    public Proxy2ServerRakNetChannelInitializer(final Supplier<ChannelHandler> handlerSupplier) {
        super(handlerSupplier);
    }

    @Override
    protected void initChannel(Channel channel) {
        channel.attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).set(-1);

        final RakNetClientChannel rakChannel = (RakNetClientChannel) channel;
        rakChannel.config().setprotocolVersions(new int[]{11});

        final UserConnection user = new UserConnectionImpl(channel, true);
        new ProtocolPipelineImpl(user);
        ProxyConnection.fromChannel(channel).setUserConnection(user);
        user.getProtocolInfo().getPipeline().add(BedrockBaseProtocol.INSTANCE);

        channel.pipeline().addLast("frame_encapsulation", new FrameDataEncapsulationCodec());
        // Encryption
        // Compression
        channel.pipeline().addLast(MCPipeline.SIZER_HANDLER_NAME, new BatchLengthCodec());
        channel.pipeline().addLast("packet_encapsulation", new PacketEncapsulationCodec());
        channel.pipeline().addLast(MCPipeline.PACKET_CODEC_HANDLER_NAME, MCPipeline.PACKET_CODEC_HANDLER.get());
        channel.pipeline().addLast(MCPipeline.HANDLER_HANDLER_NAME, this.handlerSupplier.get());

        channel.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(PacketRegistryUtil.getHandshakeRegistry(true));
        channel.pipeline().addBefore(MCPipeline.PACKET_CODEC_HANDLER_NAME, ViaPipeline.HANDLER_ENCODER_NAME, new ViaEncodeHandler(user));
        channel.pipeline().addBefore(MCPipeline.PACKET_CODEC_HANDLER_NAME, ViaPipeline.HANDLER_DECODER_NAME, new ViaProxyViaDecodeHandler(user));
    }

}
