import javax.swing.*;
import java.awt.*;

public class UnoMain extends JFrame {
    public static void main(String[] args) {
        EventQueue.invokeLater(UnoMain::new);
    }

    private UnoMain() {
        add(new UnoDisplay());

        setSize(800, 600);
        setMinimumSize(new Dimension(600, 480));

        setTitle("Uno");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setVisible(true);
    }
}