import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TFTP_TCP_CLIENT {
    static final byte TFTP_OPCODE_READ 	= 1;
    static final byte TFTP_OPCODE_WRITE = 2;
    private InetAddress remote_host;
    private int remote_port;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Socket s;

    public TFTP_TCP_CLIENT(String remote_host, int remote_port) throws IOException {
        this.remote_host = InetAddress.getByName(remote_host);
        this.remote_port = remote_port;

        s = new Socket(remote_host, remote_port);
        System.out.println("Connected to " +remote_host+ " on port " +remote_port);

        this.outputStream = new DataOutputStream(s.getOutputStream());
        this.inputStream = new DataInputStream(s.getInputStream());

    }

    private void upload(String file) throws IOException {

        // Send write request
        byte[] WRQ = generate_request(TFTP_OPCODE_WRITE, file);
        int length = WRQ.length;
        outputStream.writeInt(length);
        if (length > 0) {
            outputStream.write(WRQ, 0, length);
        }
        outputStream.flush();
        System.out.println("Sent write request!");


        byte[] resultBuff = input_reader(inputStream);

        // Check this is a confirmation ACK
        byte[] OPCODE = {resultBuff[0], resultBuff[1]};
        if (OPCODE[1] == (byte) 4) {
            System.out.println(OPCODE[1]);
            System.out.println("Got ACK!");
            //Send file

            // Read file as bytes
            Path path = Paths.get(file);
            byte[] data = Files.readAllBytes(path);

            // Send
            // Sending file data to the server
            this.outputStream = new DataOutputStream(s.getOutputStream());
            int len = data.length;
            outputStream.writeInt(len);
            if (len > 0) {
                outputStream.write(data, 0, len);
            }
            outputStream.flush();

            //Closing socket
            outputStream.close();
            s.close();

        } else if (OPCODE[1] == (byte) 5){
            System.out.println("Entered else if condition");
            System.out.println("Error received!");
            System.out.println(check_error(resultBuff));
            outputStream.close();
            s.close();
            System.exit(1);

        }
    }

    private String check_error(byte[] resultBuff) {
        String ret = "";
        if (resultBuff[3] == (byte) 0) {
            ret = "Undefined error";
        }

        if (resultBuff[3] == (byte) 1) {
            ret = "File not found error";
        }

        if (resultBuff[3] == (byte) 2) {
            ret = "Access violation error";
        }

        if (resultBuff[3] == (byte) 3) {
            ret = "Disk full or allocation exceeded error";
        }

        if (resultBuff[3] == (byte) 4) {
            ret = "Illegal TFTP operation error";
        }

        if (resultBuff[3] == (byte) 5) {
            ret = "Unknown TID error";
        }

        if (resultBuff[3] == (byte) 6) {
            ret = "File already exists error";
        }

        if (resultBuff[3] == (byte) 7) {
            ret = "No such user error";
        }
        return ret;
    }

    private void download(String file) throws IOException {
        // Send read request
        byte[] RRQ = generate_request(TFTP_OPCODE_READ, file);
        int length = RRQ.length;
        outputStream.writeInt(length);
        if (length > 0) {
            outputStream.write(RRQ, 0, length);
        }
        outputStream.flush();

        // Check for errors or write to file
        byte[] data = input_reader(inputStream);
        if (data[1] == (byte) 5) {
            System.out.println(check_error(data));
            inputStream.close();
            s.close();
            System.exit(1);

        } else {
            // Get file from server
            // FileOutputStream for writing the received file
            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(data);
            bos.flush();
        }

        // Terminate
        System.out.println("File transferred successfully.");
        inputStream.close();
        s.close();

    }

    /*
    Handle reading received data
     */
    private byte[] input_reader(DataInputStream inputStream) throws IOException {
        int len = inputStream.readInt();
        byte[] data = new byte[len];
        if (len > 0) {
            while(inputStream.available() > 0) {
                inputStream.readFully(data);
            }
        }


        return data;
    }

    private byte[] generate_request(byte opcode, String file) {
        /*
        2 B OPCODE - FILENAME - 1 B 0 - MODE - 1 B 0
         */
        int size_ptr = 0;
        int max_size = 516;
        String transfer_mode = "octet";
        // Size of packet
        byte data[] = new byte[max_size];

        // Get the ascii representation of "octet"
        byte[] octet_bytes = transfer_mode.getBytes(StandardCharsets.US_ASCII);
        byte[] file_bytes = file.getBytes(StandardCharsets.US_ASCII);


        // Opcode
        data[size_ptr] = (byte) 0;
        size_ptr++;
        data[size_ptr] = opcode;
        size_ptr++;

        // Copy the filename in
        for(byte b :file_bytes) {
            data[size_ptr] = b;
            size_ptr++;
        }

        // Second zero
        data[size_ptr] = (byte) 0;
        size_ptr++;

        // Copy the mode in
        for(byte o :octet_bytes) {
            data[size_ptr] = o;
            size_ptr++;
        }

        // Final zero
        data[size_ptr] = (byte) 0;

        return data;

    }

    public static void main(String[] args) throws Exception {
        // Make sure there are enough arguments
        if (args.length != 4)
        {
            System.err.println("Error: invalid number of arguments.");
            System.exit(1);
        }

        /*
         Usage:
            arg0: remote host
            arg1: remote port
            arg2: -u (upload) or -d (download)
            arg3: filename
          */

        String remote_host = args[0];
        int remote_port = Integer.parseInt(args[1]);
        String filename = args[3];

        if (args[2].equals("-u")) {
            TFTP_TCP_CLIENT client = new TFTP_TCP_CLIENT(remote_host, remote_port);
            client.upload(filename);
        } else {
            TFTP_TCP_CLIENT client = new TFTP_TCP_CLIENT(remote_host, remote_port);
            client.download(filename);
        }
    }
}