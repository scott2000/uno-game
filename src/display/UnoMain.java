package display;

import manager.OpponentManager;
import manager.local.ComputerManager;
import manager.web.ClientManager;
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

public class UnoMain extends JFrame {
    private static final File HOME_DIRECTORY = new File(System.getProperty("user.home"));
    public static final File UNO_DIRECTORY = new File(HOME_DIRECTORY, ".unoGame");
    private static OpponentManager opponent;

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
        mode: switch (mode) {
        case 0:
            opponent = new ComputerManager();
            break;
        case 1:
            File lastAddressFile = new File(UNO_DIRECTORY, "lastOnlineAddress");
            Path lastAddressPath = lastAddressFile.toPath();
            String lastAddress = "";
            try {
                if (lastAddressFile.exists()) {
                    lastAddress = new String(Files.readAllBytes(lastAddressPath), "utf-8").trim();
                }
            } catch (IOException ignored) {}
            while (true) {
                String setup = (String) JOptionPane.showInputDialog(
                        null,
                        "Where do you want to connect to?",
                        "Enter IP",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        lastAddress);
                if (setup == null) return;
                String[] parts = setup.split("@");
                String ip = parts[parts.length-1];
                if (parts.length <= 2 && !ip.isEmpty()) {
                    try {
                        Files.write(lastAddressPath, setup.getBytes("utf-8"));
                    } catch (IOException ignored) {}
                    try {
                        int port = parts.length == 1 ? WebManager.DEFAULT_PORT : Integer.parseInt(parts[0]);
                        if (ip.equalsIgnoreCase("server")) {
                            opponent = new ServerManager(port);
                        } else {
                            opponent = new ClientManager(ip, port);
                        }
                        break mode;
                    } catch (Exception ignored) {}
                }
                JOptionPane.showMessageDialog(
                        null,
                        "Expected an address formatted like one of:\n" +
                                "server\n" +
                                "127.0.0.1\n" +
                                WebManager.DEFAULT_PORT+"@server\n" +
                                WebManager.DEFAULT_PORT+"@127.0.0.1",
                        "Invalid Port",
                        JOptionPane.PLAIN_MESSAGE);
            }
        default:
            return;
        }
        EventQueue.invokeLater(UnoMain::new);
    }

    private static JDialog cancelDialog(String message, String title, int messageType) {
        JDialog dialog = new JDialog((Frame) null, title);
        JOptionPane pane = new JOptionPane();
        pane.setMessage(message);
        pane.setMessageType(messageType);
        pane.setOptions(new String[] {"Cancel"});
        pane.addPropertyChangeListener(e -> {
            if (e.getSource() == pane && e.getPropertyName().equals(JOptionPane.VALUE_PROPERTY)) {
                dialog.setVisible(false);
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

    public static JDialog connect() {
        return cancelDialog(
                "Attempting to connect to opponent...",
                "Attempting to Connect...",
                JOptionPane.PLAIN_MESSAGE);
    }

    public static JDialog reconnect() {
        return cancelDialog(
                "Disconnected from opponent. Attempting to reconnect...",
                "Attempting to Reconnect...",
                JOptionPane.WARNING_MESSAGE);
    }

    public static void failConnect() {
        JOptionPane.showMessageDialog(null, "Failed to connect to server.", "Server Offline", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    public static void disconnect() {
        JOptionPane.showMessageDialog(null, "The connection to your opponent has failed.", "Disconnected", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    public static void desynchronized() {
        JOptionPane.showMessageDialog(null, "The game has become desynchronized with the other player.", "Desynchronized", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    public static void opponentClosed() {
        JOptionPane.showMessageDialog(null, "Your opponent has closed the game.", "Disconnected", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    private UnoMain() {
        UnoPanel panel = new UnoPanel(opponent);
        opponent = null;

        add(panel);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                panel.onClose();
            }
        });

        setSize(800, 600);
        setMinimumSize(new Dimension(600, 480));

        setTitle("Uno");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setVisible(true);
    }
}