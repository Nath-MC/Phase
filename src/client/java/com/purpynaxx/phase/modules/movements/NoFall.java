package com.purpynaxx.phase.modules.movements;

import com.purpynaxx.phase.events.interfaces.client.ClientTick;
import com.purpynaxx.phase.events.interfaces.network.PacketHandler;
import com.purpynaxx.phase.helpers.player.PlayerHelper;
import com.purpynaxx.phase.helpers.player.Rotations;
import com.purpynaxx.phase.mixins.accessors.PlayerMoveC2SPacketAccessor;
import com.purpynaxx.phase.modules.Module;
import com.purpynaxx.phase.render.DrawMode;
import com.purpynaxx.phase.render.impl.FaceOverlay;
import com.purpynaxx.phase.settings.ListSetting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class NoFall extends Module implements PacketHandler.OUT, ClientTick.AFTER {

    private static final Text DESCRIPTION = Text.translatable("modules.movements.nofall.description");

    private final ListSetting<Mode> mode = new ListSetting.Builder<Mode>()
                                                   .id("mode")
                                                   .name(Text.translatable("settings.screen.cycling.title"))
                                                   .description(Text.translatable("settings.screen.cycling.description", name))
                                                   .module(this)
                                                   .values(Arrays.asList(new PacketMode(), new MLGMode()))
                                                   .build();

    private NoFall() {
        super(DESCRIPTION);
        registerSettings(mode);
    }

    @Override
    public void onPacketSend(Packet<?> packet, CallbackInfo event) {
        mode.get().onPacket(packet, event);
    }

    @Override
    public void afterClientTick(MinecraftClient client) {
        mode.get().onTick();
    }

    @Override
    public void onDeactivate() {
        mode.get().onDeactivate();
    }

    private abstract static class Mode {

        public abstract void onTick();

        public abstract void onPacket(Packet<?> packet, CallbackInfo event);

        public void onDeactivate() {}

        @Override
        public abstract String toString();

    }

    private static class PacketMode extends Mode {

        @Override
        public void onTick() {}

        @Override
        public void onPacket(Packet<?> packet, CallbackInfo event) {
            if (packet instanceof PlayerMoveC2SPacket movePacket &&
                        PlayerHelper.canTakeFallDamage() &&
                        !movePacket.isOnGround() &&
                        client.player.getVelocity().y < 0) {
                ((PlayerMoveC2SPacketAccessor) movePacket).setOnGround(true);
            }
        }

        @Override
        public String toString() {
            return "Packet";
        }

    }

    private static class MLGMode extends Mode {

        private static final Rotations rotations = Rotations.getInstance();
        private static final List<Item> ITEMS = Arrays.asList(
                Items.WATER_BUCKET,
                Items.POWDER_SNOW_BUCKET,
                Items.SLIME_BLOCK,
                Items.TWISTING_VINES,
                Items.COBWEB
        );

        private State currentState = State.IDLE;

        private int lastSlot;
        private int actionTimer;
        private int placeAttempts;

        private Vec3d lastVelocity;
        private Vec3d lastPosition;

        private BlockPos placementPos;
        private BlockHitResult hitResult;

        private Item currentItem;

        @Override
        public void onTick() {
            switch (currentState) {
                case IDLE:
                    handleIdleState();
                    break;
                case PICKING_UP:
                    handlePickingUpState();
                    break;
            }
        }

        private void handleIdleState() {
            if (!PlayerHelper.canTakeFallDamage()) return;

            placementPos = findLandingSpot();
            if (placementPos == null || isLandingSpotSafe(placementPos)) {
                return;
            }

            currentItem = findAvailableItem(placementPos);
            if (currentItem == null) {
                return;
            }

            if (client.player.getPos().y - placementPos.getY() <= client.player.getBlockInteractionRange()) {
                startPlacing();
            }
        }

        private void startPlacing() {
            savePlayerState();
            place();
        }

        private void place() {
            // Freeze horizontal movement
            client.player.setVelocity(0, client.player.getVelocity().y, 0);

            // Center player over the placement block
            Vec3d targetCenter = placementPos.up().toBottomCenterPos();
            PlayerHelper.setPosition(new Vec3d(targetCenter.getX(), client.player.getY(), targetCenter.getZ()), PlayerHelper.Side.BOTH);

            int itemSlot = findSlot(currentItem);
            if (itemSlot == -1) {
                reset();
                throw new RuntimeException(); // shouldn't happen
            }
            client.player.getInventory().setSelectedSlot(itemSlot);

            rotations.submit(client.player.getYaw(), 90, () -> {
                int previousCount = client.player.getMainHandStack().getCount();
                ActionResult actionResult;

                if (currentItem == Items.WATER_BUCKET || currentItem == Items.POWDER_SNOW_BUCKET) {
                    actionResult = client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                } else {
                    actionResult = client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
                }

                boolean itemUsed = currentItem != client.player.getMainHandStack().getItem() || client.player.getMainHandStack().getCount() < previousCount;

                if (actionResult.isAccepted() && itemUsed) {
                    currentState = State.PICKING_UP;
                    actionTimer = 3;
                    placeAttempts = 0;
                    renderPlacementOverlay();
                } else {
                    restorePlayerState();
                    reset();
                }
            });
        }

        private void restorePlayerState() {
            if (lastVelocity == null || lastPosition == null) return;
            client.player.getInventory().setSelectedSlot(lastSlot);
            client.player.setVelocity(lastVelocity.x, client.player.getVelocity().y, lastVelocity.z);
            PlayerHelper.setPosition(new Vec3d(lastPosition.x, client.player.getY(), lastPosition.z), PlayerHelper.Side.BOTH);
        }

        private void reset() {
            currentState = State.IDLE;
            actionTimer = 0;
            placeAttempts = 0;
            //            placementPos = null;
            //            hitResult = null;
            //            currentItem = null;
            lastVelocity = null;
            lastPosition = null;
        }

        private void renderPlacementOverlay() {
            if (placementPos != null) {
                FaceOverlay faceOverlay = new FaceOverlay.Builder()
                                                  .blockPos(placementPos)
                                                  .color(new Color(0, 0, 255, 100))
                                                  .drawMode(DrawMode.FILL)
                                                  .ticksToLive(200) // 10 seconds
                                                  .debug(true)
                                                  .build();
                renderer.addRenderable(modules.getModule(NoFall.class).orElseThrow(), faceOverlay);
            }
        }

        private void savePlayerState() {
            lastSlot = client.player.getInventory().getSelectedSlot();
            lastVelocity = client.player.getVelocity();
            lastPosition = client.player.getPos();
        }

        @Nullable
        private BlockPos findLandingSpot() {
            Vec3d playerEyePos = client.player.getEyePos();
            Box box = client.player.getBoundingBox();
            Vec3d[] checkPoints = {
                    new Vec3d(box.minX, playerEyePos.y, box.minZ),
                    new Vec3d(box.minX, playerEyePos.y, box.maxZ),
                    new Vec3d(box.maxX, playerEyePos.y, box.minZ),
                    new Vec3d(box.maxX, playerEyePos.y, box.maxZ),
                    playerEyePos
            };

            BlockPos bestPos = null;
            int highestY = Integer.MIN_VALUE;

            for (Vec3d startPos : checkPoints) {
                RaycastContext context = new RaycastContext(
                        startPos,
                        startPos.subtract(0, client.player.getBlockInteractionRange(), 0),
                        RaycastContext.ShapeType.OUTLINE,
                        RaycastContext.FluidHandling.ANY,
                        client.player
                );

                BlockHitResult rayResult = client.world.raycast(context);
                if (rayResult.getType() == BlockHitResult.Type.BLOCK) {
                    BlockPos currentPos = rayResult.getBlockPos();
                    int blockY = currentPos.getY();

                    if (client.world.getBlockState(currentPos).getFluidState().getFluid() != Fluids.EMPTY) {
                        blockY--; // Adjust for landing on waterlogged blocks
                    }

                    if (blockY > highestY) {
                        highestY = blockY;
                        bestPos = currentPos;
                        this.hitResult = rayResult; // Store the hit result for the best position
                    }
                }
            }
            return bestPos;
        }

        private boolean isLandingSpotSafe(BlockPos pos) {
            if (pos == null) return true; // No ground below, technically "safe" from fall damage for now
            BlockState blockState = client.world.getBlockState(pos);
            return blockState.isIn(BlockTags.FALL_DAMAGE_RESETTING) ||
                           blockState.isOf(Blocks.SLIME_BLOCK) ||
                           !blockState.getFluidState().isEmpty();
        }

        @Nullable
        private Item findAvailableItem(BlockPos pos) {
            BlockState blockState = client.world.getBlockState(pos);
            for (Item item : ITEMS) {
                if (findSlot(item) != -1) {
                    // Item-specific placement validation
                    if (item == Items.WATER_BUCKET && (blockState.getBlock() instanceof SlabBlock && blockState.get(SlabBlock.TYPE) == SlabType.TOP || blockState.getBlock() instanceof LeavesBlock)) {
                        continue;
                    }
                    if (item == Items.TWISTING_VINES && (!blockState.isOpaqueFullCube() || blockState.getBlock() instanceof LeavesBlock)) {
                        continue;
                    }
                    return item;
                }
            }
            return null;
        }

        private int findSlot(Item item) {
            PlayerInventory inventory = client.player.getInventory();
            for (int i = 0; i < PlayerInventory.HOTBAR_SIZE; i++) {
                if (inventory.getStack(i).getItem() == item) {
                    return i;
                }
            }
            return -1;
        }

        private void handlePickingUpState() {
            if (actionTimer > 0) {
                actionTimer--;
                return;
            }

            if (placeAttempts < 3) {
                pickUp();
                actionTimer = 2;
                placeAttempts++;
            } else {
                client.player.sendMessage(Text.translatable("modules.movements.nofall.failed_to_pickup").formatted(Formatting.RED), true);
                reset();
            }
        }

        private void pickUp() {
            Vec3d pickupTarget = placementPos.up().toBottomCenterPos();
            rotations.submit(Rotations.getYaw(pickupTarget), Rotations.getPitch(pickupTarget), () -> {
                if (currentItem == Items.WATER_BUCKET || currentItem == Items.POWDER_SNOW_BUCKET) {
                    ActionResult actionResult = Objects.requireNonNull(client.interactionManager).interactItem(client.player, Hand.MAIN_HAND);
                    if (!actionResult.isAccepted()) return; // Failed to pick up, will retry
                }
                restorePlayerState();
                reset();
            });
        }

        @Override
        public void onPacket(Packet<?> packet, CallbackInfo event) {}

        @Override
        public void onDeactivate() {
            if (currentState != State.IDLE) {
                restorePlayerState();
            }
            reset();
        }

        @Override
        public String toString() {
            return "MLG";
        }

        private enum State {
            IDLE,
            PICKING_UP
        }

    }

}
