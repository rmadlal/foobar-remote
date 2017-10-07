import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.*;

public class Server {

    static final int LAUNCH = 0;
    static final int PLAY_PAUSE = 1;
    static final int NEXT = 2;
    static final int PREV = 3;
    static final int STOP = 4;
    static final int RANDOM = 5;
    static final int VOL_UP = 6;
    static final int VOL_DOWN = 7;
    static final int DEFAULT = 8;
    static final int REPEAT_PLAYLIST = 9;
    static final int REPEAT_TRACK = 10;
    static final int ORDER_RANDOM = 11;
    static final int SHUFFLE_TRACKS = 12;
    static final int SHUFFLE_ALBUMS = 13;
    static final int SHUFFLE_FOLDERS = 14;
    static final int DISC = 15;
    static final int ACK = 16;

    static final String[] CMD_ARGS = {
            "",
            "/playpause",
            "/next",
            "/prev",
            "/command:stop",
            "/runcmd=Playback/Random",
            "/command:up",
            "/command:down",
            "/runcmd=Playback/Order/Default",
            "\"/runcmd=Playback/Order/Repeat (playlist)\"",
            "\"/runcmd=Playback/Order/Repeat (track)\"",
            "/runcmd=Playback/Order/Random",
            "\"/runcmd=Playback/Order/Shuffle (tracks)\"",
            "\"/runcmd=Playback/Order/Shuffle (albums)\"",
            "\"/runcmd=Playback/Order/Shuffle (folders)\""
    };

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
                while (!shouldTerminate && (input = reader.read()) != -1) {
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
            if (input == DISC) {
                shouldTerminate = true;
                return ACK;
            }
            String arg = CMD_ARGS[input];
            Runtime.getRuntime().exec(String.format("cmd /c start %s %s", FOOBAR_APP, arg), null, new File(FOOBAR_PATH));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException e) {
            shouldTerminate = true;
            return -1;
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
