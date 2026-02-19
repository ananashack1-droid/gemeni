package sky.client.bot;

import net.minecraft.network.ClientConnection;

public class BotInstance {
    private final String name;
    private final ClientConnection connection;

    public BotInstance(String name, ClientConnection connection) {
        this.name = name;
        this.connection = connection;
    }

    public String getName() { return name; }
    public ClientConnection getConnection() { return connection; }

    public void disconnect() {
        if (connection.isOpen()) {
            connection.disconnect(net.minecraft.text.Text.literal("Бот отключен"));
        }
    }
}