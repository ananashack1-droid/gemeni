package sky.client.manager.commandManager;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.command.CommandSource;
import sky.client.manager.IMinecraft;
import sky.client.manager.commandManager.impl.*;
import sky.client.manager.commandManager.impl.*;

import java.util.ArrayList;
import java.util.List;

public class CommandManager implements IMinecraft {
    private String prefix = ".";
    private final CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();
    private final CommandSource source = new ClientCommandSource(null, mc);
    private final List<Command> commands = new ArrayList<>();

    public CommandManager() {
        register(new FriendCommand());
        register(new GpsCommand());
        register(new WayPointCommand());
        register(new UnHookCommand());
        register(new ConfigCommand());
        register(new ChestStealerCommand());
        register(new StaffCommand());
        register(new BlockESPCommand());
        register(new BindCommand());
        register(new DragCommand());
        register(new MacroCommand());
        register(new PanicCommand());
        register(new ParseCommand());
        register(new VclipCommand());
        register(new HclipCommand());
        //register(new RotCommand());

    }

    public void register(Command command) {
        if (command == null) return;
        command.register(dispatcher);
        this.commands.add(command);
    }

    public final CommandDispatcher getDispatcher() {
        return dispatcher;
    }
    public final CommandSource getSource() {
        return source;
    }
    public final String getPrefix() {
        return prefix;
    }
}