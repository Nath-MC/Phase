package com.purpynaxx.phase.commands;

import com.purpynaxx.phase.helpers.chat.ChatHelper;
import org.jetbrains.annotations.UnmodifiableView;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.*;

import static com.purpynaxx.phase.Phase.IS_DEV_ENVIRONMENT;

public class Commands {

    public static final char PREFIX = '.';
    private static final Logger LOGGER = LoggerFactory.getLogger("Phase/Commands");
    private static final Commands INSTANCE = new Commands();
    private final Map<String, Command> commands = new HashMap<>();
    private boolean registered;

    private Commands() {}

    public static Commands getInstance() {
        return INSTANCE;
    }

    public void init() {
        if (registered)
            throw new IllegalStateException("Commands have already been registered !");

        Reflections reflections = new Reflections("com.purpynaxx.phase.commands");
        Set<Class<? extends Command>> commandClasses = reflections.getSubTypesOf(Command.class);

        if (!commandClasses.isEmpty()) {

            for (Class<? extends Command> cls : commandClasses) {

                try {
                    Constructor<? extends Command> constructor = cls.getConstructor();
                    constructor.setAccessible(true);

                    Command command = constructor.newInstance();

                    String name = command.getName();
                    commands.put(name, command);

                    if (IS_DEV_ENVIRONMENT) {
                        LOGGER.info("{} has been registered.", Character.toUpperCase(name.charAt(0)) + name.substring(1));
                    }

                } catch (Exception e) {
                    String message = String.format("Failed to instantiate command: %s", cls.getSimpleName());
                    LOGGER.error(message, e);
                }
            }
        } else {
            LOGGER.warn("No commands have been discovered");
        }

        registered = true;
    }

    /**
     * Commands entry point.
     * <p>
     * This method is called whenever the client try to send a message.
     *
     * @param message The message, might be a command
     * @return whether the message should be sent (false if {@code message} is considered as a command)
     * @see net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents#ALLOW_CHAT
     */
    public boolean onMessage(String message) {
        if (message.startsWith(String.valueOf(PREFIX)) && message.length() > 1 && message.charAt(1) != '/') {
            parse(message);
            return false;
        } else return true;
    }

    private void parse(String message) {
        message = message.substring(1);
        String[] parts = message.split(" ", -1);
        String commandName = parts[0].toLowerCase();
        List<String> stringArgs = new ArrayList<>(Arrays.asList(parts).subList(1, parts.length));

        Optional<Command> commandOptional = Optional.ofNullable(commands.get(commandName));

        if (commandOptional.isPresent()) {
            Command cmd = commandOptional.get();
            List<Class<?>> expectedTypes = cmd.getArgumentsType();
            List<Boolean> optionalFlags = cmd.getArgumentsOptional();
            int requiredArgs = (int) optionalFlags.stream().filter(o -> !o).count();
            int maxArgs = expectedTypes.size();


            if (stringArgs.size() < requiredArgs) {
                ChatHelper.send(String.format("§cError: Not enough arguments for command '%s'.§r\nUsage: %s", commandName, cmd.getUsage()));
                return;
            }

            if (stringArgs.size() > maxArgs) {
                ChatHelper.send(String.format("§cError: Too many arguments for command '%s'.§r\nUsage: %s", commandName, cmd.getUsage()));
                return;
            }

            List<Object> parsedArgs = new ArrayList<>();
            for (int i = 0; i < stringArgs.size(); i++) {
                String arg = stringArgs.get(i);
                Class<?> type = expectedTypes.get(i);
                try {
                    if (type == String.class) {
                        parsedArgs.add(arg);
                    } else if (type == Integer.class) {
                        parsedArgs.add(Integer.parseInt(arg));
                    } else if (type == Double.class) {
                        parsedArgs.add(Double.parseDouble(arg));
                    } else if (type == Float.class) {
                        parsedArgs.add(Float.parseFloat(arg));
                    } else if (type == Boolean.class) {
                        parsedArgs.add(Boolean.parseBoolean(arg));
                    } else if (type == Long.class) {
                        parsedArgs.add(Long.parseLong(arg));
                    } else {
                        ChatHelper.send("§c An error occurred, please refer to the logs.");
                        LOGGER.error("Unsupported argument type '{}' for command '{}'.§r", type.getSimpleName(), commandName);
                        return;
                    }
                } catch (NumberFormatException e) {
                    ChatHelper.send(String.format("§cError: Invalid argument format. '%s' cannot be converted to a %s.§r\nUsage: %s", arg, type.getSimpleName(), cmd.getUsage()));
                    return;
                }
            }

            cmd.execute(parsedArgs);

        } else {
            ChatHelper.send("§cUnknown command, try \".help\".§r");
        }
    }

    public Optional<Command> get(String name) {
        return Optional.ofNullable(commands.get(name));
    }

    @UnmodifiableView
    public Collection<Command> getAll() {
        return Collections.unmodifiableCollection(commands.values());
    }

}
