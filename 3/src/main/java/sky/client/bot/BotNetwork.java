package sky.client.bot;

import io.netty.channel.ChannelFuture;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.state.LoginStates; // Важно!
import net.minecraft.network.packet.c2s.handshake.ConnectionIntent;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import sky.client.bot.network.BotLoginHandler;
import sky.client.manager.Manager;

import java.net.InetSocketAddress;
import java.util.UUID;

public class BotNetwork {
    public static void spawnBot(String name, String ip, int port) {
        new Thread(() -> {
            try {
                InetSocketAddress address = new InetSocketAddress(ip, port);
                ClientConnection connection = new ClientConnection(NetworkSide.CLIENTBOUND);
                ChannelFuture future = ClientConnection.connect(address, false, connection);
                future.awaitUninterruptibly();

                BotInstance bot = new BotInstance(name, connection);
                Manager.BOT_MANAGER.addBot(bot);

                // Передаем LoginStates.S2C
                connection.transitionInbound(LoginStates.S2C, new BotLoginHandler(connection, bot));

                connection.send(new HandshakeC2SPacket(769, ip, port, ConnectionIntent.LOGIN));
                connection.send(new LoginHelloC2SPacket(name, UUID.randomUUID()));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "BotThread-" + name).start();
    }
}