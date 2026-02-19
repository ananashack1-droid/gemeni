package sky.client.modules.misc;

import org.lwjgl.glfw.GLFW;
import sky.client.modules.setting.BindSetting;
import sky.client.events.Event;
import sky.client.manager.Manager;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import sky.client.screens.unhook.UnHookScreen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@FunctionAnnotation(name = "UnHook",keywords = {"SelfDestruct"}, desc = "Отключение чита для прохождения проверки", type = Type.Misc)
public class UnHook extends Function {
    public static BindSetting unHookKey = new BindSetting("Кнопка возврата", GLFW.GLFW_KEY_INSERT);
    public static final List<Function> functionsToBack = new CopyOnWriteArrayList<>();
    public UnHook() {
        addSettings(unHookKey);
    }


    @Override
    public void onEvent(Event event) {
    }

    @Override
    protected void onEnable() {
        mc.setScreen(new UnHookScreen());
        super.onEnable();
    }

    public void onUnhook() {
        functionsToBack.clear();
        for (int i = 0; i < Manager.FUNCTION_MANAGER.getFunctions().size(); i++) {
            Function function = Manager.FUNCTION_MANAGER.getFunctions().get(i);
            if (function.state && function != this) {
                functionsToBack.add(function);
                function.setState(false);
            }
        }
        File folder = new File("C:\\ExosWare");

        if (folder.exists()) {
            try {
                Path folderPathObj = folder.toPath();
                DosFileAttributeView attributes = Files.getFileAttributeView(folderPathObj, DosFileAttributeView.class);
                attributes.setHidden(true);
            } catch (IOException e) {
            }

        }
        toggle();
    }
}
