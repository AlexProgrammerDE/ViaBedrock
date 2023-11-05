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
package net.raphimc.viabedrock.api.util;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.protocols.protocol1_20_2to1_20.packet.ClientboundPackets1_20_2;
import net.raphimc.viabedrock.protocol.BedrockProtocol;

public class PacketFactory {

    public static PacketWrapper systemChat(final UserConnection user, final JsonElement message) {
        final PacketWrapper systemChat = PacketWrapper.create(ClientboundPackets1_20_2.SYSTEM_CHAT, user);
        systemChat.write(Type.COMPONENT, message); // message
        systemChat.write(Type.BOOLEAN, false); // overlay
        return systemChat;
    }

    public static <T extends Throwable> void sendSystemChat(final UserConnection user, final JsonElement message) throws T {
        try {
            systemChat(user, message).send(BedrockProtocol.class);
        } catch (Throwable e) {
            throw (T) e;
        }
    }

}
