package GUI;

import Networking.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Random;

public class MainFrame extends JFrame {

    private JTextArea clientChatArea; // New Client Chat Area
    private JTextArea serverLog;
    private JTextField clientInput;
    private JButton sendButton;
    private JButton connectButton;
    private JButton disconnectButton; // New Disconnect Button
    private JCheckBox packetLossCheckBox; // New Packet Loss Checkbox
    private JTextArea packetInfoPanel; // New Packet Info Panel
    private JLabel appInfoLabel; // New Application Info Label
    private JComboBox<String> protocolSelector;
    private JPanel animationPanel;

    private NetworkManager networkManager;
    private final int PORT = 12345;
    private final String HOST = "localhost";

    // Animation state
    private Timer animationTimer;
    private int packetX = 50;
    private boolean isAnimating = false;
    private boolean isReturnTrip = false;
    private String currentProtocol = "TCP";

    // Handshake State
    private boolean isConnected = false;
    // 0: None, 1: SYN, 2: SYN-ACK, 3: ACK, 4: Connected
    // 5: FIN (C->S), 6: ACK (S->C), 7: FIN (S->C), 8: ACK (C->S) -> Disconnected
    private int handshakeStep = 0;

    // Packet Loss State
    private boolean packetLost = false;
    private boolean isRetransmitting = false; // New flag for retransmission
    private Random random = new Random();
    private int clientSequenceNumber = 0; // New Sequence Number State

    public MainFrame() {
        setTitle("Chat Bot Over TCP/UDP - Advanced Concepts");
        setSize(1200, 850); // Increased height for App Info
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top Panel: Protocol Selection, Controls, and App Info
        JPanel topContainer = new JPanel(new BorderLayout());

        JPanel controlsPanel = new JPanel();
        controlsPanel.add(new JLabel("Select Protocol:"));
        protocolSelector = new JComboBox<>(new String[] { "TCP", "UDP" });
        protocolSelector.addActionListener(e -> switchProtocol((String) protocolSelector.getSelectedItem()));
        controlsPanel.add(protocolSelector);

        connectButton = new JButton("Connect");
        connectButton.addActionListener(this::initiateHandshake);
        controlsPanel.add(connectButton);

        disconnectButton = new JButton("Disconnect");
        disconnectButton.addActionListener(this::initiateTeardown);
        disconnectButton.setEnabled(false);
        controlsPanel.add(disconnectButton);

        packetLossCheckBox = new JCheckBox("Simulate Packet Loss");
        packetLossCheckBox.setVisible(true); // Always visible now
        packetLossCheckBox.addActionListener(e -> {
            if (networkManager != null) {
                networkManager.setSimulatePacketLoss(packetLossCheckBox.isSelected());
            }
        });
        controlsPanel.add(packetLossCheckBox);

        topContainer.add(controlsPanel, BorderLayout.NORTH);

        // Application Info Panel
        JPanel appInfoPanel = new JPanel();
        appInfoPanel.setBorder(BorderFactory.createTitledBorder("Application & Protocol Info"));
        appInfoLabel = new JLabel("TCP: Reliable, Ordered. Used for: Web (HTTP), Email (SMTP), File Transfer (FTP).");
        appInfoLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        appInfoPanel.add(appInfoLabel);
        topContainer.add(appInfoPanel, BorderLayout.SOUTH);

        add(topContainer, BorderLayout.NORTH);

        // Main Split: Animation (Top) vs Content (Bottom)
        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // Animation Panel
        animationPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());

                // Draw Nodes
                g.setColor(Color.BLUE);
                g.fillOval(50, getHeight() / 2 - 25, 50, 50); // Client
                g.setColor(Color.BLACK);
                g.drawString("Client", 55, getHeight() / 2 + 40);

                g.setColor(Color.RED);
                g.fillOval(getWidth() - 100, getHeight() / 2 - 25, 50, 50); // Server
                g.setColor(Color.BLACK);
                g.drawString("Server", getWidth() - 95, getHeight() / 2 + 40);

                // Draw Connection Line for TCP (Only if connected)
                if ("TCP".equals(currentProtocol) && isConnected) {
                    g.setColor(Color.GREEN);
                    g.drawLine(100, getHeight() / 2, getWidth() - 100, getHeight() / 2);
                    g.drawString("TCP Connection Established", getWidth() / 2 - 80, getHeight() / 2 - 10);
                }

