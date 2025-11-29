package Networking;

import java.io.IOException;
import java.net.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class UDPHandler implements NetworkManager {
    private DatagramSocket socket;
    private boolean isRunning;
    private boolean simulatePacketLoss = false;
    private int expectedSequenceNumber = 0;
    private java.util.Random random = new java.util.Random();

    @Override
    public void setSimulatePacketLoss(boolean simulate) {
        this.simulatePacketLoss = simulate;
    }

    @Override
    public void startServer(int port, Function<String, String> onMessageReceived) {
        new Thread(() -> {
            try {
                socket = new DatagramSocket(port);
                isRunning = true;
                expectedSequenceNumber = 0; // Reset on start

                byte[] buffer = new byte[1024];
                while (isRunning) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String received = new String(packet.getData(), 0, packet.getLength());

                    // Packet Loss Simulation
                    if (simulatePacketLoss && random.nextInt(100) < 50) { // 50% chance to drop
                        System.out.println("Server: Packet Dropped! (Simulated)");
                        continue; // Drop packet
                    }

                    String response;
                    // Parse Sequence Number
                    if (received.startsWith("SEQ:")) {
                        try {
                            int splitIndex = received.indexOf("|");
                            if (splitIndex != -1) {
                                int seqNum = Integer.parseInt(received.substring(4, splitIndex));
                                String content = received.substring(splitIndex + 1);

                                if (seqNum > expectedSequenceNumber) {
                                    // Gap Detected
                                    response = "NACK:" + expectedSequenceNumber;
                                    // We do NOT update expectedSequenceNumber because we still need the missing one
                                    // But for this simple simulation, we might want to just notify and move on,
                                    // or strictly wait. The requirement says "notify ... that i have not received".
                                    // Sending NACK is the notification.
                                    // To allow the chat to continue, we might want to accept it anyway or just
                                    // NACK.
                                    // Let's just NACK and NOT process the message content to simulate "waiting" or
                                    // "alerting".
                                    // Actually, if we NACK, the client knows.
                                    // Let's also process the message so the chat doesn't get stuck,
                                    // BUT the NACK serves as the "I missed something before this" notification.
                                    // However, standard ARQ would discard out-of-order.
                                    // Let's stick to the user request: "send the client that i have not recieved
                                    // the message".
                                    // So we send NACK.
                                } else {
                                    if (seqNum == expectedSequenceNumber) {
                                        expectedSequenceNumber++;
                                    }
                                    // Process normally
                                    response = onMessageReceived.apply(content);
                                }
                            } else {
                                response = onMessageReceived.apply(received);
                            }
                        } catch (NumberFormatException e) {
                            response = onMessageReceived.apply(received);
                        }
                    } else {
                        response = onMessageReceived.apply(received);
                    }

                    byte[] responseData = response.getBytes();

                    DatagramPacket responsePacket = new DatagramPacket(
                            responseData, responseData.length, packet.getAddress(), packet.getPort());
                    socket.send(responsePacket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void sendMessage(String host, int port, String message, Consumer<String> onResponseReceived) {
        new Thread(() -> {
            try {
                DatagramSocket clientSocket = new DatagramSocket();
                byte[] buffer = message.getBytes();
                InetAddress address = InetAddress.getByName(host);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
                clientSocket.send(packet);

                // Wait for response
                byte[] responseBuffer = new byte[1024];
                DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
                clientSocket.setSoTimeout(5000); // 5 second timeout
                try {
                    clientSocket.receive(responsePacket);
                    String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
                    onResponseReceived.accept(response);
                } catch (SocketTimeoutException e) {
                    onResponseReceived.accept("Error: Server timeout");
                }

                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void stopServer() {
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
