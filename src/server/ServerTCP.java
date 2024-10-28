package src.server;
import java.net.*;
import java.io.*;
import java.nio.*;

public class ServerTCP {
    private static final int BUFSIZE = 1024;

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Parameter(s): <Port>");
        }

        int port = Integer.parseInt(args[0]);
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server is running on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // Handle client in a separate thread to allow multiple clients
                Thread clientHandler = new Thread(() -> handleClient(clientSocket));
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Server socket closed.");
            }
        }
    }

    private static void handleClient(Socket clientSocket) {
        try {
            InputStream in = clientSocket.getInputStream();
            OutputStream out = clientSocket.getOutputStream();
            byte[] buffer = new byte[BUFSIZE];

            // Keep reading from the client until they disconnect
            while (true) {
                int length = in.read(buffer);
                if (length == -1) {
                    // Client has closed the connection
                    break;
                }

                System.out.println("Received request (hex):");
                for (int i = 0; i < length; i++) {
                    System.out.printf("%02X ", buffer[i]);
                }
                System.out.println();

                // Parse the request
                ByteBuffer bb = ByteBuffer.wrap(buffer, 0, length);
                short tml = bb.getShort();

                // Check if the actual message length matches the TML
                byte errorCode = 0;
                if (tml != length) {
                    System.out.println("Error: Actual message length does not match TML");
                    errorCode = 127;
                }

                byte opCode = bb.get();
                short operand1 = bb.getShort();
                short operand2 = bb.getShort();
                byte requestId = bb.get();

                String[] operations = {"div", "mul", "and", "or", "add", "sub"};
                String operation = (opCode >= 0 && opCode < operations.length) ? operations[opCode] : "unknown";

                System.out.printf("Request ID: %d, Operation: %s, Operands: %d, %d\n", 
                                requestId, operation, operand1, operand2);

                // Perform the calculation
                int result = 0;
                if (errorCode == 0) {
                    try {
                        switch (opCode) {
                            case 0: result = operand1 / operand2; break;
                            case 1: result = operand1 * operand2; break;
                            case 2: result = operand1 & operand2; break;
                            case 3: result = operand1 | operand2; break;
                            case 4: result = operand1 + operand2; break;
                            case 5: result = operand1 - operand2; break;
                            default:
                                System.out.println("Error: Invalid operation code");
                                errorCode = 127;
                        }
                    } catch (ArithmeticException e) {
                        System.out.println("Error: " + e.getMessage());
                        errorCode = 127;
                    }
                }

                // Prepare the response
                ByteBuffer response = ByteBuffer.allocate(8);
                response.putShort((short) 8); // TML
                response.put(requestId);
                response.putInt(result);
                response.put(errorCode);

                // Send the response
                out.write(response.array());
                out.flush(); 

                System.out.println("Sent response (hex):");
                for (byte b : response.array()) {
                    System.out.printf("%02X ", b);
                }
                System.out.println("\n");
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                System.out.println("Client connection closed.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}