package sky.client.bot.network;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkPhase;
import net.minecraft.network.listener.ClientLoginPacketListener;
import net.minecraft.network.packet.s2c.login.*;
import net.minecraft.network.packet.c2s.login.LoginQueryResponseC2SPacket;
import net.minecraft.network.packet.s2c.common.CookieRequestS2CPacket;
import net.minecraft.network.DisconnectionInfo;
import sky.client.bot.BotInstance;

public class BotLoginHandler implements ClientLoginPacketListener {
    private final ClientConnection connection;
    private final BotInstance bot;

    public BotLoginHandler(ClientConnection connection, BotInstance bot) {
        this.connection = connection;
        this.bot = bot;
    }

    @Override
    public void onSuccess(LoginSuccessS2CPacket packet) {
        // Используем прямой доступ к стейту через фазу
        this.connection.transitionInbound(NetworkPhase.PLAY.getNetworkState(), new BotPlayHandler(connection, bot));
    }

    @Override public void onCompression(LoginCompressionS2CPacket p) { connection.setCompressionThreshold(p.getCompressionThreshold(), false); }
    @Override public void onHello(LoginHelloS2CPacket p) {}
    @Override public void onQueryRequest(LoginQueryRequestS2CPacket p) { connection.send(new LoginQueryResponseC2SPacket(p.queryId(), null)); }
    @Override public void onDisconnect(LoginDisconnectS2CPacket p) {}
    @Override public void onDisconnected(DisconnectionInfo info) {}
    @Override public boolean isConnectionOpen() { return connection.isOpen(); }
    @Override public void onCookieRequest(CookieRequestS2CPacket p) {}
}