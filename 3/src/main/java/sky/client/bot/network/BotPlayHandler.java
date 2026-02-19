package sky.client.bot.network;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.common.*;
import net.minecraft.network.packet.c2s.common.*;
import net.minecraft.network.DisconnectionInfo;
import sky.client.bot.BotInstance;

public class BotPlayHandler implements ClientPlayPacketListener {
    private final ClientConnection connection;
    private final BotInstance bot;

    public BotPlayHandler(ClientConnection connection, BotInstance bot) {
        this.connection = connection;
        this.bot = bot;
    }

    @Override
    public void onPlayerPositionLook(PlayerPositionLookS2CPacket packet) {
        connection.send(new TeleportConfirmC2SPacket(packet.teleportId()));
        // Ошибка сказала, что x(), y(), z() - это не методы. Значит это ПОЛЯ.
        connection.send(new PlayerMoveC2SPacket.Full(
                packet.change().x, packet.change().y, packet.change().z,
                packet.change().yaw, packet.change().pitch, true, false
        ));
    }

    // Трюк с Object, чтобы компилятор не искал несуществующий символ класса
    // Но Gradle требует PingResultS2CPacket. Давай попробуем импортировать его СИЛОЙ.
    @Override
    public void onPingResult(net.minecraft.network.packet.s2c.common.PingResultS2CPacket packet) {}

    @Override
    public void onKeepAlive(KeepAliveS2CPacket packet) {
        // Заменяем id() на getId(), раз он его не видит
        connection.send(new KeepAliveC2SPacket(packet.getId()));
    }

    @Override
    public void onPing(CommonPingS2CPacket packet) {
        // Заменяем id() на getId() или getParameter()
        connection.send(new CommonPongC2SPacket(packet.getId()));
    }

    @Override public void onCustomReportDetails(CustomReportDetailsS2CPacket p) {}
    @Override public void onDisconnected(DisconnectionInfo info) {}
    @Override public boolean isConnectionOpen() { return connection.isOpen(); }
    @Override public void onCustomPayload(CustomPayloadS2CPacket p) {}
    @Override public void onResourcePackSend(ResourcePackSendS2CPacket p) {}
    @Override public void onResourcePackRemove(ResourcePackRemoveS2CPacket p) {}
    @Override public void onCookieRequest(CookieRequestS2CPacket p) {}
    @Override public void onStoreCookie(StoreCookieS2CPacket p) {}
    @Override public void onServerLinks(ServerLinksS2CPacket p) {}

