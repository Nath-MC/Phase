package com.purpynaxx.phase.commands.pathfinding;

import com.purpynaxx.phase.commands.Command;
import com.purpynaxx.phase.helpers.pathfinding.PathExecutor;
import net.minecraft.text.Text;

import java.util.List;

@SuppressWarnings("unused")
public class Stop implements Command {

    @Override
    public Text getDescription() {
        return Text.literal("Stop current pathfinding process(es)");
    }

    @Override
    public List<Class<?>> getArgumentsType() {
        return List.of();
    }

    @Override
    public String getName() {
        return "stop";
    }

    @Override
    public List<String> getArgumentsName() {
        return List.of();
    }

    @Override
    public List<Boolean> getArgumentsOptional() {
        return List.of();
    }

    @Override
    public void execute(List<Object> args) {
        PathExecutor.stop();
    }

}
