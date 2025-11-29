package Networking;

import java.util.function.Consumer;
import java.util.function.Function;

public interface NetworkManager {
    void startServer(int port, Function<String, String> onMessageReceived);

    void sendMessage(String host, int port, String message, Consumer<String> onResponseReceived);

    void stopServer();

    void setSimulatePacketLoss(boolean simulate);
}
