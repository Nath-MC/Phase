package com.purpynaxx.phase.commands.pathfinding;

import com.purpynaxx.phase.commands.Command;
import com.purpynaxx.phase.helpers.chat.ChatHelper;
import com.purpynaxx.phase.helpers.pathfinding.PathExecutor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;

@SuppressWarnings("unused")
public class GoTo implements Command {

    @Override
    public Text getDescription() {
        return Text.literal("Finds a path and walks to the specified coordinates.");
    }

    @Override
    public List<Class<?>> getArgumentsType() {
        return List.of(Integer.class, Integer.class, Integer.class);
    }

    @Override
    public String getName() {
        return "goto";
    }

    @Override
    public List<String> getArgumentsName() {
        return List.of("x", "y", "z");
    }

    @Override
    public List<Boolean> getArgumentsOptional() {
        return List.of(false, false, false);
    }

    @Override
    public void execute(List<Object> args) {
        BlockPos start = MinecraftClient.getInstance().player.getBlockPos();
        BlockPos end = new BlockPos((int) args.get(0), (int) args.get(1), (int) args.get(2));
        ChatHelper.send(String.format("§aFinding path to %d, %d, %d...", end.getX(), end.getY(), end.getZ()));
        PathExecutor.findAndExecutePath(start, end);
    }

}
