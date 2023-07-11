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
package net.raphimc.viabedrock.api;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;

import java.util.ArrayList;
import java.util.List;

public class BedrockProtocolVersion {

    public static final List<ProtocolVersion> PROTOCOLS = new ArrayList<>();
    public static final String LATEST_BEDROCK_VERSION = "1.20.10";

    public static final ProtocolVersion bedrockLatest = registerBedrock(1_000_594, "Bedrock " + LATEST_BEDROCK_VERSION);

    private static ProtocolVersion registerBedrock(final int version, final String name) {
        final ProtocolVersion protocolVersion = ProtocolVersion.register(version, name);
        PROTOCOLS.add(protocolVersion);
        return protocolVersion;
    }

}
