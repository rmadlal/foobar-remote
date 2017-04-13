import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.*;

public class Server {

    static final int LAUNCH = 1;
    static final int PLAY_PAUSE = 2;
    static final int NEXT = 3;
    static final int PREV = 4;
    static final int STOP = 5;
    static final int RANDOM = 6;
    static final int VOL_UP = 7;
    static final int VOL_DOWN = 8;
    static final int DEFAULT = 9;
    static final int REPEAT_PLAYLIST = 10;
    static final int REPEAT_TRACK = 11;
    static final int ORDER_RANDOM = 12;
    static final int SHUFFLE_TRACKS = 13;
    static final int SHUFFLE_ALBUMS = 14;
    static final int SHUFFLE_FOLDERS = 15;
    static final int DISC = 16;
    static final int ACK = 17;

    static final String FOOBAR_APP = "foobar2000.exe";
    static final String FOOBAR_PATH = "C:\\Program Files (x86)\\foobar2000\\";

    private ServerSocket serverSocket;
    private int port;
    private boolean shouldTerminate;

    public Server(int port) {
        serverSocket = null;
        this.port = port;
        shouldTerminate = false;
    }

    private void serve() throws IOException {
        serverSocket = new ServerSocket(port);
        while (true) {
            try (Socket clientSocket = serverSocket.accept();
                 BufferedInputStream reader = new BufferedInputStream(clientSocket.getInputStream());
                 BufferedOutputStream writer = new BufferedOutputStream(clientSocket.getOutputStream())) {

                System.out.println("Connection established");
                shouldTerminate = false;
                int input;
                while (!shouldTerminate && (input = reader.read()) >= 0) {
                    writer.write(process(input));
                    writer.flush();
                }
                System.out.println("Connection closed");
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
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
                case STOP:
                    arg = "/command:stop";
                    break;
                case RANDOM:
                    arg = "/command:random";
                    break;
                case VOL_UP:
                    arg = "/command:up";
                    break;
                case VOL_DOWN:
                    arg = "/command:down";
                    break;
                case DEFAULT:
                    arg = "/runcmd=Playback/Order/Default";
                    break;
                case REPEAT_PLAYLIST:
                    arg = "\"/runcmd=Playback/Order/Repeat (playlist)\"";
                    break;
                case REPEAT_TRACK:
                    arg = "\"/runcmd=Playback/Order/Repeat (track)\"";
                    break;
                case ORDER_RANDOM:
                    arg = "/runcmd=Playback/Order/Random";
                    break;
                case SHUFFLE_TRACKS:
                    arg = "\"/runcmd=Playback/Order/Shuffle (tracks)\"";
                    break;
                case SHUFFLE_ALBUMS:
                    arg = "\"/runcmd=Playback/Order/Shuffle (albums)\"";
                    break;
                case SHUFFLE_FOLDERS:
                    arg = "\"/runcmd=Playback/Order/Shuffle (folders)\"";
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
        try {
            System.out.println("Listening on " + InetAddress.getLocalHost().getHostAddress());
            new Server(Integer.parseInt(args[0])).serve();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
