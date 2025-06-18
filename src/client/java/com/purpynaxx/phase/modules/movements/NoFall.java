package com.purpynaxx.phase.modules.movements;

import com.purpynaxx.phase.events.listeners.ClientTickEndListener;
import com.purpynaxx.phase.events.listeners.PacketSendListener;
import com.purpynaxx.phase.helpers.Player;
import com.purpynaxx.phase.mixins.accessors.PlayerMoveC2SPacketAccessor;
import com.purpynaxx.phase.modules.impl.Module;
import com.purpynaxx.phase.settings.CyclingSetting;
import com.purpynaxx.phase.settings.Setting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class NoFall extends Module implements PacketSendListener, ClientTickEndListener {

    private static final float DEFAULT_BLOCK_INTERACTION_RANGE = 4.5f;
    private static final float FALL_DAMAGE_THRESHOLD = 3.0f;
    private static final Text description = Text.translatable("modules.movements.nofall.description");

    private final Setting<Mode> mode = registerSetting(new CyclingSetting<>("mode", Text.translatable("settings.screen.cycling.title"), Text.translatable("settings.screen.cycling.description", name), Mode.class));

    private boolean waterBucketUsed = false;
    private boolean isAttemptingPlacement = false;

    private float lastYaw;
    private float lastPitch;
    private int lastSlot;
    private Vec3d lastVelocity;

    private BlockHitResult result;
    private Vec3d waterPos;

    private long lastWaterPlacementTime = 0;
    private int waterPickupTimer = 0;

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

    private static boolean hasItemInHotbar(PlayerInventory inventory, Item item) {
        // Only check hotbar slots (0-8)
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() == item) {
                return true;
            }
        }
        return false;
    }

    private static int findItemSlot(Item item) {
        PlayerInventory inventory = client.player.getInventory();
        // Only check hotbar slots (0-8)
        for (int i = 0; i < 9; i++) {
            if (inventory.getStack(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onPacketSend(Packet<?> packet, CallbackInfo event) {
        if (mode.getValue() == Mode.PACKET) {
            if (packet instanceof PlayerMoveC2SPacket movePacket &&
                    Player.isInSurvival(client.player) &&
                    !movePacket.isOnGround() &&
                    client.player.fallDistance > FALL_DAMAGE_THRESHOLD) {

                // Only modify the packet if we're actually falling, not jumping or flying
                if (client.player.getVelocity().y < 0) {
                    ((PlayerMoveC2SPacketAccessor) movePacket).setOnGround(true);
                }
            }
        }
    }

    @Override
    public void onClientTickEnd(MinecraftClient client) {
        if (mode.getValue() == Mode.MLG) {
            handleMlg();
        }
    }

    private void handleMlg() {
        float currentFallDistance = (float) client.player.fallDistance;

        long currentTime = System.currentTimeMillis();
        if (isAttemptingPlacement && currentTime - lastWaterPlacementTime < 500) {
            return;
        }

        if (Player.isInSurvival(client.player) && currentFallDistance > FALL_DAMAGE_THRESHOLD && !waterBucketUsed) {
            handleFallingPlayer();
        } else if (waterBucketUsed) {
            // Decrement the timer when water is placed
            if (waterPickupTimer > 0) {
                waterPickupTimer--;
            }

            //TODO max pickup try

            // Only try to pick up water if we've landed or stopped falling AND the timer has expired
            if ((client.player.isOnGround() || currentFallDistance < 0.5f) && waterPickupTimer <= 0) {
                if (!pickUpWater()) {
                    waterPickupTimer = 2;
                }
            }
        }
    }

    private void handleFallingPlayer() {
        // Don't continue if we're about to land on a safe surface
        if (isSafe()) {
            return;
        }

        // Update surface detection
        detectHighestSurface();

        // Check if we have a water bucket in the hotbar or find an alternative item
        Item placementItem = findSuitablePlacementItem();
        if (placementItem == null) {
            return;
        }

        int itemSlot = findItemSlot(placementItem);
        if (itemSlot == -1 || result.getType() != BlockHitResult.Type.BLOCK) {
            return;
        }

        // Calculate distance to ground
        double distanceToGround = client.player.getPos().y - result.getBlockPos().getY();

        // Only attempt placement when we're close enough to the ground but not too close
        if (distanceToGround > 1.0 && distanceToGround < DEFAULT_BLOCK_INTERACTION_RANGE) {
            isAttemptingPlacement = true;
            lastWaterPlacementTime = System.currentTimeMillis();

            // Save the current player state
            int slot = client.player.getInventory().getSelectedSlot();
            float yaw = client.player.getYaw();
            float pitch = client.player.getPitch();
            Vec3d pos = client.player.getPos();
            Vec3d vel = client.player.getVelocity();

            placeWaterBucket(itemSlot, vel, yaw, pitch, slot, pos);
        }
    }

    /**
     * Finds a suitable item for fall damage prevention
     */
    private Item findSuitablePlacementItem() {
        PlayerInventory inventory = client.player.getInventory();

        // Prioritize water bucket
        if (hasItemInHotbar(inventory, Items.WATER_BUCKET)) {
            return Items.WATER_BUCKET;
        }

        // Then check for the powder snow bucket
        if (hasItemInHotbar(inventory, Items.POWDER_SNOW_BUCKET)) {
            return Items.POWDER_SNOW_BUCKET;
        }

        // Then check for hay bale (reduces fall damage)
        if (hasItemInHotbar(inventory, Items.HAY_BLOCK)) {
            return Items.HAY_BLOCK;
        }

        return null;
    }

    /**
     * Places water bucket at the target position
     */
    private void placeWaterBucket(int itemSlot, Vec3d preVel, float preYaw, float prePitch, int preSlot, Vec3d prePos) {
        // Freeze horizontal movement temporarily
        client.player.setVelocity(0, preVel.y, 0);


        // Look at the block we're going to place water on
        BlockPos pos = result.getBlockPos();
        Vec3d target = pos.toCenterPos().offset(Direction.UP, 0.5);
        Player.lookAt(client.player, target, false);

        // Position player correctly for placement
        Vec3d placementPos = new Vec3d(
                target.getX(),
                client.player.getY(),
                target.getZ()
        );
        Player.setPosition(client.player, placementPos, false);

        // Select the water bucket and use it
        client.player.getInventory().setSelectedSlot(itemSlot);
        ActionResult actionResult = client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);

        if (actionResult.isAccepted()) {
            waterBucketUsed = true;
            lastYaw = preYaw;
            lastPitch = prePitch;
            lastSlot = preSlot;
            lastVelocity = preVel;
            waterPickupTimer = 3; // Initialize the timer when water is placed
            waterPos = pos.up().toBottomCenterPos();
        }

        // Restore player position and velocity
        Player.setPosition(client.player, prePos, false);
        Player.syncPosition(client.player);
        isAttemptingPlacement = false;
    }

    private boolean pickUpWater() {
        Player.lookAt(client.player, waterPos, false);
        ActionResult actionResult = client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);

        if (!actionResult.isAccepted()) {
            return false;
        }

        waterBucketUsed = false;

        // Restore previous states
        client.player.setYaw(lastYaw);
        client.player.setPitch(lastPitch);
        client.player.getInventory().setSelectedSlot(lastSlot);
        client.player.setVelocity(lastVelocity);
        return true;
    }

    private boolean isSafe() {
        BlockHitResult surfaceResult = detectHighestSurface();
        if (surfaceResult.getType() == BlockHitResult.Type.BLOCK) {
            BlockState blockState = client.world.getBlockState(surfaceResult.getBlockPos());

            // Check if the block we're landing on cancels fall damage
            return blockState.isIn(BlockTags.FALL_DAMAGE_RESETTING)
                    || blockState.isOf(Blocks.SLIME_BLOCK)
                    || blockState.isOf(Blocks.HONEY_BLOCK)
                    || (blockState.getFluidState().getFluid() == Fluids.WATER
                    || blockState.getFluidState().getFluid() == Fluids.FLOWING_WATER);
        }
        return false;
    }

    private BlockHitResult detectHighestSurface() {
        Vec3d[] checks = getChecks();

        int maxY = Integer.MIN_VALUE;
        BlockHitResult bestResult = new BlockHitResult(new Vec3d(0, 0, 0), null, BlockPos.ORIGIN, false);

        for (Vec3d check : checks) {
            RaycastContext raycastContext = new RaycastContext(
                    check,
                    check.subtract(0, DEFAULT_BLOCK_INTERACTION_RANGE, 0),
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

        this.result = bestResult;
        return bestResult;
    }

    @Override
    public void onDeactivate() {
        waterBucketUsed = false;
        isAttemptingPlacement = false;
        waterPickupTimer = 0;
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
