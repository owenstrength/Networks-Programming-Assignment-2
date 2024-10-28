 # TCP Calculator

## Overview

This project implements a simple TCP-based calculator system consisting of a server and a client. The server performs bitwise Boolean and arithmetic computations requested by the client on signed integers.

## Features

- Supports six operations: addition (+), subtraction (-), bitwise OR (|), bitwise AND (&), division (/), and multiplication (*).
- Implements a custom protocol for communication between client and server.
- Handles errors such as invalid operations and division by zero.
- Provides both Java and Python implementations of the server.

## Requirements

- Java Development Kit (JDK) 8 or higher
- Python 3.6 or higher (for Python server implementation)

## File Structure

```
root/
│
├── src/
│   ├── server/
│   │   ├── ServerTCP.java
│   │   └── ServerTCP.py
│   │
│   └── client/
│       └── ClientTCP.java
│
└── README.md
```

## Setup and Compilation

### Java Implementation

1. Compile the server:
   ```
   javac src/server/ServerTCP.java
   ```
2. Compile the client:
   ```
   javac src/client/ClientTCP.java
   ```

### Python Implementation

No compilation is necessary for the Python server.

## Usage

### Running the Server

#### Java Server

```
java src.server.ServerTCP <port>
```

#### Python Server

```
python src/server/serverTCP.py <port>
```

Replace `<port>` with the desired port number (e.g., 10031).

### Running the Client

```
java src.client.ClientTCP <server_address> <server_port>
```

Replace `<server_address>` with the server's IP address or hostname (use `localhost` if running on the same machine), and `<server_port>` with the port number used for the server.

## Client Usage

Once the client is running, you will be prompted to enter operations in the following format:

```
Enter OpCode (0-5), Operand1, and Operand2 (or 'q' to quit): <op_code> <operand1> <operand2>
```

OpCodes:
- 0: Division (/)
- 1: Multiplication (*)
- 2: Bitwise AND (&)
- 3: Bitwise OR (|)
- 4: Addition (+)
- 5: Subtraction (-)

Example: `4 10 20` will perform addition: 10 + 20

Enter 'q' to quit the client.

## Protocol Specification

### Request Format

| Field         | Size (bytes) | Description                                    |
|---------------|--------------|------------------------------------------------|
| TML           | 2            | Total Message Length (including TML)           |
| Op Code       | 1            | Operation code (0-5)                           |
| Operand 1     | 2            | First operand (signed short)                   |
| Operand 2     | 2            | Second operand (signed short)                  |
| Request ID    | 1            | Unique identifier for the request              |
| Op Name Length| 1            | Length of the operation name in bytes          |
| Op Name       | Variable     | Name of the operation (UTF-16 encoded)         |

### Response Format

| Field         | Size (bytes) | Description                                    |
|---------------|--------------|------------------------------------------------|
| TML           | 2            | Total Message Length (always 8)                |
| Request ID    | 1            | Matches the Request ID from the request        |
| Result        | 4            | Result of the operation (signed int)           |
| Error Code    | 1            | 0 for success, 127 for error                   |

## Error Handling

- The server returns an error code of 127 for invalid operations, division by zero, or when the actual message length doesn't match the TML.
- The client will retry the request up to 5 times in case of a timeout.
