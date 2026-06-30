package com.cobblepass.common;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class NetworkPackets {
    public static final Identifier SYNC_ID = Identifier.of("cobblepass", "sync_battlepass");
    public static final Identifier CLAIM_ID = Identifier.of("cobblepass", "claim_reward");
    public static final Identifier CLAIM_ALL_ID = Identifier.of("cobblepass", "claim_all");
    public static final Identifier OPEN_ID = Identifier.of("cobblepass", "open_battlepass");
    public static final Identifier BUY_PREMIUM_ID = Identifier.of("cobblepass", "buy_premium");

    public record SyncBattlePassPayload(String questsJson, String rewardsJson, String progressJson, String currencyType, String currencyTarget, int premiumCost, long seasonStartTime, long seasonEndTime, boolean seasonActive) implements CustomPayload {
        public static final CustomPayload.Id<SyncBattlePassPayload> TYPE = new CustomPayload.Id<>(SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, SyncBattlePassPayload> CODEC = PacketCodec.of(
                (value, buf) -> {
                    buf.writeString(value.questsJson());
                    buf.writeString(value.rewardsJson());
                    buf.writeString(value.progressJson());
                    buf.writeString(value.currencyType());
                    buf.writeString(value.currencyTarget());
                    buf.writeInt(value.premiumCost());
                    buf.writeVarLong(value.seasonStartTime());
                    buf.writeVarLong(value.seasonEndTime());
                    buf.writeBoolean(value.seasonActive());
                },
                buf -> new SyncBattlePassPayload(
                    buf.readString(262144),
                    buf.readString(262144),
                    buf.readString(262144),
                    buf.readString(256),
                    buf.readString(256),
                    buf.readInt(),
                    buf.readVarLong(),
                    buf.readVarLong(),
                    buf.readBoolean()
                )
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return TYPE;
        }
    }

    public static final Identifier SYNC_FULL_QUESTS_ID = Identifier.of("cobblepass", "sync_full_quests");

    public record SyncFullQuestsPayload(String fullQuestsJson) implements CustomPayload {
        public static final CustomPayload.Id<SyncFullQuestsPayload> TYPE = new CustomPayload.Id<>(SYNC_FULL_QUESTS_ID);
        public static final PacketCodec<RegistryByteBuf, SyncFullQuestsPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.string(524288), SyncFullQuestsPayload::fullQuestsJson,
                SyncFullQuestsPayload::new
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return TYPE;
        }
    }

    public record ClaimRewardPayload(int level, boolean premium) implements CustomPayload {
        public static final CustomPayload.Id<ClaimRewardPayload> TYPE = new CustomPayload.Id<>(CLAIM_ID);
        public static final PacketCodec<RegistryByteBuf, ClaimRewardPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER, ClaimRewardPayload::level,
                PacketCodecs.BOOL, ClaimRewardPayload::premium,
                ClaimRewardPayload::new
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return TYPE;
        }
    }

    public record ClaimAllPayload() implements CustomPayload {
        public static final CustomPayload.Id<ClaimAllPayload> TYPE = new CustomPayload.Id<>(CLAIM_ALL_ID);
        public static final PacketCodec<RegistryByteBuf, ClaimAllPayload> CODEC = PacketCodec.of(
                (value, buf) -> {},
                buf -> new ClaimAllPayload()
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return TYPE;
        }
    }

    public record OpenBattlePassPayload() implements CustomPayload {
        public static final CustomPayload.Id<OpenBattlePassPayload> TYPE = new CustomPayload.Id<>(OPEN_ID);
        public static final PacketCodec<RegistryByteBuf, OpenBattlePassPayload> CODEC = PacketCodec.of(
                (value, buf) -> {},
                buf -> new OpenBattlePassPayload()
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return TYPE;
        }
    }

    public record BuyPremiumPayload() implements CustomPayload {
        public static final CustomPayload.Id<BuyPremiumPayload> TYPE = new CustomPayload.Id<>(BUY_PREMIUM_ID);
        public static final PacketCodec<RegistryByteBuf, BuyPremiumPayload> CODEC = PacketCodec.of(
                (value, buf) -> {},
                buf -> new BuyPremiumPayload()
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return TYPE;
        }
    }

    public static final Identifier SAVE_QUESTS_ID = Identifier.of("cobblepass", "save_quests");
    public static final Identifier SAVE_REWARDS_ID = Identifier.of("cobblepass", "save_rewards");
    public static final Identifier SAVE_CONFIG_ID = Identifier.of("cobblepass", "save_config");

    public record SaveQuestsPayload(String questsJson) implements CustomPayload {
        public static final CustomPayload.Id<SaveQuestsPayload> TYPE = new CustomPayload.Id<>(SAVE_QUESTS_ID);
        public static final PacketCodec<RegistryByteBuf, SaveQuestsPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.string(262144), SaveQuestsPayload::questsJson,
                SaveQuestsPayload::new
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return TYPE;
        }
    }

    public record SaveRewardsPayload(String rewardsJson) implements CustomPayload {
        public static final CustomPayload.Id<SaveRewardsPayload> TYPE = new CustomPayload.Id<>(SAVE_REWARDS_ID);
        public static final PacketCodec<RegistryByteBuf, SaveRewardsPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.string(262144), SaveRewardsPayload::rewardsJson,
                SaveRewardsPayload::new
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return TYPE;
        }
    }

    public record SaveConfigPayload(String configJson) implements CustomPayload {
        public static final CustomPayload.Id<SaveConfigPayload> TYPE = new CustomPayload.Id<>(SAVE_CONFIG_ID);
        public static final PacketCodec<RegistryByteBuf, SaveConfigPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.string(262144), SaveConfigPayload::configJson,
                SaveConfigPayload::new
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return TYPE;
        }
    }

    public static final Identifier REQUEST_FULL_QUESTS_ID = Identifier.of("cobblepass", "request_full_quests");

    public record RequestFullQuestsPayload() implements CustomPayload {
        public static final CustomPayload.Id<RequestFullQuestsPayload> TYPE = new CustomPayload.Id<>(REQUEST_FULL_QUESTS_ID);
        public static final PacketCodec<RegistryByteBuf, RequestFullQuestsPayload> CODEC = PacketCodec.of(
                (value, buf) -> {},
                buf -> new RequestFullQuestsPayload()
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return TYPE;
        }
    }

    public static final Identifier PIN_QUEST_ID = Identifier.of("cobblepass", "pin_quest");

    public record TogglePinQuestPayload(String questId) implements CustomPayload {
        public static final CustomPayload.Id<TogglePinQuestPayload> TYPE = new CustomPayload.Id<>(PIN_QUEST_ID);
        public static final PacketCodec<RegistryByteBuf, TogglePinQuestPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, TogglePinQuestPayload::questId,
                TogglePinQuestPayload::new
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return TYPE;
        }
    }
}
