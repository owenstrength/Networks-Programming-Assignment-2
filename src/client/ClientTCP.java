package src.client;
import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ClientTCP {
    private static final int TIMEOUT = 3000; // Timeout in milliseconds
    private static final int MAXTRIES = 5;   // Maximum number of retries

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException("Parameters: <Server> <Port>");
        }

        InetAddress serverAddress = InetAddress.getByName(args[0]);
        int serverPort = Integer.parseInt(args[1]);

        Scanner scanner = new Scanner(System.in);
        byte requestId = (byte) (Math.random() * 256); // Initialize request ID

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(serverAddress, serverPort), TIMEOUT);
            socket.setSoTimeout(TIMEOUT);

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            while (true) {
                System.out.print("Enter OpCode (0-5), Operand1, and Operand2 (or 'q' to quit): ");
                String input = scanner.nextLine();

                if (input.equalsIgnoreCase("q")) {
                    break;
                }

                String[] parts = input.split("\\s+");
                if (parts.length != 3 || !parts[0].matches("[0-5]") || !parts[1].matches("-?\\d+") || !parts[2].matches("-?\\d+")) {
                    System.out.println("Invalid input. Please enter OpCode, Operand1, and Operand2.");
                    continue;
                }

                byte opCode = Byte.parseByte(parts[0]);
                short operand1 = Short.parseShort(parts[1]);
                short operand2 = Short.parseShort(parts[2]);

                // Get the operation name
                String[] opNames = {"div", "mul", "and", "or", "add", "sub"};
                String opName = opNames[opCode];

                // Encode the operation name in UTF-16
                byte[] opNameBytes = opName.getBytes(StandardCharsets.UTF_16);
                short opNameLength = (short) opNameBytes.length;

                // Calculate TML
                short tml = (short) (2 + 1 + 2 + 2 + 1 + 1 + opNameLength);

                // Create the request
                ByteBuffer request = ByteBuffer.allocate(tml);
                request.putShort(tml);
                request.put(opCode);
                request.putShort(operand1);
                request.putShort(operand2);
                request.put(requestId);
                request.put((byte) opNameLength);
                request.put(opNameBytes);

                byte[] requestData = request.array();

                // Display the request in hexadecimal
                System.out.println("Sending request (hex):");
                for (byte b : requestData) {
                    System.out.printf("%02X ", b);
                }
                System.out.println();

                long startTime = System.currentTimeMillis();
                boolean receivedResponse = false;
                int tries = 0;
                byte[] responseData = new byte[8];

                do {
                    out.write(requestData);
                    out.flush();

                    try {
                        int bytesRead = in.read(responseData);
                        if (bytesRead == 8) {
                            receivedResponse = true;
                        }
                    } catch (SocketTimeoutException e) {
                        tries++;
                        System.out.println("Timeout, retrying...");
                    }
                } while (!receivedResponse && tries < MAXTRIES);

                if (receivedResponse) {
                    long endTime = System.currentTimeMillis();
                    ByteBuffer response = ByteBuffer.wrap(responseData);
                    short responseTml = response.getShort();
                    byte responseId = response.get();
                    int result = response.getInt();
                    byte errorCode = response.get();

                    System.out.println("Received response (hex):");
                    for (byte b : responseData) {
                        System.out.printf("%02X ", b);
                    }
                    System.out.println();

                    System.out.printf("Request ID: %d, Result: %d, Error Code: %s\n",
                            responseId, result, errorCode == 0 ? "OK" : errorCode);
                    System.out.println("Round Trip Time: " + (endTime - startTime) + " ms");
                } else {
                    System.out.println("No response - giving up.");
                }

                // Increment request ID
                requestId++;
                System.out.println();
            }
        } finally {
            scanner.close();
        }
    }
}