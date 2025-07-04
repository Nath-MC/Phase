package com.purpynaxx.phase.modules.movements;

import com.purpynaxx.phase.events.interfaces.client.ClientTick;
import com.purpynaxx.phase.events.interfaces.network.PacketHandler;
import com.purpynaxx.phase.helpers.entity.Player;
import com.purpynaxx.phase.helpers.render.BlockOverlay;
import com.purpynaxx.phase.mixins.accessors.PlayerMoveC2SPacketAccessor;
import com.purpynaxx.phase.modules.impl.Module;
import com.purpynaxx.phase.settings.CyclingSetting;
import com.purpynaxx.phase.settings.Setting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.purpynaxx.phase.Phase.IS_DEV_ENVIRONMENT;

public class NoFall extends Module implements PacketHandler.OUT, ClientTick.AFTER {

    private static final Text description = Text.translatable("modules.movements.nofall.description");
    private static final Set<Item> suitableItems = new LinkedHashSet<>();

    static {
        suitableItems.add(Items.WATER_BUCKET);
        suitableItems.add(Items.POWDER_SNOW_BUCKET);
        suitableItems.add(Items.SLIME_BLOCK);
        suitableItems.add(Items.LADDER);
        //suitableItems.add(Items.VINE);
        suitableItems.add(Items.SCAFFOLDING);
        //suitableItems.add(Items.WEEPING_VINES);
        //suitableItems.add(Items.TWISTING_VINES);
        suitableItems.add(Items.COBWEB);
    }

    private final Setting<Mode> mode = registerSetting(new CyclingSetting<>("mode", Text.translatable("settings.screen.cycling.title"), Text.translatable("settings.screen.cycling.description", name), Mode.class));

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

    private Item itemInUse;

    private NoFall() {
        super(description);
    }

    private static Vec3d @NotNull [] getChecks() {
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

    @Override
    public void onPacketSend(Packet<?> packet, CallbackInfo event) {
        if (mode.getValue() == Mode.PACKET) {

            if (
                    packet instanceof PlayerMoveC2SPacket movePacket
                            && Player.canTakeFallDamage(client.player)
                            && !movePacket.isOnGround()
            ) {

                // Only modify the packet if we're actually falling, not jumping or flying
                if (client.player.getVelocity().y < 0) {
                    ((PlayerMoveC2SPacketAccessor) movePacket).setOnGround(true);
                }
            }
        }
    }

    @Override
    public void afterClientTick(MinecraftClient client) {
        if (mode.getValue() == Mode.MLG) {
            boolean isFalling = Player.canTakeFallDamage(client.player);
            if (isFalling && !placed) {
                handleFall();
            } else if (placed && !isFalling) { // Ensure we don't try to pick up water/snow if we have not stopped falling yet
                handlePickup();
            }
        }
    }

    private void handlePickup() {
        if (pickupTimer > 0) {
            pickupTimer--;
        } else if (tries < 3) { // Pick up only if the timer has expired and we did not exceed the number of tries
            if (!pickUp()) {
                pickupTimer = 2;
                tries++;
            }
        } else { // If we have already tried 3 times, we forget and give up
            tries = 0;
            pickupTimer = 0;
            placed = false;
            waterPos = null;
            restoreStates(lastSlot, lastYaw, lastPitch, lastPosition, lastVelocity);
            client.player.sendMessage(Text.translatable("modules.movements.nofall.failed_to_pickup").formatted(Formatting.RED), true);
        }
    }

    private void handleFall() {

        // Don't continue if we're about to land on a safe surface
        if (isSafe()) {
            return;
        }

        // Check if we have an appropriate item in the hotbar
        int itemSlot = findItemSlot();
        if (itemSlot == -1) {
            return;
        }

        // Only attempt placement when the ground is within reach
        if ((client.player.getPos().y - result.getY()) < client.player.getBlockInteractionRange()) {
            place();
        }
    }

    @Range(from = -1, to = 8)
    private int findItemSlot() {
        PlayerInventory inventory = client.player.getInventory();

        for (Item item : suitableItems) {

            if (item == Items.WATER_BUCKET) {
                BlockState blockState = client.world.getBlockState(result);
                if (blockState.getBlock() instanceof SlabBlock && blockState.get(SlabBlock.TYPE) == SlabType.TOP) {
                    continue;
                }

                if (blockState.getBlock() instanceof LeavesBlock) {
                    continue;
                }
            }

            int itemSlot = -1; // Initialize itemSlot to -1, indicating no item found

            // Searching for the item in the hotbar (slots 0-8)
            for (int i = 0; i < 9; i++) {
                ItemStack stack = inventory.getStack(i);

                if (stack.getItem() == item) {
                    itemSlot = i;
                    break; // Return the index of the found item
                }
            }

            if (itemSlot != -1) {
                itemInUse = item; // Save the item being used
                return itemSlot; // Return the first suitable item found in the hotbar
            }
        }

        return -1; // Return -1 if no suitable item is found
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

        // Select the item
        int itemSlot = findItemSlot();
        client.player.getInventory().setSelectedSlot(itemSlot);

        int previousCount = client.player.getMainHandStack().getCount();
        ActionResult actionResult;

        if (itemInUse == Items.WATER_BUCKET) {
            actionResult = client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        } else {
            actionResult = client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
        }

        boolean b = itemInUse != client.player.getMainHandStack().getItem();
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

            if (IS_DEV_ENVIRONMENT)
                if (result != null) {
                    Color color = new Color(0, 0, 255, 100);
                    renderer.addRenderable(new BlockOverlay(result.up(), color, true, 200));
                }
        } else {
            // Restore if placement failed
            restoreStates(lastSlot, lastYaw, lastPitch, lastPosition, lastVelocity);
        }

    }

