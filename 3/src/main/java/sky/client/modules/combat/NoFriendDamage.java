package sky.client.modules.combat;


import sky.client.events.Event;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;

@FunctionAnnotation(name = "NoFriendDamage", keywords = {"NFD","FriendDamage"}, type = Type.Combat, desc = "Отключает урон по друзьям")
public class NoFriendDamage extends Function {
    public NoFriendDamage() {
        addSettings();
    }
    @Override
    public void onEvent(Event event) {
    }
}
