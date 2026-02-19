package sky.client.manager;

import sky.client.manager.accountManager.AccountManager;
import sky.client.manager.commandManager.CommandManager;
import sky.client.manager.configManager.ConfigManager;
import sky.client.manager.dragManager.DragManager;
import sky.client.manager.friendManager.FriendManager;
import sky.client.manager.macroManager.MacroManager;
import sky.client.manager.modulesManager.ChestStealerManager;
import sky.client.manager.notificationManager.NotificationManager;
import sky.client.manager.proxyManager.ProxyManager;
import sky.client.manager.staffManager.StaffManager;
import sky.client.manager.themeManager.StyleManager;
import sky.client.modules.FunctionManager;
import sky.client.modules.combat.rotation.RotationController;
import sky.client.protect.UserProfile;
import sky.client.manager.fontManager.FontUtils;

public class Manager {
    public static final RotationController ROTATION = RotationController.get();
    public static UserProfile USER_PROFILE;
    public static FunctionManager FUNCTION_MANAGER;
    public static StyleManager STYLE_MANAGER;
    public static NotificationManager NOTIFICATION_MANAGER;
    public static FriendManager FRIEND_MANAGER;
    public static ConfigManager CONFIG_MANAGER;
    public static MacroManager MACROS_MANAGER;
    public static StaffManager STAFF_MANAGER;
    public static CommandManager COMMAND_MANAGER;
    public static DragManager DRAG_MANAGER;
    public static SyncManager SYNC_MANAGER;
    public static FontUtils FONT_MANAGER;
    public static AccountManager ACCOUNT_MANAGER;
    public static ChestStealerManager CHESTSTEALER_MANAGER;
    public static ProxyManager PROXY_MANAGER;
    public static sky.client.bot.BotManager BOT_MANAGER = new sky.client.bot.BotManager();
}

