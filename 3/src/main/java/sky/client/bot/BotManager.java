package sky.client.bot;

import java.util.ArrayList;
import java.util.List;

public class BotManager {
    public List<BotInstance> bots = new ArrayList<>();

    public void addBot(BotInstance bot) {
        bots.add(bot);
    }

    public void removeBot(String name) {
        bots.removeIf(bot -> {
            if (bot.getName().equalsIgnoreCase(name)) {
                bot.disconnect();
                return true;
            }
            return false;
        });
    }
}