package com.purpynaxx.phase.helpers.pathfinding.nodes;

import com.purpynaxx.phase.helpers.pathfinding.Node;
import com.purpynaxx.phase.helpers.player.PlayerHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class MovementNode extends Node {

    public MovementNode(BlockPos pos, int gCost, int hCost, @Nullable Node previousNode) {
        super(pos, gCost, hCost, previousNode);
    }

    @Override
    public void execute() {
        PlayerHelper.lookAtNoPitch(pos.toBottomCenterPos(), PlayerHelper.Side.CLIENT, 0);

        KeyBinding forwardKey = client.options.forwardKey;
        KeyBinding sprintKey = client.options.sprintKey;
        if (!forwardKey.isPressed())
            forwardKey.setPressed(true);

        if (!sprintKey.isPressed())
            sprintKey.setPressed(true);
    }

    @Override
    public boolean isDone() {
        Vec3d playerPos = client.player.getPos().offset(Direction.UP, 0.5);
        return playerPos.isInRange(pos.toCenterPos(), 0.5);
    }

    @Override
    public int getCost() {
        return 1;
    }

}
