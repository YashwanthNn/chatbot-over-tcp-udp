package Networking;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class TCPHandler implements NetworkManager {
    private ServerSocket serverSocket;
    private boolean isRunning;

    @Override
    public void startServer(int port, Function<String, String> onMessageReceived) {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                isRunning = true;
                // Log server start (we can't easily log to GUI here without another callback,
                // but the Function return value is for the client. We'll assume the Function
                // handles logging if needed,
                // or we just process silently and return the response).
                // Actually, the previous implementation used Consumer<String> which was used
                // for logging.
                // To keep it simple, we will assume the Function does the logging AND returns
                // the response.

                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();

                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        String response = onMessageReceived.apply(inputLine);
                        out.println(response);
                    }
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void sendMessage(String host, int port, String message, Consumer<String> onResponseReceived) {
        new Thread(() -> {
            try (Socket socket = new Socket(host, port);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.println(message);
                String response = in.readLine();
                if (response != null) {
                    onResponseReceived.accept(response);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setSimulatePacketLoss(boolean simulate) {
        // TCP handles packet loss internally, so we don't simulate it at this layer
        // or we could, but the user asked for UDP specifically.
    }
}
