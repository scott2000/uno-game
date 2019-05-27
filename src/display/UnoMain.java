package display;

import javax.swing.*;
import java.awt.*;

public class UnoMain extends JFrame {
    private static UnoMain instance;

    public static void main(String[] args) {
        EventQueue.invokeLater(UnoMain::new);
    }

    public static void desynchronized() {
        JOptionPane.showMessageDialog(instance, "The game has become desynchronized with the other player.", "Desynchronized", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    public static void failConnect() {
        JOptionPane.showMessageDialog(instance, "The connection to other player failed!", "Connection Failed", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    private UnoMain() {
        instance = this;

        add(new UnoDisplay());

        setSize(800, 600);
        setMinimumSize(new Dimension(600, 480));

        setTitle("Uno");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setVisible(true);
    }
}