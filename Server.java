import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Server {

    static final int LAUNCH = 1;
    static final int PLAY_PAUSE = 2;
    static final int NEXT = 3;
    static final int PREV = 4;
    static final int VOL_UP = 5;
    static final int VOL_DOWN = 6;
    static final int DISC = 7;
    static final int ACK = 8;

    static final String FOOBAR_APP = "foobar2000.exe";
    static final String FOOBAR_PATH = "C:\\Program Files (x86)\\foobar2000\\";

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private BufferedInputStream reader;
    private BufferedOutputStream writer;
    private int port;
    private boolean shouldTerminate;

    public Server(int port) {
        serverSocket = null;
        clientSocket = null;
        reader = null;
        writer = null;
        this.port = port;
        shouldTerminate = false;
    }

    private void serve() {
        try {
            serverSocket = new ServerSocket(port);
            while (true) {
                clientSocket = serverSocket.accept();
                System.out.println("Connection established");
                reader = new BufferedInputStream(clientSocket.getInputStream());
                writer = new BufferedOutputStream(clientSocket.getOutputStream());
                shouldTerminate = false;
                int input;
                while (!shouldTerminate && (input = reader.read()) >= 0) {
                    writer.write(process(input));
                    writer.flush();
                }
                System.out.println("Connection closed");
            }
        } catch (SocketException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    private void close() {
        try {
            clientSocket.close();
            reader.close();
            writer.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int process(int input) {
        try {
            String arg;
            switch (input) {
                case LAUNCH:
                    arg = "";
                    break;
                case PLAY_PAUSE:
                    arg = "/playpause";
                    break;
                case NEXT:
                    arg = "/next";
                    break;
                case PREV:
                    arg = "/prev";
                    break;
                case VOL_UP:
                    arg = "/command:up";
                    break;
                case VOL_DOWN:
                    arg = "/command:down";
                    break;
                case DISC:
                    shouldTerminate = true;
                    return ACK;
                default:
                    return -1;
            }
            Runtime.getRuntime().exec(String.format("cmd /c start %s %s", FOOBAR_APP, arg), null, new File(FOOBAR_PATH));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ACK;
    }

    public static void main(String args[]) {
        new Server(Integer.parseInt(args[0])).serve();
    }
}
