package core;

import core.controller.MainController;
import core.lines.MessageLine;
import core.lines.NoticeLine;
import core.lines.TextLine;
import core.lines.WarningLine;
import core.records.Channel;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.logging.Logger.getLogger;

public class ConnectionHandler extends Thread {
    public static final String DEBUG_HOST = "irc.vub.lt";
    public static final String DEBUG_NICK = "test";
    public static final int DEFAULT_PORT = 6667;
    public static final Set<String> COMMANDS = Set.of("m", "list", "join");
    
    private static final AtomicReference<ConnectionHandler> instance = new AtomicReference<>(null);
    private static final HashMap<String, Integer> COMMAND_ARG_COUNT = new HashMap<>();
    static {
        COMMAND_ARG_COUNT.put("list", 1);
        COMMAND_ARG_COUNT.put("join", 2);
        COMMAND_ARG_COUNT.put("m", 3);
    }
    
    private final String host;
    private final String nick;
    private final Socket socket;
    private final BufferedReader in;
    private final PrintStream out;
    private final ExecutorService decoderThreadPool = Executors.newCachedThreadPool();
    private final HashMap<String, Channel> channels = new HashMap<>();
    private final ArrayDeque<String> messagesToSend = new ArrayDeque<>();
    private Channel curChannel = null;

    public ConnectionHandler(@NotNull String host, int port, @NotNull String nickName) throws IOException, IllegalArgumentException {
        this(host, port, nickName, "");
    }

    public ConnectionHandler(@NotNull String host, int port, @NotNull String nick, @NotNull String password) throws IOException, IllegalArgumentException {
        super("ConnectionHandler-" + host);
        this.host = host;
        this.nick = nick;
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintStream(socket.getOutputStream(), true);
        instance.set(this);
        
        getLogger("core.ConnectionHandler").setLevel(App.LOG_LEVEL);
        getLogger("core.ConnectionHandler").info("Logging in...");
        if(!password.isEmpty())
            out.println("PASS " + password);
        out.println("NICK " + nick);
        out.printf("USER %s localhost %s :%s\n", nick, host, nick);
    }
    
    /**
     * Parses client command and sends it to the server
     *
     * @param cmdArgs client command plus arguments
     * @return true on success
     */
    public static boolean sendCommand(@NotNull String[] cmdArgs) {
        if (cmdArgs.length == 0 || !COMMANDS.contains(cmdArgs[0]) || cmdArgs.length != COMMAND_ARG_COUNT.get(cmdArgs[0]))
            return false;
        
        var inst = instance.getAcquire();
        switch (cmdArgs[0]) {
            case "m" -> {
                sendMessage(cmdArgs[1], cmdArgs[2]);
                MainController.print(new MessageLine("YOU", cmdArgs[2]));
            }
            case "join" -> inst.messagesToSend.add("JOIN #" + cmdArgs[1]);
            case "list" -> inst.messagesToSend.add("LIST");
            default -> getLogger("core.ConnectionHandler").severe("Command " + cmdArgs[0] + " is in the set but not defined!");
        }
        instance.setRelease(inst);
        
        return true;
    }

    
    public static void sendMessage(@NotNull String dest, @NotNull String msg) {
        synchronized (instance) {
            instance.get().messagesToSend.add("PRIVMSG " + dest + " :" + msg);
        }
    }

    /**
     * @param msg message to send
     * @return true on success, false if user isn't in any channel
     */
    public static boolean sendMessage(@NotNull String msg) {
        synchronized (instance) {
            if (instance.get().curChannel == null)
                return false;
            sendMessage("#" + instance.get().curChannel.name(), msg);
            return true;
        }
    }

    @Override
    public void run() {
        new Thread(() -> {
            String buffer;
            while(App.isRunning() && !socket.isClosed()) {
                var inst = instance.getAcquire(); // locking the instance
                if(messagesToSend.isEmpty()) { // if there are no messages to send - release
                    instance.setRelease(inst);
                    continue;
                }
                buffer = messagesToSend.poll(); // take the first message in queue
                getLogger("core.ConnectionHandler").fine("Sending message: " + buffer);
                if(buffer != null && !buffer.isEmpty())
                    out.println(buffer); // send the message
                instance.setRelease(inst); // release the instance
            }
        }, "SenderThread").start();

        while(App.isRunning()) {
            try {
                synchronized (socket) {
                    if (in.ready()) {
                        String buffer = in.readLine(); // Reading from the server
                        System.out.println(buffer);
                        if(buffer.startsWith("PING")) {
                            // If PING request is received then send PONG as quickly as possible
                            var inst = instance.getAcquire();
                            out.println("PONG" + buffer.substring(4));
                            instance.setRelease(inst);
                        } else {
                            // Submit the task of processing a command to the executor
                            // so that processes of receiving and sending messages
                            // would be interrupted
                            decoderThreadPool.submit(() -> interpretServerCommand(buffer));
                        }
                    }
                }
            } catch (IOException e) { // An error has occurred
                synchronized (socket) {
                    if (socket.isClosed()) { // Server must've closed the connection
                        Platform.runLater(() -> new Alert(Alert.AlertType.INFORMATION, "Server has closed the connection").showAndWait());
                        break;
                    }
                }
                e.printStackTrace();
            }
        }

        disconnect(); // close the connection
    }
    
