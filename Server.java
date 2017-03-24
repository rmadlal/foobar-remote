import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Server {

    public static final byte LAUNCH = 1;
    public static final byte PLAY_PAUSE = 2;
    public static final byte NEXT = 3;
    public static final byte PREV = 4;
    public static final byte VOL_UP = 5;
    public static final byte VOL_DOWN = 6;
    public static final byte DISC = 7;
    public static final byte ACK = 8;

    private ServerSocket serverSocket;
    private BufferedInputStream reader;
    private BufferedOutputStream writer;
    private Socket clientSocket;
    private int port;
    private boolean shouldTerminate;

    public Server(int port) {
        serverSocket = null;
        reader = null;
        writer = null;
        clientSocket = null;
        this.port = port;
        shouldTerminate = false;
    }

    private void serve() {
        try {
            serverSocket = new ServerSocket(port);
            clientSocket = serverSocket.accept();
            System.out.println("Connection established");
            reader = new BufferedInputStream(clientSocket.getInputStream());
            writer = new BufferedOutputStream(clientSocket.getOutputStream());
            int input;
            while (!shouldTerminate && (input = reader.read()) >= 0) {
                writer.write(process((byte) input));
                writer.flush();
            }
            close();
            System.out.println("Connection closed");
        } catch (SocketException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void close() {
        try {
            if (reader != null)
                reader.close();
            if (writer != null)
                writer.close();
            if (clientSocket != null)
                clientSocket.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte process(byte input) {
        try {
            String application;
            String arg = "";
            String path;
            switch (input) {
                case LAUNCH:
                    application = "foobar2000.exe";
                    path = "C:\\Program Files (x86)\\foobar2000\\";
                    break;
                case PLAY_PAUSE:
                    application = "Keypress.py";
                    arg = "play_pause";
                    path = "C:\\Python34\\My programs\\";
                    break;
                case NEXT:
                    application = "Keypress.py";
                    arg = "next";
                    path = "C:\\Python34\\My programs\\";
                    break;
                case PREV:
                    application = "Keypress.py";
                    arg = "prev";
                    path = "C:\\Python34\\My programs\\";
                    break;
                case VOL_UP:
                    application = "Keypress.py";
                    arg = "vol_up";
                    path = "C:\\Python34\\My programs\\";
                    break;
                case VOL_DOWN:
                    application = "Keypress.py";
                    arg = "vol_down";
                    path = "C:\\Python34\\My programs\\";
                    break;
                case DISC:
                    shouldTerminate = true;
                    return ACK;
                default:
                    return -1;
            }
            Runtime.getRuntime().exec(String.format("cmd /c start %s %s", application, arg), null, new File(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ACK;
    }

    public static void main(String args[]) {
        new Server(Integer.parseInt(args[0])).serve();
    }
}
