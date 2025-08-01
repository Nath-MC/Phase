package com.purpynaxx.phase.modules.movements;

import com.purpynaxx.phase.events.interfaces.client.ClientTick;
import com.purpynaxx.phase.events.interfaces.network.PacketHandler;
import com.purpynaxx.phase.helpers.entity.Player;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.List;

public class NoFall extends Module implements PacketHandler.OUT, ClientTick.AFTER {

    private static final Text description = Text.translatable("modules.movements.nofall.description");

    private final ListSetting<Mode> mode = new ListSetting.Builder<Mode>()
                                                   .id("mode")
                                                   .name(Text.translatable("settings.screen.cycling.title"))
                                                   .description(Text.translatable("settings.screen.cycling.description", name))
                                                   .module(this)
                                                   .values(List.of(
                                                           new Packet(),
                                                           new MLG()
                                                   ))
                                                   .build();


    private NoFall() {
        super(description);
        registerSettings(mode);
    }

    @Override
    public void onPacketSend(net.minecraft.network.packet.Packet<?> packet, CallbackInfo event) {
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

        public abstract void onPacket(net.minecraft.network.packet.Packet<?> packet, CallbackInfo event);

        public void onDeactivate() {}

        @Override
        public abstract String toString();

    }

    private static class Packet extends Mode {

        @Override
        public void onTick() {}

        @Override
        public void onPacket(net.minecraft.network.packet.Packet<?> packet, CallbackInfo event) {
            if (packet instanceof PlayerMoveC2SPacket movePacket && Player.canTakeFallDamage(client.player) && !movePacket.isOnGround() && client.player.getVelocity().y < 0) {
                ((PlayerMoveC2SPacketAccessor) movePacket).setOnGround(true);
            }
        }

        @Override
        public String toString() {
            return "Packet";
        }

    }

    private static class MLG extends Mode {

        private final Item[] items = new Item[]{
                Items.WATER_BUCKET,
                Items.POWDER_SNOW_BUCKET,
                Items.SLIME_BLOCK,
                Items.TWISTING_VINES,
                Items.COBWEB
        };

        private boolean placed = false;
        private float lastYaw;
        private float lastPitch;
        private int lastSlot;
        private Vec3d lastVelocity;
        private Vec3d lastPosition;
        private BlockHitResult hitResult;
        private BlockPos result;
        private Vec3d waterPos;
        private int pickupTimer;
        private int tries = 0;
        private Item lastFoundItem;
        private int lastFoundItemSlot;

        @Override
        public void onTick() {
            boolean isFalling = Player.canTakeFallDamage(client.player);

            if (isFalling && !placed) {

                if (isSafe()) {
                    return; // as we are about to land on a safe surface
                }

                // retrieve the most suitable available item from the hotbar
                if (!findItem()) {
                    return;
                }

                if ((client.player.getPos().y - result.getY()) < client.player.getBlockInteractionRange()) {
                    place();
                }

            } else if (placed && !isFalling) {

                if (pickupTimer > 0) {
                    pickupTimer--;
                } else if (tries < 3) {
                    if (!pickUp()) {
                        pickupTimer = 2;
                        tries++;
                    }
                } else {
                    tries = 0;
                    pickupTimer = 0;
                    placed = false;
                    waterPos = null;
                    restoreStates(lastSlot, lastYaw, lastPitch, lastPosition, lastVelocity);
                    client.player.sendMessage(Text.translatable("modules.movements.nofall.failed_to_pickup").formatted(Formatting.RED), true);
                }
            }
        }

        private boolean findItem() {
            for (Item item : items) {
                int slot = findSlot(item);

                if (slot == -1) continue;

                BlockState blockState = client.world.getBlockState(result);
                if (item == Items.WATER_BUCKET) {

                    if (blockState.getBlock() instanceof SlabBlock && blockState.get(SlabBlock.TYPE) == SlabType.TOP) {
                        continue; // the slab will be waterlogged, and we will still take damage
                    }

                    if (blockState.getBlock() instanceof LeavesBlock) {
                        continue; // waterlogged
                    }

                } else if (item == Items.TWISTING_VINES) {

                    if (blockState.isOpaqueFullCube() && blockState.getBlock() instanceof LeavesBlock) {
                        continue;
                    }
                }

                lastFoundItem = item;
                lastFoundItemSlot = slot;
                return true;

            }
            return false;
        }

        @Range(from = -1, to = 8)
        private int findSlot(Item item) {
            PlayerInventory inventory = client.player.getInventory();

            for (int i = 0; i < PlayerInventory.HOTBAR_SIZE; i++) {
                if (inventory.getStack(i).getItem() == item) {
                    return i;
                }
            }

            return -1;
        }

        @Override
        public void onPacket(net.minecraft.network.packet.Packet<?> packet, CallbackInfo event) {}

        @Override
        public void onDeactivate() {
            placed = false;
            pickupTimer = 0;
            tries = 0;
            result = null;
        }

        @Override
        public String toString() {
            return "MLG";
        }

