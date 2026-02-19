package sky.client.modules.misc;

import sky.client.modules.setting.BooleanSetting;
import sky.client.modules.setting.TextSetting;
import sky.client.events.Event;
import sky.client.manager.Manager;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;

@FunctionAnnotation(name = "NameProtect", desc = "", type = Type.Misc)
public class NameProtect extends Function {
    public final TextSetting text = new TextSetting("Ник","Sky Client");
    public final BooleanSetting friend = new BooleanSetting("Скрывать друзей",true);

    public NameProtect() {
        addSettings(text,friend);
    }
    public String getCustomName() {
        return Manager.FUNCTION_MANAGER.nameProtect.state ? text.getValue().replaceAll("&", "\u00a7") : mc.getGameProfile().getName();
    }
    public String getProtectedName(String originalName) {
        if (!Manager.FUNCTION_MANAGER.nameProtect.state) return originalName;

        if (isSelf(originalName)) {
            return applyFormatting(text.getValue());
        }

        if (friend.get() && Manager.FRIEND_MANAGER.isFriend(originalName)) {
            return applyFormatting(text.getValue());
        }

        return originalName;
    }
    private String applyFormatting(String name) {
        return name.replace('&', '§');
    }

    private boolean isSelf(String name) {
        return name.equals(mc.getSession().getUsername());
    }
    @Override
    public void onEvent(Event event) {

    }
}
