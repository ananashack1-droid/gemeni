package sky.client;

import lombok.Getter;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import sky.client.manager.ClientManager;
import sky.client.manager.Manager;
import sky.client.manager.SyncManager;
import sky.client.manager.accountManager.AccountManager;
import sky.client.manager.modulesManager.ChestStealerManager;
import sky.client.manager.proxyManager.ProxyManager;
import sky.client.manager.themeManager.StyleManager;
import sky.client.protect.NativeHelper;
import sky.client.modules.setting.BindBooleanSetting;
import sky.client.modules.setting.Setting;
import sky.client.events.Event;
import sky.client.events.impl.input.EventKey;
import sky.client.manager.commandManager.CommandManager;
import sky.client.manager.configManager.ConfigManager;
import sky.client.manager.dragManager.DragManager;
import sky.client.manager.dragManager.Dragging;
import sky.client.manager.friendManager.FriendManager;
import sky.client.manager.macroManager.MacroManager;
import sky.client.manager.notificationManager.NotificationManager;
import sky.client.manager.staffManager.StaffManager;
import sky.client.modules.Function;
import sky.client.modules.FunctionManager;
import sky.client.modules.misc.UnHook;
import sky.client.screens.dropdown.ClickGUI;
import sky.client.manager.fontManager.FontUtils;
import sky.client.util.color.ColorUtil;
import sky.client.util.player.AudioUtil;
import sky.client.util.render.providers.ResourceProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
import java.util.Objects;

@SuppressWarnings("All")
public final class  Main implements ModInitializer {
    private static Main instance;
    private final File directory;
    private final File directoryAddon;
    public final String name = "GPT Сlient";
    @Getter
    boolean initialized;

    public static Main getInstance() {
        return instance;
    }

    public Main() {
        instance = this;
        this.directory = new File(Objects.requireNonNull(MinecraftClient.getInstance().runDirectory), "files");
        this.directoryAddon = new File(Objects.requireNonNull(MinecraftClient.getInstance().runDirectory), "files/modules");
    }

    private void setupProtection() {
        NativeHelper.setProfile();
    }

    @Override
    public void onInitialize() {
        setupProtection();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                shutDown();
            } catch (Exception ignored) {}
        }));
    }

    public void init() {
        ensureDirectoryExists();
        try {
            Manager.SYNC_MANAGER = new SyncManager();
            Manager.FUNCTION_MANAGER = new FunctionManager();
            Manager.STYLE_MANAGER = new StyleManager();
            Manager.STYLE_MANAGER.init();
            Manager.ACCOUNT_MANAGER = new AccountManager();
            Manager.ACCOUNT_MANAGER.init();
            Manager.FONT_MANAGER = new FontUtils();
            Manager.FONT_MANAGER.init();
            Manager.COMMAND_MANAGER = new CommandManager();
            Manager.DRAG_MANAGER = new DragManager();
            Manager.DRAG_MANAGER.init();
            Manager.MACROS_MANAGER = new MacroManager();
            Manager.MACROS_MANAGER.init();
            Manager.FRIEND_MANAGER = new FriendManager();
            Manager.FRIEND_MANAGER.init();
            Manager.STAFF_MANAGER = new StaffManager();
            Manager.STAFF_MANAGER.init();
            Manager.NOTIFICATION_MANAGER = new NotificationManager();
            Manager.CHESTSTEALER_MANAGER = new ChestStealerManager();
            Manager.PROXY_MANAGER = new ProxyManager();
            Manager.PROXY_MANAGER.init();

            Manager.CONFIG_MANAGER = new ConfigManager();
            Manager.CONFIG_MANAGER.init();

            ColorUtil.loadImage(ResourceProvider.color_image);

//			if (Manager.FUNCTION_MANAGER.clientSounds.check.get("Вход в клиент")) {
//				AudioUtil.playSound("join.wav");
//			}
            initialized = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void keyPress(int key) {
        int processedKey = key >= 0 ? key : -(100 + key + 2);
        Event.call(new EventKey(processedKey));

        if (key == Manager.FUNCTION_MANAGER.unHook.unHookKey.getKey() && ClientManager.legitMode) {
            UnHook.functionsToBack.forEach(function -> function.setState(true));
            File folder = new File("C:\\" + Main.getInstance().name.replaceAll("\\s+", ""));
            if (folder.exists()) {
                try {
                    Path folderPathObj = folder.toPath();
                    DosFileAttributeView attributes = Files.getFileAttributeView(folderPathObj, DosFileAttributeView.class);
                    attributes.setHidden(false);
                } catch (IOException ignored) {
                }
            }
            UnHook.functionsToBack.clear();
            ClientManager.legitMode = false;
        }

        if (!ClientManager.legitMode) {
            for (Function module : Manager.FUNCTION_MANAGER.getFunctions()) {
                if (module.bind == processedKey) {
                    module.toggle();
                }
                for (Setting setting : module.getSettings()) {
                    if (setting instanceof BindBooleanSetting bindSetting) {
                        bindSetting.onKeyPress(key, true);
                    }
                }
            }

            if (key == Manager.FUNCTION_MANAGER.clickGUI.getBindCode()) {
                MinecraftClient.getInstance().setScreen(new ClickGUI());
            }
            if (Manager.MACROS_MANAGER != null) {
                Manager.MACROS_MANAGER.onKeyPressed(key);
            }
        }
    }

    public void shutDown() {
        Manager.DRAG_MANAGER.save();
        Manager.ACCOUNT_MANAGER.saveAccounts();
        Manager.ACCOUNT_MANAGER.saveLastAlt();
        Manager.CONFIG_MANAGER.saveConfiguration("autocfg");
        System.out.println("[-] Client shutdown");
    }
    public static void openURL(String url) {
        try {
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", url});
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", url});
            } else {
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Dragging createDrag(Function function, String name, float x, float y) {
        DragManager.draggables.put(name, new Dragging(function, name, x, y));
        return DragManager.draggables.get(name);
    }
    private void ensureDirectoryExists() {
        if (!directory.exists() && !directory.mkdirs()) {
            System.err.println("Failed to create directory: " + directory.getAbsolutePath());
        }
        if (!directoryAddon.exists() && !directoryAddon.mkdirs()) {
            System.err.println("Failed to create directory: " + directoryAddon.getAbsolutePath());
        }
    }
}