        private boolean isSafe() {
            Vec3d[] checks = getVec3ds();
            int maxY = Integer.MIN_VALUE;
            BlockHitResult bestResult = null;
            BlockPos result = null;

            for (Vec3d check : checks) {
                RaycastContext raycastContext = new RaycastContext(
                        check,
                        check.subtract(0, client.player.getBlockInteractionRange(), 0),
                        RaycastContext.ShapeType.OUTLINE,
                        RaycastContext.FluidHandling.ANY,
                        client.player
                );

                BlockHitResult blockHitResult = client.world.raycast(raycastContext);
                if (blockHitResult.getType() == BlockHitResult.Type.BLOCK) {

                    int blockY = blockHitResult.getBlockPos().getY();
                    if (client.world.getBlockState(blockHitResult.getBlockPos())
                                    .getFluidState()
                                    .getFluid() != Fluids.EMPTY) {
                        blockY--; // Prevent taking damage on irregular surfaces
                    }

                    // Find the highest right block below the player
                    if (blockY > maxY) {
                        maxY = blockY;
                        bestResult = blockHitResult;
                    }
                }
            }

            if (bestResult != null) {
                hitResult = bestResult;
                result = bestResult.getBlockPos();
            }

            this.result = result;

            if (result == null) {
                return true; // Still falling, stop further checks
            }

            BlockState blockState = client.world.getBlockState(result);

            // Check if the block we're landing on cancels fall damage
            return blockState.isIn(BlockTags.FALL_DAMAGE_RESETTING)
                           || blockState.isOf(Blocks.SLIME_BLOCK)
                           || (blockState.getFluidState().getFluid() == Fluids.WATER
                                       || blockState.getFluidState().getFluid() == Fluids.FLOWING_WATER);
        }

        private Vec3d @NotNull [] getVec3ds() {
            Vec3d playerEyePos = client.player.getEyePos();
            Box box = client.player.getBoundingBox();

            // Create check points at the corners of the player's hitbox and center
            return new Vec3d[]{
                    new Vec3d(box.minX, playerEyePos.y, box.minZ),
                    new Vec3d(box.minX, playerEyePos.y, box.maxZ),
                    new Vec3d(box.maxX, playerEyePos.y, box.minZ),
                    new Vec3d(box.maxX, playerEyePos.y, box.maxZ),
                    playerEyePos,
            };
        }

        private void place() {

            // Save the current player state
            Vec3d position = client.player.getPos();
            float pitch = client.player.getPitch();
            float yaw = client.player.getYaw();
            int previousSlot = client.player.getInventory().getSelectedSlot();

            // Freeze horizontal movement temporarily
            Vec3d velocity = client.player.getVelocity();
            client.player.setVelocity(0, velocity.y, 0);

            // Look at and position ourselves over the block we're going to place on
            Vec3d target = result.up().toBottomCenterPos();
            Vec3d placementPos = new Vec3d(target.getX(), client.player.getY(), target.getZ());
            Player.setPosition(client.player, placementPos, Player.Side.CLIENT);
            Player.lookAt(client.player, target, Player.Side.CLIENT);
            Player.syncFull(client.player);

            client.player.getInventory().setSelectedSlot(lastFoundItemSlot);

            int previousCount = client.player.getMainHandStack().getCount();
            ActionResult actionResult;

            if (lastFoundItem == Items.WATER_BUCKET) {
                actionResult = client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
            } else {
                actionResult = client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
            }

            boolean b = lastFoundItem != client.player.getMainHandStack().getItem();
            boolean b1 = client.player.getMainHandStack().getCount() < previousCount;

            if (actionResult instanceof ActionResult.Success && (b || b1)) {
                placed = true;
                lastYaw = yaw;
                lastPitch = pitch;
                lastSlot = previousSlot;
                lastVelocity = velocity;
                lastPosition = position;
                pickupTimer = 3;
                waterPos = target;

                if (result != null) {
                    Color color = new Color(0, 0, 255, 100);
                    FaceOverlay faceOverlay = new FaceOverlay.Builder()
                                                      .blockPos(result)
                                                      .color(color)
                                                      .drawMode(DrawMode.FILL)
                                                      .ticksToLive(200)
                                                      .debug(true)
                                                      .build();
                    renderer.addRenderable(modules.getModule(NoFall.class).orElseThrow(), faceOverlay);
                }

            } else {
                restoreStates(lastSlot, lastYaw, lastPitch, lastPosition, lastVelocity);
            }

        }

        private void restoreStates(int slot, float yaw, float pitch, Vec3d position, Vec3d velocity) {
            client.player.getInventory().setSelectedSlot(slot);
            client.player.setVelocity(velocity.x, client.player.getVelocity().y, velocity.z);
            Player.setRotation(client.player, yaw, pitch, Player.Side.CLIENT);
            Player.setPosition(client.player, new Vec3d(position.x, client.player.getY(), position.z), Player.Side.CLIENT);
            Player.syncFull(client.player);
        }

        private boolean pickUp() {
            Player.lookAt(client.player, waterPos, Player.Side.CLIENT);

            if (lastFoundItem == Items.WATER_BUCKET || lastFoundItem == Items.POWDER_SNOW_BUCKET) { // Don't try to pick up block(s)
                ActionResult actionResult = client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);

                if (!actionResult.isAccepted()) {
                    return false;
                }
            }

            placed = false;
            restoreStates(lastSlot, lastYaw, lastPitch, lastPosition, lastVelocity);

            return true;
        }

    }

}