    // Остальные заглушки (onEntitySpawn и т.д.) просто оставь пустыми
    @Override public void onEntitySpawn(EntitySpawnS2CPacket p) {}
    @Override public void onExperienceOrbSpawn(ExperienceOrbSpawnS2CPacket p) {}
    @Override public void onScoreboardObjectiveUpdate(ScoreboardObjectiveUpdateS2CPacket p) {}
    @Override public void onEntityAnimation(EntityAnimationS2CPacket p) {}
    @Override public void onDamageTilt(DamageTiltS2CPacket p) {}
    @Override public void onStatistics(StatisticsS2CPacket p) {}
    @Override public void onRecipeBookAdd(RecipeBookAddS2CPacket p) {}
    @Override public void onRecipeBookRemove(RecipeBookRemoveS2CPacket p) {}
    @Override public void onRecipeBookSettings(RecipeBookSettingsS2CPacket p) {}
    @Override public void onBlockBreakingProgress(BlockBreakingProgressS2CPacket p) {}
    @Override public void onSignEditorOpen(SignEditorOpenS2CPacket p) {}
    @Override public void onBlockEntityUpdate(BlockEntityUpdateS2CPacket p) {}
    @Override public void onBlockEvent(BlockEventS2CPacket p) {}
    @Override public void onBlockUpdate(BlockUpdateS2CPacket p) {}
    @Override public void onGameMessage(GameMessageS2CPacket p) {}
    @Override public void onChatMessage(ChatMessageS2CPacket p) {}
    @Override public void onProfilelessChatMessage(ProfilelessChatMessageS2CPacket p) {}
    @Override public void onRemoveMessage(RemoveMessageS2CPacket p) {}
    @Override public void onChunkDeltaUpdate(ChunkDeltaUpdateS2CPacket p) {}
    @Override public void onMapUpdate(MapUpdateS2CPacket p) {}
    @Override public void onCloseScreen(CloseScreenS2CPacket p) {}
    @Override public void onInventory(InventoryS2CPacket p) {}
    @Override public void onOpenHorseScreen(OpenHorseScreenS2CPacket p) {}
    @Override public void onScreenHandlerPropertyUpdate(ScreenHandlerPropertyUpdateS2CPacket p) {}
    @Override public void onScreenHandlerSlotUpdate(ScreenHandlerSlotUpdateS2CPacket p) {}
    @Override public void onEntityStatus(EntityStatusS2CPacket p) {}
    @Override public void onEntityAttach(EntityAttachS2CPacket p) {}
    @Override public void onEntityPassengersSet(EntityPassengersSetS2CPacket p) {}
    @Override public void onExplosion(ExplosionS2CPacket p) {}
    @Override public void onGameStateChange(GameStateChangeS2CPacket p) {}
    @Override public void onChunkData(ChunkDataS2CPacket p) {}
    @Override public void onChunkBiomeData(ChunkBiomeDataS2CPacket p) {}
    @Override public void onUnloadChunk(UnloadChunkS2CPacket p) {}
    @Override public void onWorldEvent(WorldEventS2CPacket p) {}
    @Override public void onGameJoin(GameJoinS2CPacket p) {}
    @Override public void onEntity(EntityS2CPacket p) {}
    @Override public void onMoveMinecartAlongTrack(MoveMinecartAlongTrackS2CPacket p) {}
    @Override public void onPlayerRotation(PlayerRotationS2CPacket p) {}
    @Override public void onParticle(ParticleS2CPacket p) {}
    @Override public void onPlayerAbilities(PlayerAbilitiesS2CPacket p) {}
    @Override public void onPlayerRemove(PlayerRemoveS2CPacket p) {}
    @Override public void onPlayerList(PlayerListS2CPacket p) {}
    @Override public void onEntitiesDestroy(EntitiesDestroyS2CPacket p) {}
    @Override public void onRemoveEntityStatusEffect(RemoveEntityStatusEffectS2CPacket p) {}
    @Override public void onPlayerRespawn(PlayerRespawnS2CPacket p) {}
    @Override public void onEntitySetHeadYaw(EntitySetHeadYawS2CPacket p) {}
    @Override public void onUpdateSelectedSlot(UpdateSelectedSlotS2CPacket p) {}
    @Override public void onScoreboardDisplay(ScoreboardDisplayS2CPacket p) {}
    @Override public void onEntityTrackerUpdate(EntityTrackerUpdateS2CPacket p) {}
    @Override public void onEntityVelocityUpdate(EntityVelocityUpdateS2CPacket p) {}
    @Override public void onEntityEquipmentUpdate(EntityEquipmentUpdateS2CPacket p) {}
    @Override public void onExperienceBarUpdate(ExperienceBarUpdateS2CPacket p) {}
    @Override public void onHealthUpdate(HealthUpdateS2CPacket p) {}
    @Override public void onTeam(TeamS2CPacket p) {}
    @Override public void onScoreboardScoreUpdate(ScoreboardScoreUpdateS2CPacket p) {}
    @Override public void onScoreboardScoreReset(ScoreboardScoreResetS2CPacket p) {}
    @Override public void onPlayerSpawnPosition(PlayerSpawnPositionS2CPacket p) {}
    @Override public void onWorldTimeUpdate(WorldTimeUpdateS2CPacket p) {}
    @Override public void onPlaySound(PlaySoundS2CPacket p) {}
    @Override public void onPlaySoundFromEntity(PlaySoundFromEntityS2CPacket p) {}
    @Override public void onItemPickupAnimation(ItemPickupAnimationS2CPacket p) {}
    @Override public void onEntityPositionSync(EntityPositionSyncS2CPacket p) {}
    @Override public void onEntityPosition(EntityPositionS2CPacket p) {}
    @Override public void onUpdateTickRate(UpdateTickRateS2CPacket p) {}
    @Override public void onTickStep(TickStepS2CPacket p) {}
    @Override public void onEntityAttributes(EntityAttributesS2CPacket p) {}
    @Override public void onEntityStatusEffect(EntityStatusEffectS2CPacket p) {}
    @Override public void onEndCombat(EndCombatS2CPacket p) {}
    @Override public void onEnterCombat(EnterCombatS2CPacket p) {}
    @Override public void onDeathMessage(DeathMessageS2CPacket p) {}
    @Override public void onDifficulty(DifficultyS2CPacket p) {}
    @Override public void onSetCameraEntity(SetCameraEntityS2CPacket p) {}
    @Override public void onWorldBorderInitialize(WorldBorderInitializeS2CPacket p) {}
    @Override public void onWorldBorderInterpolateSize(WorldBorderInterpolateSizeS2CPacket p) {}
    @Override public void onWorldBorderSizeChanged(WorldBorderSizeChangedS2CPacket p) {}
    @Override public void onWorldBorderWarningTimeChanged(WorldBorderWarningTimeChangedS2CPacket p) {}
    @Override public void onWorldBorderWarningBlocksChanged(WorldBorderWarningBlocksChangedS2CPacket p) {}
    @Override public void onWorldBorderCenterChanged(WorldBorderCenterChangedS2CPacket p) {}
    @Override public void onPlayerListHeader(PlayerListHeaderS2CPacket p) {}
    @Override public void onBossBar(BossBarS2CPacket p) {}
    @Override public void onCooldownUpdate(CooldownUpdateS2CPacket p) {}
    @Override public void onVehicleMove(VehicleMoveS2CPacket p) {}
    @Override public void onAdvancements(AdvancementUpdateS2CPacket p) {}
    @Override public void onSelectAdvancementTab(SelectAdvancementTabS2CPacket p) {}
    @Override public void onCraftFailedResponse(CraftFailedResponseS2CPacket p) {}
    @Override public void onCommandTree(CommandTreeS2CPacket p) {}
    @Override public void onStopSound(StopSoundS2CPacket p) {}
    @Override public void onCommandSuggestions(CommandSuggestionsS2CPacket p) {}
    @Override public void onSynchronizeRecipes(SynchronizeRecipesS2CPacket p) {}
    @Override public void onLookAt(LookAtS2CPacket p) {}
    @Override public void onNbtQueryResponse(NbtQueryResponseS2CPacket p) {}
    @Override public void onLightUpdate(LightUpdateS2CPacket p) {}
    @Override public void onOpenWrittenBook(OpenWrittenBookS2CPacket p) {}
    @Override public void onOpenScreen(OpenScreenS2CPacket p) {}
    @Override public void onSetTradeOffers(SetTradeOffersS2CPacket p) {}
    @Override public void onChunkLoadDistance(ChunkLoadDistanceS2CPacket p) {}
    @Override public void onSimulationDistance(SimulationDistanceS2CPacket p) {}
    @Override public void onChunkRenderDistanceCenter(ChunkRenderDistanceCenterS2CPacket p) {}
    @Override public void onPlayerActionResponse(PlayerActionResponseS2CPacket p) {}
    @Override public void onOverlayMessage(OverlayMessageS2CPacket p) {}
    @Override public void onSubtitle(SubtitleS2CPacket p) {}
    @Override public void onTitle(TitleS2CPacket p) {}
    @Override public void onTitleFade(TitleFadeS2CPacket p) {}
    @Override public void onTitleClear(ClearTitleS2CPacket p) {}
    @Override public void onServerMetadata(ServerMetadataS2CPacket p) {}
    @Override public void onChatSuggestions(ChatSuggestionsS2CPacket p) {}
    @Override public void onBundle(BundleS2CPacket p) {}
    @Override public void onEntityDamage(EntityDamageS2CPacket p) {}
    @Override public void onEnterReconfiguration(EnterReconfigurationS2CPacket p) {}
    @Override public void onStartChunkSend(StartChunkSendS2CPacket p) {}
    @Override public void onChunkSent(ChunkSentS2CPacket p) {}
    @Override public void onDebugSample(DebugSampleS2CPacket p) {}
    @Override public void onProjectilePower(ProjectilePowerS2CPacket p) {}
    @Override public void onSetCursorItem(SetCursorItemS2CPacket p) {}
    @Override public void onSetPlayerInventory(SetPlayerInventoryS2CPacket p) {}
}