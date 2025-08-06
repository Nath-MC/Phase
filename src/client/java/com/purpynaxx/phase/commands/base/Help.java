package com.purpynaxx.phase.commands.base;

import com.purpynaxx.phase.commands.Command;
import com.purpynaxx.phase.commands.Commands;
import com.purpynaxx.phase.helpers.chat.ChatHelper;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
public class Help implements Command {

    private static final Commands manager = Commands.getInstance();
    private static final Text description = Text.of("Displays all commands, and their purpose");

    @Override
    public Text getDescription() {
        return description;
    }

    @Override
    public List<Class<?>> getArgumentsType() {
        return List.of(String.class);
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public List<String> getArgumentsName() {
        return List.of("command");
    }

    @Override
    public List<Boolean> getArgumentsOptional() {
        return List.of(true);
    }

    @Override
    public void execute(List<Object> args) {

        if (!args.isEmpty()) {
            Optional<Command> optional = manager.get((String) args.getFirst());

            if (optional.isPresent()) {
                Command command = optional.get();
                ChatHelper.send(String.format("§l%s§r\n§7>§r %s - §o%s", command.getName(), command.getUsage(), command.getDescription().getString()));
                return;
            }
        }

        StringBuilder stringBuilder = new StringBuilder("§lAvailable Commands:§r");
        for (Command command : manager.getAll()) {
            stringBuilder.append("\n§7> §r")
                         .append(command.getUsage())
                         .append(" - §o")
                         .append(command.getDescription().getString())
                         .append("\n");
        }

        String message = stringBuilder.toString();
        ChatHelper.send(message);

    }

}
