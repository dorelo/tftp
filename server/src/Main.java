import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Created by Alpha on 20/04/2017.
 */
public class Main {
    private static int listening_port = 9000;

    public static void main(String[] args) {
        try {
            Socket server;
            ServerSocket listener = new ServerSocket(listening_port);
            System.out.println("Listening on port " + listening_port);

            while(true) {
                server = listener.accept();
                Worker conn = new Worker(server);
                Thread t = new Thread(conn);
                t.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