    private boolean pickUp() {
        Player.lookAt(client.player, waterPos, Player.Side.CLIENT);

        if (itemInUse == Items.WATER_BUCKET || itemInUse == Items.POWDER_SNOW_BUCKET) { // Don't try to pick up block(s)
            ActionResult actionResult = client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);

            if (!actionResult.isAccepted()) {
                return false; // If the pick-up was not successful
            }
        }

        placed = false;

        // Restore previous states
        restoreStates(lastSlot, lastYaw, lastPitch, lastPosition, lastVelocity);

        return true;
    }

    private void restoreStates(int slot, float yaw, float pitch, Vec3d position, Vec3d velocity) {
        client.player.getInventory().setSelectedSlot(slot);
        client.player.setVelocity(velocity.x, client.player.getVelocity().y, velocity.z);
        Player.setRotation(client.player, yaw, pitch, Player.Side.CLIENT);
        Player.setPosition(client.player, new Vec3d(position.x, client.player.getY(), position.z), Player.Side.CLIENT);
        Player.syncFull(client.player);
    }

    private boolean isSafe() {
        result = detectHighestSurface();

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

    private @Nullable BlockPos detectHighestSurface() {
        Vec3d[] checks = getChecks();

        int maxY = Integer.MIN_VALUE;
        BlockHitResult bestResult = null;

        for (Vec3d check : checks) {
            RaycastContext raycastContext = new RaycastContext(
                    check,
                    check.subtract(0, client.player.getBlockInteractionRange(), 0),
                    RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.ANY, // Check for any fluid, not just water
                    client.player
            );

            BlockHitResult blockHitResult = client.world.raycast(raycastContext);

            if (blockHitResult.getType() == BlockHitResult.Type.BLOCK) {

                int blockY = blockHitResult.getBlockPos().getY();

                if (client.world.getBlockState(blockHitResult.getBlockPos()).getFluidState().getFluid() != Fluids.EMPTY) {
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
            return bestResult.getBlockPos();
        }

        return null;
    }

    @Override
    public void onDeactivate() {
        placed = false;
        pickupTimer = 0;
        tries = 0;
        result = null;
    }

    private enum Mode {
        PACKET("Server-side no fall damage"),
        MLG("Place (e.g. Water Bucket)");

        private final String description;

        Mode(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            String name = name();
            return name.charAt(0) + name.substring(1).toLowerCase();
        }
    }

}