                // Draw Packet
                if (isAnimating && !packetLost) {
                    if (handshakeStep > 0 && handshakeStep < 4) {
                        g.setColor(Color.YELLOW); // Handshake packet
                        String label = "";
                        if (handshakeStep == 1)
                            label = "SYN";
                        else if (handshakeStep == 2)
                            label = "SYN-ACK";
                        else if (handshakeStep == 3)
                            label = "ACK";
                        g.drawString(label, packetX, getHeight() / 2 - 15);
                    } else if (handshakeStep >= 5) {
                        g.setColor(Color.MAGENTA); // Teardown packet
                        String label = "";
                        if (handshakeStep == 5)
                            label = "FIN";
                        else if (handshakeStep == 6)
                            label = "ACK";
                        else if (handshakeStep == 7)
                            label = "FIN";
                        else if (handshakeStep == 8)
                            label = "ACK";
                        g.drawString(label, packetX, getHeight() / 2 - 15);
                    } else if (isReturnTrip) {
                        g.setColor(Color.BLUE); // Return packet color
                    } else {
                        g.setColor(Color.ORANGE); // Send packet color
                        if (isRetransmitting) {
                            g.drawString("Retrying...", packetX, getHeight() / 2 - 25);
                        }
                    }
                    g.fillOval(packetX, getHeight() / 2 - 10, 20, 20);
                } else if (packetLost && isAnimating) {
                    g.setColor(Color.RED);
                    g.drawString("X (Loss)", packetX, getHeight() / 2);
                }
            }
        };
        animationPanel.setPreferredSize(new Dimension(800, 200));
        mainSplit.setTopComponent(animationPanel);

        // Content Split: Client Chat (Left) vs Technical Logs (Right)
        JSplitPane contentSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // Client Chat Area
        clientChatArea = new JTextArea();
        clientChatArea.setEditable(false);
        clientChatArea.setBorder(BorderFactory.createTitledBorder("Client Chat (User View)"));
        clientChatArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        contentSplit.setLeftComponent(new JScrollPane(clientChatArea));

        // Technical Logs Split: Server Log (Top) vs Packet Info (Bottom)
        JSplitPane techSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // Server Log
        serverLog = new JTextArea();
        serverLog.setEditable(false);
        serverLog.setBorder(BorderFactory.createTitledBorder("Server Log (Technical View)"));
        serverLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        techSplit.setTopComponent(new JScrollPane(serverLog));

        // Packet Info Panel
        packetInfoPanel = new JTextArea();
        packetInfoPanel.setEditable(false);
        packetInfoPanel.setBorder(BorderFactory.createTitledBorder("Packet Inspection (Headers)"));
        packetInfoPanel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        techSplit.setBottomComponent(new JScrollPane(packetInfoPanel));
        techSplit.setResizeWeight(0.5);

        contentSplit.setRightComponent(techSplit);
        contentSplit.setResizeWeight(0.4); // 40% for Chat, 60% for Logs

        mainSplit.setBottomComponent(contentSplit);
        mainSplit.setResizeWeight(0.3); // 30% Animation, 70% Content

        add(mainSplit, BorderLayout.CENTER);

        // Bottom Panel: Client Input
        JPanel bottomPanel = new JPanel(new BorderLayout());
        clientInput = new JTextField();
        clientInput.setBorder(BorderFactory.createTitledBorder("Client Message"));
        sendButton = new JButton("Send");
        sendButton.addActionListener(this::sendMessage);
        sendButton.setEnabled(false); // Disabled initially for TCP
        bottomPanel.add(clientInput, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // Initialize Animation Timer
        animationTimer = new Timer(10, e -> updateAnimation());

        // Start initial protocol
        switchProtocol("TCP");
    }

    private void updateAnimation() {
        int clientX = 100;
        int serverX = getWidth() - 120;

        // Packet Loss Simulation
        if (packetLossCheckBox.isSelected() && !isReturnTrip && !packetLost && !isRetransmitting) {
            if (packetX > getWidth() / 2 && random.nextInt(100) < 2) { // 2% chance per frame ~ high loss
                packetLost = true;

                if ("TCP".equals(currentProtocol)) {
                    serverLog.append("Network: TCP Packet Lost! Retransmitting...\n");
                    updatePacketInfo("TCP", "LOST", "N/A", "N/A", "None");

                    // TCP Retransmission Logic
                    Timer retryTimer = new Timer(500, evt -> { // Reduced delay
                        serverLog.append("Client: Retransmitting Packet...\n");
                        packetLost = false;
                        isRetransmitting = true;
                        packetX = 50; // Reset to start
                        animationPanel.repaint();
                    });
                    retryTimer.setRepeats(false);
                    retryTimer.start();

                } else {
                    // UDP Loss (No Retransmission)
                    serverLog.append("Network: UDP Packet Lost! (No Retransmission)\n");
                    updatePacketInfo("UDP", "LOST", "N/A", "N/A", "None");
                    Timer lossTimer = new Timer(500, evt -> {
                        isAnimating = false;
                        packetLost = false;
                        packetX = 50;
                        animationTimer.stop();
                        animationPanel.repaint();
                    });
                    lossTimer.setRepeats(false);
                    lossTimer.start();
                }
                return;
            }
        }

        if (handshakeStep > 0 && handshakeStep < 4) {
            // Handshake Animation
            handleHandshakeAnimation(clientX, serverX);
        } else if (handshakeStep >= 5) {
            // Teardown Animation
            handleTeardownAnimation(clientX, serverX);
        } else {
            // Normal Message Animation
            handleMessageAnimation(clientX, serverX);
        }
        animationPanel.repaint();
    }

    private void handleHandshakeAnimation(int clientX, int serverX) {
        if (handshakeStep == 1 || handshakeStep == 3) { // Client -> Server (SYN or ACK)
            if (packetX < serverX) {
                packetX += 5;
                if (handshakeStep == 1)
                    updatePacketInfo("TCP", "SYN", "100", "0", "SYN");
                else
                    updatePacketInfo("TCP", "ACK", "101", "301", "ACK");
            } else {
                if (handshakeStep == 1) {
                    serverLog.append("Server: Received SYN. Sending SYN-ACK...\n");
                    handshakeStep = 2;
                    packetX = serverX;
                } else {
                    serverLog.append("Server: Received ACK. Connection Established.\n");
                    handshakeStep = 4;
                    isConnected = true;
                    isAnimating = false;
                    sendButton.setEnabled(true);
                    connectButton.setEnabled(false);
                    disconnectButton.setEnabled(true);
                    animationTimer.stop();
                    updatePacketInfo("TCP", "Idle", "-", "-", "-");
                }
            }
        } else if (handshakeStep == 2) { // Server -> Client (SYN-ACK)
            if (packetX > clientX) {
                packetX -= 5;
                updatePacketInfo("TCP", "SYN-ACK", "300", "101", "SYN, ACK");
            } else {
                serverLog.append("Client: Received SYN-ACK. Sending ACK...\n");
                handshakeStep = 3;
                packetX = clientX;
            }
        }
    }

    private void handleTeardownAnimation(int clientX, int serverX) {
        if (handshakeStep == 5 || handshakeStep == 8) { // Client -> Server (FIN or ACK)
            if (packetX < serverX) {
                packetX += 5;
                if (handshakeStep == 5)
                    updatePacketInfo("TCP", "FIN", "500", "0", "FIN");
                else
                    updatePacketInfo("TCP", "ACK", "502", "702", "ACK");
            } else {
                if (handshakeStep == 5) {
                    serverLog.append("Server: Received FIN. Sending ACK...\n");
                    handshakeStep = 6;
                    packetX = serverX;
                } else {
                    serverLog.append("Server: Received ACK. Connection Closed.\n");
                    handshakeStep = 0; // Reset
                    isConnected = false;
                    isAnimating = false;
                    sendButton.setEnabled(false);
                    connectButton.setEnabled(true);
                    disconnectButton.setEnabled(false);
                    animationTimer.stop();
                    updatePacketInfo("TCP", "Closed", "-", "-", "-");
                }
            }
        } else if (handshakeStep == 6 || handshakeStep == 7) { // Server -> Client (ACK or FIN)
            if (packetX > clientX) {
                packetX -= 5;
                if (handshakeStep == 6)
                    updatePacketInfo("TCP", "ACK", "700", "501", "ACK");
                else
                    updatePacketInfo("TCP", "FIN", "701", "501", "FIN");
            } else {
                if (handshakeStep == 6) {
                    serverLog.append("Client: Received ACK. Server sending FIN...\n");
                    handshakeStep = 7;
                    packetX = serverX; // Server sends FIN now
                } else {
                    serverLog.append("Client: Received FIN. Sending ACK...\n");
                    handshakeStep = 8;
                    packetX = clientX;
                }
            }
        }
    }

    private void handleMessageAnimation(int clientX, int serverX) {
        if (!isReturnTrip) {
            if (packetX < serverX) {
                packetX += 5;
                updatePacketInfo(currentProtocol, "Data", "1000", "500", "PSH, ACK");
            } else {
                isReturnTrip = true;
                isRetransmitting = false; // Reset flag on success
            }
        } else {
            if (packetX > clientX) {
                packetX -= 5;
                updatePacketInfo(currentProtocol, "Response", "500", "1050", "PSH, ACK");
            } else {
                isAnimating = false;
                isReturnTrip = false;
                packetX = 50;
                animationTimer.stop();
                updatePacketInfo(currentProtocol, "Idle", "-", "-", "-");
            }
        }
    }

    private void updatePacketInfo(String protocol, String type, String seq, String ack, String flags) {
        String info = String.format("""
                Protocol: %s
                Type: %s
                Source Port: %s
                Dest Port: %s
                SEQ: %s
                ACK: %s
                Flags: %s
                """,
                protocol, type,
                (isReturnTrip || handshakeStep == 2 || handshakeStep == 6 || handshakeStep == 7) ? "12345 (Server)"
                        : "54321 (Client)",
                (isReturnTrip || handshakeStep == 2 || handshakeStep == 6 || handshakeStep == 7) ? "54321 (Client)"
                        : "12345 (Server)",
                seq, ack, flags);
        packetInfoPanel.setText(info);
    }

    private void switchProtocol(String protocol) {
        if (networkManager != null) {
            networkManager.stopServer();
        }

        currentProtocol = protocol;
        serverLog.append("\nSwitching to " + protocol + "...\n");
        clientChatArea.append("\n--- Switched to " + protocol + " ---\n");

        // Reset State
        isConnected = false;
        handshakeStep = 0;
        isAnimating = false;
        packetLost = false;
        isRetransmitting = false;
        animationTimer.stop();
        isRetransmitting = false;
        animationTimer.stop();
        clientSequenceNumber = 0; // Reset Sequence Number
        updatePacketInfo(protocol, "Idle", "-", "-", "-");

        if ("TCP".equals(protocol)) {
            networkManager = new TCPHandler();
            connectButton.setVisible(true);
            connectButton.setEnabled(true);
            disconnectButton.setVisible(true);
            disconnectButton.setEnabled(false);
            sendButton.setEnabled(false);
            if (appInfoLabel != null)
                appInfoLabel
                        .setText("TCP: Reliable, Ordered. Used for: Web (HTTP), Email (SMTP), File Transfer (FTP).");
        } else {
            networkManager = new UDPHandler();
            connectButton.setVisible(false);
            disconnectButton.setVisible(false);
            sendButton.setEnabled(true);
            isConnected = true;
            if (appInfoLabel != null)
                appInfoLabel.setText("UDP: Unreliable, Fast. Used for: Streaming, Gaming, VoIP, DNS.");
            networkManager.setSimulatePacketLoss(packetLossCheckBox.isSelected());
        }

        // Start Server with Bot Logic
        networkManager.startServer(PORT, msg -> {
            SwingUtilities.invokeLater(() -> serverLog.append("Server Received: " + msg + "\n"));
            return "Bot: I received '" + msg + "'";
        });

        animationPanel.repaint();
    }

    private void initiateHandshake(ActionEvent e) {
        if ("TCP".equals(currentProtocol)) {
            serverLog.append("Client: Initiating 3-Way Handshake (SYN)....\n");
            handshakeStep = 1; // Start SYN
            packetX = 100;
            isAnimating = true;
            connectButton.setEnabled(false);
            animationTimer.start();
        }
    }

    private void initiateTeardown(ActionEvent e) {
        if ("TCP".equals(currentProtocol) && isConnected) {
            serverLog.append("Client: Initiating Teardown (FIN)...\n");
            handshakeStep = 5; // Start FIN
            packetX = 100;
            isAnimating = true;
            disconnectButton.setEnabled(false);
            sendButton.setEnabled(false);
            animationTimer.start();
        }
    }

    private void sendMessage(ActionEvent e) {
        String msg = clientInput.getText();
        if (!msg.isEmpty()) {
            clientChatArea.append("Me: " + msg + "\n"); // Update Chat UI

            // Send message and handle response
            // Send message and handle response
            String finalMsg = msg;
            if ("UDP".equals(currentProtocol)) {
                finalMsg = "SEQ:" + clientSequenceNumber + "|" + msg;
                clientSequenceNumber++;
            }

            networkManager.sendMessage(HOST, PORT, finalMsg, response -> {
                if (!packetLost) {
                    SwingUtilities.invokeLater(() -> {
                        if (response.startsWith("NACK:")) {
                            String missingSeq = response.split(":")[1];
                            clientChatArea
                                    .append("Server Notification: I have not received message with Sequence Number "
                                            + missingSeq + "\n");
                            serverLog.append("Network: Server sent NACK for SEQ " + missingSeq + "\n");
                        } else {
                            clientChatArea.append(response + "\n"); // Update Chat UI with Bot Response
                        }
                        // serverLog.append("Client Received: " + response + "\n"); // Optional: Keep
                        // technical log clean
                    });
                }
            });
            clientInput.setText("");
            startAnimation();
        }
    }

    private void startAnimation() {
        isAnimating = true;
        isReturnTrip = false;
        packetLost = false;
        isRetransmitting = false;
        handshakeStep = 0;
        packetX = 100;
        animationTimer.start();
    }
}
