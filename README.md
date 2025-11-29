# chatbot-over-tcp-udp

Chat Bot Over TCP/UDP Walkthrough

Overview
This application demonstrates Chat Bot communication over TCP and UDP protocols with a graphical visualization.

How to Run
Open Terminal: Navigate to the project directory:

     cd "/Users/yash/Desktop/cn proj"
Compile:

     javac -d bin src/Main.java src/GUI/MainFrame.java src/Networking/*.java
Run:

     java -cp bin Main
Features
Protocol Selection
Use the dropdown menu at the top to switch between TCP and UDP.
Switching protocols will restart the internal server on the selected protocol.
Messaging
Type a message in the "Client Message" box at the bottom.
Click Send to transmit the message.
The message will appear in the "Server Log" area.
Bot Response: The Server (Bot) will automatically reply with "Bot: I received '[your message]'".
Visualization
TCP:
Handshake: Shows a 3-way handshake animation (SYN -> SYN-ACK -> ACK) before connection.
Connected: Shows a green connection line after handshake.
UDP: No connection line is shown.
Animation:
Client -> Server: An orange packet animates from Client to Server.
Server Processing: The Server processes the message.
Server -> Client: A blue packet animates back from Server to Client (Round-Trip).
Enhanced Chat UI
Client Chat (Left): Displays the conversation history ("Me" and "Bot" messages) in a user-friendly format.
Server Log (Right Top): Displays technical network events (e.g., "Received SYN", "Connection Established").
Packet Inspection (Right Bottom): Displays detailed headers for the current packet.
TCP Retransmission & Applications
TCP Retransmission: If a packet is lost in TCP, the client waits for a timeout and automatically retransmits it, ensuring reliability.
Application Info: A panel at the top displays real-world use cases for the selected protocol (e.g., HTTP/SMTP for TCP, Streaming/Gaming for UDP).
Verification Steps
Launch the App: Follow the "How to Run" steps.
Test TCP:
Ensure "TCP" is selected.
App Info: Verify the top panel says "TCP: Reliable... Used for: Web...".
Handshake: Click Connect.
Retransmission:
Check "Simulate Packet Loss".
Send "Test Retry".
Verify: Packet turns Red "X" -> Log shows "Timeout" -> Packet restarts (Retrying...) -> Reaches Server.
Chat: Send "Hello". Verify Chat UI updates.
Test UDP:
Switch to "UDP".
App Info: Verify the top panel says "UDP: Unreliable... Used for: Streaming...".
Packet Loss: Check "Simulate Packet Loss".
Chat: Send "Test Loss".
Verify: Packet turns Red "X" -> Log shows "Lost" -> Animation stops (No retry).
