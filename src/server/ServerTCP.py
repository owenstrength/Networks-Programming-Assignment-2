import socket
import struct
import sys
import threading

BUFSIZE = 1024

def handle_client(conn, addr):
    """Handle a single client connection."""
    print(f"Connected by {addr}")
    
    try:
        while True:
            # Receive data from the client
            data = conn.recv(BUFSIZE)
            if not data:  # Client has closed the connection
                break
                
            print("Received request (hex):")
            print(' '.join(f'{b:02X}' for b in data))

            # Parse the request
            tml = struct.unpack('>H', data[:2])[0]
            
            # Check if the actual message length matches the TML
            error_code = 0
            if tml != len(data):
                print("Error: Actual message length does not match TML")
                error_code = 127

            op_code = data[2]
            operand1, operand2 = struct.unpack('>hh', data[3:7])
            request_id = data[7]

            operations = ["div", "mul", "and", "or", "add", "sub"]
            operation = operations[op_code] if 0 <= op_code < len(operations) else "unknown"
            
            print(f"Request ID: {request_id}, Operation: {operation}, Operands: {operand1}, {operand2}")

            # Perform the calculation
            result = 0
            if error_code == 0:
                try:
                    if op_code == 0:
                        result = operand1 // operand2  # Integer division
                    elif op_code == 1:
                        result = operand1 * operand2
                    elif op_code == 2:
                        result = operand1 & operand2
                    elif op_code == 3:
                        result = operand1 | operand2
                    elif op_code == 4:
                        result = operand1 + operand2
                    elif op_code == 5:
                        result = operand1 - operand2
                    else:
                        print("Error: Invalid operation code")
                        error_code = 127
                except ZeroDivisionError:
                    print("Error: Division by zero")
                    error_code = 127

            # Prepare the response
            response = struct.pack('>HBiB', 8, request_id, result, error_code)
            
            # Send the response
            conn.sendall(response)
            print("Sent response (hex):")
            print(' '.join(f'{b:02X}' for b in response))
            print()
            
    except ConnectionError as e:
        print(f"Connection error with {addr}: {e}")
    finally:
        conn.close()
        print(f"Connection closed with {addr}")

def main(port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        # Allow socket address reuse
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        
        sock.bind(('', port))
        sock.listen(5)  # Increased backlog for multiple clients
        print(f"Server is running on port {port}")
        
        try:
            while True:
                conn, addr = sock.accept()
                # Create a new thread for each client
                client_thread = threading.Thread(
                    target=handle_client,
                    args=(conn, addr)
                )
                client_thread.daemon = True  # Allow the thread to be terminated when main program exits
                client_thread.start()
        except KeyboardInterrupt:
            print("\nServer shutting down...")

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python server.py <port>")
        sys.exit(1)
        
    try:
        port = int(sys.argv[1])
        main(port)
    except ValueError:
        print("Error: Port must be an integer")
        sys.exit(1)