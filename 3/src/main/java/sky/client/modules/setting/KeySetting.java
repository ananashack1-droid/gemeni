package sky.client.modules.setting;

import java.util.function.Supplier;

public class KeySetting extends Setting {
    private int keyCode;
    private boolean binding;

    public KeySetting(String name, int defaultKey) {
        this.name = name;
        this.keyCode = defaultKey;
        this.binding = false;
        setVisible(() -> true);
    }

    public KeySetting(String name, int defaultKey, Supplier<Boolean> visible) {
        this.name = name;
        this.keyCode = defaultKey;
        this.binding = false;
        setVisible(visible);
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    public boolean isBinding() {
        return binding;
    }

    public void setBinding(boolean binding) {
        this.binding = binding;
    }

    public String getName() {
        return name;
    }
}