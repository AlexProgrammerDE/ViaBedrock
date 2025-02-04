/*
 * This file is part of ViaBedrock - https://github.com/RaphiMC/ViaBedrock
 * Copyright (C) 2023-2024 RK_01/RaphiMC and contributors
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
package net.raphimc.viabedrock.protocol.packets;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ClientboundPackets1_20_3;
import com.viaversion.viaversion.util.Pair;
import net.lenni0451.mcstructs_bedrock.text.components.RootBedrockComponent;
import net.lenni0451.mcstructs_bedrock.text.components.TranslationBedrockComponent;
import net.lenni0451.mcstructs_bedrock.text.serializer.BedrockComponentSerializer;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.model.scoreboard.ScoreboardEntry;
import net.raphimc.viabedrock.api.model.scoreboard.ScoreboardObjective;
import net.raphimc.viabedrock.api.util.BitSets;
import net.raphimc.viabedrock.api.util.StringUtil;
import net.raphimc.viabedrock.api.util.TextUtil;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.ClientboundBedrockPackets;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.TitleTypes;
import net.raphimc.viabedrock.protocol.model.SkinData;
import net.raphimc.viabedrock.protocol.providers.SkinProvider;
import net.raphimc.viabedrock.protocol.storage.PlayerListStorage;
import net.raphimc.viabedrock.protocol.storage.ResourcePacksStorage;
import net.raphimc.viabedrock.protocol.storage.ScoreboardTracker;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;

public class HudPackets {

    public static void register(final BedrockProtocol protocol) {
        protocol.registerClientbound(ClientboundBedrockPackets.PLAYER_LIST, ClientboundPackets1_20_3.PLAYER_INFO_UPDATE, wrapper -> {
            final PlayerListStorage playerListStorage = wrapper.user().get(PlayerListStorage.class);
            final ScoreboardTracker scoreboardTracker = wrapper.user().get(ScoreboardTracker.class);

            final short action = wrapper.read(Type.UNSIGNED_BYTE); // action
            if (action == 0) { // ADD
                final int length = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // length
                final UUID[] uuids = new UUID[length];
                final long[] playerListIds = new long[length];
                final String[] names = new String[length];
                wrapper.write(Type.PROFILE_ACTIONS_ENUM, BitSets.create(6, 0, 3, 5)); // actions | ADD_PLAYER, UPDATE_LISTED, UPDATE_DISPLAY_NAME
                wrapper.write(Type.VAR_INT, length); // length
                for (int i = 0; i < length; i++) {
                    uuids[i] = wrapper.read(BedrockTypes.UUID); // uuid
                    wrapper.write(Type.UUID, uuids[i]); // uuid
                    playerListIds[i] = wrapper.read(BedrockTypes.VAR_LONG); // player list id
                    names[i] = wrapper.read(BedrockTypes.STRING); // username
                    wrapper.write(Type.STRING, StringUtil.encodeUUID(uuids[i])); // username
                    wrapper.write(Type.VAR_INT, 5); // property count
                    wrapper.write(Type.STRING, "xuid"); // property name
                    wrapper.write(Type.STRING, wrapper.read(BedrockTypes.STRING)); // xuid
                    wrapper.write(Type.OPTIONAL_STRING, null); // signature
                    wrapper.write(Type.STRING, "platform_chat_id"); // property name
                    wrapper.write(Type.STRING, wrapper.read(BedrockTypes.STRING)); // platform chat id
                    wrapper.write(Type.OPTIONAL_STRING, null); // signature
                    wrapper.write(Type.STRING, "device_os"); // property name
                    wrapper.write(Type.STRING, wrapper.read(BedrockTypes.INT_LE).toString()); // device os
                    wrapper.write(Type.OPTIONAL_STRING, null); // signature
                    final SkinData skin = wrapper.read(BedrockTypes.SKIN); // skin
                    wrapper.write(Type.STRING, "is_teacher"); // property name
                    wrapper.write(Type.STRING, wrapper.read(Type.BOOLEAN).toString()); // is teacher
                    wrapper.write(Type.OPTIONAL_STRING, null); // signature
                    wrapper.write(Type.STRING, "is_host"); // property name
                    wrapper.write(Type.STRING, wrapper.read(Type.BOOLEAN).toString()); // is host
                    wrapper.write(Type.OPTIONAL_STRING, null); // signature

                    wrapper.write(Type.BOOLEAN, true); // listed
                    wrapper.write(Type.OPTIONAL_TAG, TextUtil.stringToNbt(names[i])); // display name

                    Via.getManager().getProviders().get(SkinProvider.class).setSkin(wrapper.user(), uuids[i], skin);
                }
                for (int i = 0; i < length; i++) {
                    wrapper.read(Type.BOOLEAN); // trusted skin
                }

                final List<UUID> toRemoveUUIDs = new ArrayList<>();
                final List<String> toRemoveNames = new ArrayList<>();
                for (int i = 0; i < uuids.length; i++) {
                    final Pair<Long, String> entry = playerListStorage.addPlayer(uuids[i], playerListIds[i], names[i]);
                    if (entry != null) {
                        toRemoveUUIDs.add(uuids[i]);
                        toRemoveNames.add(entry.value());
                    }

                    final Pair<ScoreboardObjective, ScoreboardEntry> scoreboardEntry = scoreboardTracker.getEntryForPlayer(playerListIds[i]);
                    if (scoreboardEntry != null) {
                        scoreboardEntry.key().updateEntry(wrapper.user(), scoreboardEntry.value());
                    }
                }

                if (!toRemoveUUIDs.isEmpty()) {
                    // Remove duplicate players from the player list first because Mojang client overwrites entries if they are added twice
                    final PacketWrapper playerInfoRemove = PacketWrapper.create(ClientboundPackets1_20_3.PLAYER_INFO_REMOVE, wrapper.user());
                    playerInfoRemove.write(Type.UUID_ARRAY, toRemoveUUIDs.toArray(new UUID[0])); // uuids
                    playerInfoRemove.send(BedrockProtocol.class);

                    final PacketWrapper customChatCompletions = PacketWrapper.create(ClientboundPackets1_20_3.CUSTOM_CHAT_COMPLETIONS, wrapper.user());
                    customChatCompletions.write(Type.VAR_INT, 1); // action | 1 = REMOVE
                    customChatCompletions.write(Type.STRING_ARRAY, toRemoveNames.toArray(new String[0])); // entries
                    customChatCompletions.send(BedrockProtocol.class);
                }

                final PacketWrapper customChatCompletions = PacketWrapper.create(ClientboundPackets1_20_3.CUSTOM_CHAT_COMPLETIONS, wrapper.user());
                customChatCompletions.write(Type.VAR_INT, 0); // action | 0 = ADD
                customChatCompletions.write(Type.STRING_ARRAY, names); // entries
                customChatCompletions.send(BedrockProtocol.class);
            } else if (action == 1) { // REMOVE
                wrapper.setPacketType(ClientboundPackets1_20_3.PLAYER_INFO_REMOVE);
                final UUID[] uuids = wrapper.read(BedrockTypes.UUID_ARRAY); // uuids
                wrapper.write(Type.UUID_ARRAY, uuids); // uuids

                final List<String> names = new ArrayList<>();
                for (UUID uuid : uuids) {
                    final Pair<Long, String> entry = playerListStorage.removePlayer(uuid);
                    if (entry != null) {
                        names.add(entry.value());
                        final Pair<ScoreboardObjective, ScoreboardEntry> scoreboardEntry = scoreboardTracker.getEntryForPlayer(entry.key());
                        if (scoreboardEntry != null) {
                            scoreboardEntry.key().updateEntry(wrapper.user(), scoreboardEntry.value());
                        }
                    }
                }

                final PacketWrapper customChatCompletions = PacketWrapper.create(ClientboundPackets1_20_3.CUSTOM_CHAT_COMPLETIONS, wrapper.user());
                customChatCompletions.write(Type.VAR_INT, 1); // action | 1 = REMOVE
                customChatCompletions.write(Type.STRING_ARRAY, names.toArray(new String[0])); // entries
                customChatCompletions.send(BedrockProtocol.class);
            } else {
                BedrockProtocol.kickForIllegalState(wrapper.user(), "Unsupported player list action: " + action);
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.SET_TITLE, null, wrapper -> {
            final int type = wrapper.read(BedrockTypes.VAR_INT); // type
            String text = wrapper.read(BedrockTypes.STRING); // text
            final int fadeInTicks = wrapper.read(BedrockTypes.VAR_INT); // fade in ticks
            final int stayTicks = wrapper.read(BedrockTypes.VAR_INT); // stay ticks
            final int fadeOutTicks = wrapper.read(BedrockTypes.VAR_INT); // fade out ticks
            wrapper.read(BedrockTypes.STRING); // xuid
            wrapper.read(BedrockTypes.STRING); // platform chat id

            final Function<String, String> translator = wrapper.user().get(ResourcePacksStorage.class).getTranslationLookup();
            final String originalText = text;
            try {
                if (type >= TitleTypes.TITLE_JSON && type <= TitleTypes.ACTIONBAR_JSON) {
                    final RootBedrockComponent rootComponent = BedrockComponentSerializer.deserialize(text);
                    rootComponent.forEach(c -> {
                        if (c instanceof TranslationBedrockComponent) ((TranslationBedrockComponent) c).setTranslator(translator);
                    });
                    text = rootComponent.asString();
                }

                switch (type) {
                    case TitleTypes.CLEAR:
                    case TitleTypes.RESET:
                        wrapper.setPacketType(ClientboundPackets1_20_3.CLEAR_TITLES);
                        wrapper.write(Type.BOOLEAN, type == TitleTypes.RESET); // reset
                        break;
                    case TitleTypes.TITLE:
                    case TitleTypes.TITLE_JSON:
                        wrapper.setPacketType(ClientboundPackets1_20_3.TITLE_TEXT);
                        wrapper.write(Type.TAG, TextUtil.stringToNbt(text)); // text
                        break;
                    case TitleTypes.SUBTITLE:
                    case TitleTypes.SUBTITLE_JSON:
                        wrapper.setPacketType(ClientboundPackets1_20_3.TITLE_SUBTITLE);
                        wrapper.write(Type.TAG, TextUtil.stringToNbt(text)); // text
                        break;
                    case TitleTypes.ACTIONBAR:
                    case TitleTypes.ACTIONBAR_JSON:
                        wrapper.setPacketType(ClientboundPackets1_20_3.ACTIONBAR);
                        wrapper.write(Type.TAG, TextUtil.stringToNbt(text)); // text
                        break;
                    case TitleTypes.TIMES:
                        wrapper.setPacketType(ClientboundPackets1_20_3.TITLE_TIMES);
                        wrapper.write(Type.INT, fadeInTicks); // fade in ticks
                        wrapper.write(Type.INT, stayTicks); // stay ticks
                        wrapper.write(Type.INT, fadeOutTicks); // fade out ticks
                        break;
                    default: // Mojang client silently ignores unknown actions
                        ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown title type: " + type);
                        wrapper.cancel();
                }
            } catch (Throwable e) { // Mojang client silently ignores errors
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Error while translating '" + originalText + "'", e);
                wrapper.cancel();
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.SET_DISPLAY_OBJECTIVE, ClientboundPackets1_20_3.DISPLAY_SCOREBOARD, wrapper -> {
            final ScoreboardTracker scoreboardTracker = wrapper.user().get(ScoreboardTracker.class);

            final String displaySlot = wrapper.read(BedrockTypes.STRING); // display slot
            final String objectiveName = wrapper.read(BedrockTypes.STRING); // objective name
            final String displayName = wrapper.read(BedrockTypes.STRING); // display name
            wrapper.read(BedrockTypes.STRING); // criteria
            final boolean ascending = wrapper.read(BedrockTypes.VAR_INT) == 0; // sort order | Any invalid value is treated as no sorting, but Java Edition doesn't support that

            switch (displaySlot) {
                case "sidebar":
                    wrapper.write(Type.VAR_INT, 1); // position
                    break;
                case "belowname":
                    wrapper.write(Type.VAR_INT, 2); // position
                    break;
                case "list":
                    wrapper.write(Type.VAR_INT, 0); // position
                    break;
                default: // Mojang client silently ignores unknown actions
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown scoreboard display slot: " + displaySlot);
                    wrapper.cancel();
                    return;
            }
            wrapper.write(Type.STRING, objectiveName); // objective name

            if (objectiveName.isEmpty()) return;

            if (!scoreboardTracker.hasObjective(objectiveName)) {
                scoreboardTracker.addObjective(objectiveName, new ScoreboardObjective(objectiveName, ascending));

                final PacketWrapper scoreboardObjective = PacketWrapper.create(ClientboundPackets1_20_3.SCOREBOARD_OBJECTIVE, wrapper.user());
                scoreboardObjective.write(Type.STRING, objectiveName); // objective name
                scoreboardObjective.write(Type.BYTE, (byte) 0); // mode | 0 = CREATE
                scoreboardObjective.write(Type.TAG, TextUtil.stringToNbt(wrapper.user().get(ResourcePacksStorage.class).translate(displayName))); // display name
                scoreboardObjective.write(Type.VAR_INT, 0); // display mode | 0 = INTEGER
                scoreboardObjective.write(Type.BOOLEAN, false); // has number format
                scoreboardObjective.send(BedrockProtocol.class);
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.SET_SCORE, null, wrapper -> {
            wrapper.cancel();
            final ScoreboardTracker scoreboardTracker = wrapper.user().get(ScoreboardTracker.class);

            final int action = wrapper.read(Type.UNSIGNED_BYTE); // action
            final int count = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // count
            for (int i = 0; i < count; i++) {
                final long scoreboardId = wrapper.read(BedrockTypes.VAR_LONG); // scoreboard id
                final String objectiveName = wrapper.read(BedrockTypes.STRING); // objective name
                final int score = wrapper.read(BedrockTypes.INT_LE); // score

                final ScoreboardEntry entry;
                if (action == 0) { // SET
                    final short type = wrapper.read(Type.UNSIGNED_BYTE); // type
                    Long entityId = null;
                    String fakePlayerName = null;
                    if (type == 1 || type == 2) { // PLAYER or ENTITY
                        entityId = wrapper.read(BedrockTypes.VAR_LONG); // entity id
                    } else if (type == 3) { // FAKE_PLAYER
                        fakePlayerName = wrapper.read(BedrockTypes.STRING); // fake player name
                    }
                    entry = new ScoreboardEntry(score, type == 1, entityId, fakePlayerName);
                } else if (action == 1) { // REMOVE
                    entry = null;
                } else { // Mojang client silently ignores unknown actions
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown scoreboard action: " + action);
                    continue;
                }

                final ScoreboardObjective objective = scoreboardTracker.getObjective(objectiveName);
                final Pair<ScoreboardObjective, ScoreboardEntry> existingEntry = scoreboardTracker.getEntry(scoreboardId);
                if (existingEntry != null) {
                    existingEntry.key().removeEntry(wrapper.user(), scoreboardId);
                    if (entry != null && objective != null) {
                        existingEntry.value().setScore(entry.score());
                        objective.addEntry(wrapper.user(), scoreboardId, existingEntry.value());
                    }
                } else if (entry != null && objective != null) {
                    final ScoreboardEntry sameTargetEntry = objective.getEntryWithSameTarget(entry);
                    if (sameTargetEntry != null) {
                        sameTargetEntry.setScore(entry.score());
                        objective.updateEntry0(wrapper.user(), sameTargetEntry);
                    } else if (entry.isValid()) {
                        objective.addEntry(wrapper.user(), scoreboardId, entry);
                    }
                }
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.SET_SCOREBOARD_IDENTITY, null, wrapper -> {
            wrapper.cancel();
            final ScoreboardTracker scoreboardTracker = wrapper.user().get(ScoreboardTracker.class);

            final int action = wrapper.read(Type.UNSIGNED_BYTE); // action
            final int count = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // count
            for (int i = 0; i < count; i++) {
                final long scoreboardId = wrapper.read(BedrockTypes.VAR_LONG); // scoreboard id
                final Pair<ScoreboardObjective, ScoreboardEntry> entry = scoreboardTracker.getEntry(scoreboardId);
                if (action == 0) { // SET_IDENTITY
                    final long playerListId = wrapper.read(BedrockTypes.VAR_LONG); // player list id
                    if (entry == null) continue;
                    final ScoreboardEntry scoreboardEntry = entry.value();

                    if (scoreboardEntry.entityId() == null) {
                        scoreboardEntry.updateTarget(true, playerListId, scoreboardEntry.fakePlayerName());
                        entry.key().updateEntry(wrapper.user(), scoreboardEntry);
                    }
                } else if (action == 1) { // CLEAR_IDENTITY
                    if (entry == null) continue;
                    final ScoreboardEntry scoreboardEntry = entry.value();

                    if (scoreboardEntry.fakePlayerName() != null) {
                        scoreboardEntry.updateTarget(false, null, scoreboardEntry.fakePlayerName());
                        entry.key().updateEntry(wrapper.user(), scoreboardEntry);
                    }
                }
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.REMOVE_OBJECTIVE, ClientboundPackets1_20_3.SCOREBOARD_OBJECTIVE, new PacketHandlers() {
            @Override
            protected void register() {
                map(BedrockTypes.STRING, Type.STRING); // objective name
                create(Type.BYTE, (byte) 1); // mode | 1 = REMOVE
                handler(wrapper -> wrapper.user().get(ScoreboardTracker.class).removeObjective(wrapper.get(Type.STRING, 0)));
            }
        });
    }

}
