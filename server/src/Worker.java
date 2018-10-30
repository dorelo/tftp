import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Created by Alpha on 20/04/2017.
 */
public class Worker implements Runnable {
    private Socket server;
    private DataInputStream in;
    private DataOutputStream out;
    private Path file_path;

    public Worker(Socket server) throws IOException {
        this.server = server;
        this.in = new DataInputStream(server.getInputStream());
        this.out = new DataOutputStream(server.getOutputStream());
    }

    @Override
    public void run() {
        System.out.println("Running!");

        System.out.println("Just connected to " + server.getRemoteSocketAddress());
        try {


            byte[] resultBuff = input_reader(in);

            // Check for a request
            byte[] OPCODE = {resultBuff[0], resultBuff[1]};
            // Is write request
            if (OPCODE[1] == (byte) 2) {
                // Pass to write handler
                WRQ_handler(resultBuff);
                // Is read request
            } else if (OPCODE[1] == (byte) 1) {
                // Pass to read handler
                RRQ_handler(resultBuff);
            } else {
                // Illegal operation

                byte[] error_code = {(byte) 0, (byte) 4};
                byte[] error = generate_error(error_code, "Sent illegal operation error!");
                int error_len = error.length;
                out.writeInt(error_len);
                if (error_len > 0) {
                    out.write(error, 0, error_len);
                }
                out.flush();
                System.out.println("Sent illegal TFTP operation error!");
                out.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WRQ_handler(byte[] request) throws IOException {
        System.out.println("Got write request!");
        int i = 2;
        char ptr = (char) request[i];
        String filename = "";

        while (ptr != (char) 0) {
            filename += ptr;
            i++;
            ptr = (char) request[i];

        }

        out.flush();

        File test_file = new File(filename);
        if(!test_file.exists()) {

            // Send ACK response
            byte[] ack = {(byte) 0, (byte) 4, (byte) 0, (byte) 0};
            int len = ack.length;
            out.writeInt(len);
            if (len > 0){
                out.write(ack, 0, len);
                System.out.println("Sent initial ACK!");
            }

            // Get file from client
            // FileOutputStream for writing the received file
            FileOutputStream fos = new FileOutputStream(filename);
            BufferedOutputStream bos = new BufferedOutputStream(fos);

            // Write to file
            bos.write(input_reader(in));
            bos.flush();

            // Terminate
            System.out.println("File transferred successfully.");
            in.close();
            server.close();
        } else {
            // Send error packet for FILE EXISTS
            byte[] error_code = {(byte) 0, (byte) 6};
            byte[] error = generate_error(error_code, "File exists");
            int error_len = error.length;
            out.writeInt(error_len);
            if (error_len > 0) {
                out.write(error, 0, error_len);
            }
            out.flush();
            System.out.println("Sent file exists error!");

        }

    }

    private void RRQ_handler(byte[] request) {
        System.out.println("Got read request!");

        // Get filename from request
        int i = 2;
        char ptr = (char) request[i];
        String filename = "";

        while (ptr != (char) 0) {
            filename += ptr;
            i++;
            ptr = (char) request[i];

        }

        // Read file as bytes

        try {
            try {
                this.file_path = Paths.get(filename);
                Path real_path = file_path.toRealPath();
            } catch (NoSuchFileException e) {
                // Send error packet for FILE NOT FOUND

                byte[] error_code = {(byte) 0, (byte) 1};

                byte[] error = generate_error(error_code, "File not found");

                int error_len = error.length;
                out.writeInt(error_len);
                if (error_len > 0) {
                    out.write(error, 0, error_len);
                }
                System.out.println("Sent file not found error!");
                out.flush();
                out.close();
                server.close();

            } catch (AccessDeniedException e1) {
                // Send error packet for ACCESS VIOLATION

                byte[] error_code = {(byte) 0, (byte) 2};
                byte[] error = generate_error(error_code, "Access violation");

                int error_len = error.length;
                out.writeInt(error_len);
                if (error_len > 0) {
                    out.write(error, 0, error_len);
                }
                System.out.println("Sent access violation error!");
                out.flush();
                out.close();
                server.close();
            }

            byte[] data = Files.readAllBytes(file_path);

            // Sending file data to the server
            int len = data.length;
            out.writeInt(len);
            if (len > 0) {
                out.write(data, 0, len);
            }
            out.flush();

            //Closing socket

            out.close();
            server.close();
        } catch (IOException e) {
        }

    }

    private byte[] input_reader(DataInputStream inputStream) throws IOException {
        int len = inputStream.readInt();
        byte[] data = new byte[len];
        if (len > 0) {
            inputStream.readFully(data);
        }
        return data;
    }

    private byte[] generate_error(byte[] error_code, String error_message) {
        // ASCII representation of error message string
        int size_ptr = 0;
        byte[] error_message_bytes = error_message.getBytes(StandardCharsets.US_ASCII);
        byte[] data = new byte[error_message_bytes.length + 5];

        // Initial byte
        data[size_ptr] = (byte) 0;
        size_ptr++;

        // OPCODE
        data[size_ptr] = (byte) 5;
        size_ptr++;

        // Error code
        data[size_ptr] = error_code[0];
        size_ptr++;
        data[size_ptr] = error_code[1];
        size_ptr++;


        // Error string
        for(byte b :error_message_bytes) {
            data[size_ptr] = b;
            size_ptr++;
        }

        // Terminating byte
        data[size_ptr] = (byte) 0;

        // Return

        return data;
    }

}
