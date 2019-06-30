package display;

import manager.local.ComputerManager;
import manager.web.ClientManager;
import manager.web.Message;
import manager.web.ServerManager;
import manager.web.WebManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Uno extends JFrame {
    public static final int VERSION = 3_01;
    public static final int BACK_COMPAT_VERSION = 3_00;

    public static final UnoPanel PANEL = new UnoPanel();

    private static final File HOME_DIRECTORY = new File(System.getProperty("user.home"));

    public static final File UNO_DIRECTORY = new File(HOME_DIRECTORY, ".unoGame");

    private static final File LAST_ADDRESS_FILE = new File(UNO_DIRECTORY, "lastOnlineAddress");
    private static final Path LAST_ADDRESS_PATH = LAST_ADDRESS_FILE.toPath();

    public static boolean isCompatible(String versionInfo) {
        if (versionInfo == null) return false;
        String[] parts = versionInfo.split(" ");
        try {
            int version = Integer.parseInt(parts[0]);
            return version >= BACK_COMPAT_VERSION;
        } catch (NumberFormatException ignored) {}
        return false;
    }

    private static String formatVersion(String version) {
        int pointIndex = version.length()-2;
        return version.substring(0, pointIndex)+'.'+version.substring(pointIndex);
    }

    public static void main(String[] args) {
        UNO_DIRECTORY.mkdir();
        int mode = JOptionPane.showOptionDialog(
                null,
                "Do you want to play against a computer or online?",
                "Select Mode",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                new String[] {"Computer", "Online"},
                null);
        switch (mode) {
        case 0:
            UnoPanel.setOpponent(new ComputerManager());
            break;
        case 1:
            prepareServer();
            break;
        default:
            return;
        }
        EventQueue.invokeLater(Uno::new);
    }

    private static void prepareServer() {
        loadName(true);
        String lastAddress = loadAddress();
        while (true) {
            String setup = (String) JOptionPane.showInputDialog(
                    null,
                    "Where do you want to connect to?",
                    "Enter IP",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    lastAddress);
            if (setup == null) System.exit(0);
            if (setup.equalsIgnoreCase("resetName")) {
                loadName(false);
                continue;
            }
            String[] parts = setup.split("@");
            String ip = parts[parts.length - 1];
            if (parts.length <= 2 && !ip.isEmpty()) {
                saveAddress(setup);
                int port = parts.length == 1 ? WebManager.DEFAULT_PORT : Integer.parseInt(parts[0]);
                boolean server = ip.equalsIgnoreCase("server");
                UnoPanel.setOpponent(server ? new ServerManager(port) : new ClientManager(ip, port));
                return;
            }
            JOptionPane.showMessageDialog(
                    null,
                    "Expected an address formatted like one of:\n" +
                            "resetName // to change your name\n" +
                            "server // to start a server \n" +
                            "127.0.0.1\n" +
                            WebManager.DEFAULT_PORT + "@server\n" +
                            WebManager.DEFAULT_PORT + "@127.0.0.1",
                    "Invalid Port",
                    JOptionPane.PLAIN_MESSAGE);
        }
    }

    private static void loadName(boolean fromFile) {
        File nameFile = new File(UNO_DIRECTORY, "onlineName");
        Path namePath = nameFile.toPath();
        String name = "";
        if (fromFile) {
            try {
                if (nameFile.exists()) {
                    name = new String(Files.readAllBytes(namePath), "utf-8").trim();
                }
            } catch (IOException ignored) {}
        }
        if (name.isEmpty()) {
            while (true) {
                name = JOptionPane.showInputDialog(null, "What is your name?", "Name", JOptionPane.PLAIN_MESSAGE);
                if (name == null) {
                    System.exit(0);
                } else if (!name.isEmpty()) {
                    break;
                }
                JOptionPane.showMessageDialog(null, "Name cannot be empty!", "Invalid Name", JOptionPane.ERROR_MESSAGE);
            }
            try {
                Files.write(namePath, name.getBytes("utf-8"));
            } catch (IOException ignored) {}
        }
        UnoPanel.setChatName(name);
    }

    private static String loadAddress() {
        try {
            if (LAST_ADDRESS_FILE.exists()) {
                return new String(Files.readAllBytes(LAST_ADDRESS_PATH), "utf-8").trim();
            }
        } catch (IOException ignored) {}
        return "";
    }

    private static void saveAddress(String address) {
        try {
            Files.write(LAST_ADDRESS_PATH, address.getBytes("utf-8"));
        } catch (IOException ignored) {}
    }

    public static JDialog connect() {
        JDialog dialog = new JDialog((Frame) null, "Attempting to Connect...");
        JOptionPane pane = new JOptionPane();
        pane.setMessage("Attempting to connect to opponent...");
        pane.setMessageType(JOptionPane.PLAIN_MESSAGE);
        pane.setOptions(new String[] {"Cancel"});
        pane.addPropertyChangeListener(e -> {
            if (e.getSource() == pane && e.getPropertyName().equals(JOptionPane.VALUE_PROPERTY)) {
                dialog.dispose();
                System.exit(1);
            }
        });
        dialog.setContentPane(pane);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        return dialog;
    }

    private static void asyncErrorDialog(String message, String title) {
        JDialog dialog = new JDialog((Frame) null, title);
        JOptionPane pane = new JOptionPane();
        pane.setMessage(message);
        pane.setMessageType(JOptionPane.ERROR_MESSAGE);
        pane.setOptionType(JOptionPane.DEFAULT_OPTION);
        pane.addPropertyChangeListener(e -> {
            if (e.getSource() == pane && e.getPropertyName().equals(JOptionPane.VALUE_PROPERTY)) {
                dialog.dispose();
                System.exit(1);
            }
        });
        dialog.setContentPane(pane);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(1);
            }
        });
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setModalityType(JDialog.ModalityType.MODELESS);
        dialog.setVisible(true);
    }

    public static void failServerStart() {
        asyncErrorDialog("Failed to start server. Is the game already running?", "Failed to Start Server");
    }

    public static void failConnect() {
        asyncErrorDialog("Failed to connect to server.", "Server Offline");
    }

    public static void disconnect() {
        asyncErrorDialog("Disconnected from opponent.", "Disconnected");
    }

    public static void desynchronized() {
        asyncErrorDialog("The game has become desynchronized with the other player.", "Desynchronized");
    }

    public static void opponentClosed() {
        asyncErrorDialog("Your opponent has closed the game.", "Disconnected");
    }

    public static void opponentIncompatible(String version) {
        JOptionPane.showMessageDialog(
                null,
                "Your opponent is using an old version of Uno that is incompatible with this one.",
                String.format(
                        "Incompatible Version (%s < %s)",
                        formatVersion(version),
                        formatVersion(Integer.toString(BACK_COMPAT_VERSION))),
                JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    public static void playerIncompatible(String version) {
        asyncErrorDialog(
                "You are using an old version of Uno that is incompatible with your opponent.",
                String.format(
                        "Incompatible Version (%s < %s)",
                        formatVersion(Integer.toString(VERSION)),
                        formatVersion(version)));
    }

    public static void unknownMessage(String kind) {
        asyncErrorDialog(
                "An unknown message type was received from your opponent.",
                String.format("Unknown Message Type (%s)", kind));
    }

    public static void invalidMessage(Message message) {
        asyncErrorDialog(
                "An invalid message was received from your opponent:\n" + message,
                "Invalid Message");
    }

    public static void missingMessage(String kind) {
        asyncErrorDialog(
                "Expected a message of type \""+kind+"\", but it never came.",
                "Missing Message");
    }

    private Uno() {
        add(PANEL);
        addKeyListener(PANEL);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                PANEL.onClose();
            }
        });

        // To enable TAB key detection
        setFocusTraversalKeysEnabled(false);

        setSize(800, 600);
        setMinimumSize(new Dimension(600, 480));

        setTitle("Uno");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setVisible(true);
    }
}