    /**
     * Closes the connection with the server and switches
     * the root a starting one
     */
    public static void disconnect() {
        var inst = instance.getAcquire();
        if(inst == null) {
            instance.setRelease(null);
            return;
        }

        synchronized (inst.socket) {
            // Complete the remaining tasks
            if(inst.socket.isConnected())
                inst.decoderThreadPool.shutdown();
            else
                inst.decoderThreadPool.shutdownNow();
            try {
                if (!inst.decoderThreadPool.awaitTermination(2, TimeUnit.SECONDS))
                    throw new TimeoutException("ExecutorService timeout in the connection handler");
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                inst.socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    
        instance.setRelease(null);
        if (App.isRunning())
            App.showRoot("start");
    }
    
    /**
     * Interprets a server command and reacts accordingly.
     * If reaction to a particular command is undefined,
     * it will be logged using {@link java.util.logging.Logger Logger}
     * @param cmd command to interpret
     */
    private static void interpretServerCommand(String cmd) {
        // We have some sort of command:
        // :sender!sender@example.org PRIVMSG thisUser :hello!

        final String[] cmdParts = cmd.split(":", 3); // Firstly, we split it by the ':' character
        // The first part is empty
        final String prefix = cmdParts[1].stripTrailing(); // The second part is the prefix
        final String text = cmdParts.length > 2 ? cmdParts[2].strip() : ""; // The third part is some text
        
        final String[] prefixParts = prefix.split(" "); // Then we split the prefix
        final String
                sender = prefixParts[0].split("!", 2)[0], // The first part is the sender
                command = prefixParts[1]; // The second part is a command

        var logger = getLogger("core.ConnectionHandler.processCommand()");
        try {
            int intCommand = Integer.parseInt(command); // Check if the command is numeric
            switch (intCommand) { // If so, do the action corresponding to it
                // Just some server info
                case 1, 2, 3, 251, 265, 266 -> MainController.print(new NoticeLine("SERVER", text));
                // Start of the channel list
                case 321 -> MainController.print(new TextLine("Channels:\n"));
                // Channel info
                case 322 -> parseChannelInfo(prefix, text);
                // End of the channel list
                case 323 -> {
                    synchronized (instance) {
                        MainController.printChannels(instance.get().channels.values());
                    }
                    MainController.print(new TextLine());
                }
                // Other commands are ignored
                default -> logger.fine("Numeric command: " + intCommand + " ignored");
            }
        } catch (NumberFormatException ignored) { // The command is not numeric
            var inst = ConnectionHandler.instance.getAcquire();
            switch (command) {
                case "KICK" -> {
                    MainController.print(new WarningLine("You were kicked from channel " + inst.curChannel.name()));
                    inst.curChannel = null;
                }
                case "JOIN" -> {
                    if(sender.equals(inst.nick)) { // this client was moved to a new channel
                        final String channelName = text.substring(1);
                        inst.channels.putIfAbsent(channelName, new Channel(channelName));
                        MainController.print(new WarningLine("You have joined a channel: " + channelName));
                    } else {
                        MainController.print(new WarningLine("User " + sender + " has joined your channel."));
                    }
                }
                // Important message
                case "NOTICE" -> MainController.print(new NoticeLine(sender.equals(inst.host) ? "SERVER" : sender, text));
                // Usual message
                case "PRIVMSG" -> MainController.print(new MessageLine(sender, text));
                // Other commands are ignored
                default -> logger.warning("Command \"%s\" was ignored".formatted(command));
            }
            ConnectionHandler.instance.setRelease(inst);
        }
    }
    
    private static void parseChannelInfo(String prefix, String text) {
        int indOfChName = prefix.indexOf('#'); // Channel name starts with '#'
        int indOfUserCount = prefix.indexOf(' ', indOfChName); // ends with ' '
        String name = prefix.substring(indOfChName + 1, indOfUserCount);
        
        // Then there should be a user count
        int userCount = -1;
        try {
            userCount = Integer.parseInt(prefix.substring(++indOfUserCount));
        } catch (NumberFormatException ignored) {
            getLogger("core.ConnectionHandler").warning("User count couldn't be parsed!");
        }
        // Then, there are channel options in format [x,y,...]
        // and channel description
        var splitMsg = text.split(" ", 2);
        
        // Add this info to a temporary list
        synchronized (instance) {
            instance.get().channels.putIfAbsent(name, new Channel(name, userCount, splitMsg[0], splitMsg[1]));
        }
    }
}
