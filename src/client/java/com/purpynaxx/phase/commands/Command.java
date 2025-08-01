package com.purpynaxx.phase.commands;

import net.minecraft.text.Text;

import java.util.List;

public interface Command {

    Text getDescription();

    List<Class<?>> getArgumentsType();

    /**
     * Generates a usage string for the command based on its arguments.
     * e.g., .command {@literal <required_arg>} [optional_arg]
     *
     * @return The formatted usage string.
     */
    default String getUsage() {
        List<String> args = getArgumentsName();
        List<Boolean> optionals = getArgumentsOptional();
        StringBuilder stringBuilder = new StringBuilder(Commands.PREFIX + getName());

        for (int i = 0; i < args.size(); i++) {
            String argument = args.get(i);
            boolean optional = optionals.get(i);
            String formatted = String.format(optional ? " [%s]" : " <%s>", argument);
            stringBuilder.append(formatted);
        }

        return stringBuilder.toString();
    }

    String getName();

    List<String> getArgumentsName();

    List<Boolean> getArgumentsOptional();

    /**
     * Executes the command's logic.
     *
     * @param args A list of parsed and type-converted arguments. The objects
     *             will match the types defined in getArgumentsType().
     */
    void execute(List<Object> args);

}
