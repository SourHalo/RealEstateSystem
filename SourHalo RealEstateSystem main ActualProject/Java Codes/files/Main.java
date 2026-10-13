import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.concurrent.ExecutionException;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

/** Starts the application: shows the login dialog, then the main window. */
public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // keep the default look and feel
        }
        SwingUtilities.invokeLater(() -> new LoginDialog().setVisible(true));
    }
}

/** Asks for the MySQL connection details and tests them on a background thread. */
class LoginDialog extends JDialog {

    private final JTextField hostField = new JTextField("localhost");
    private final JTextField portField = new JTextField("3306");
    private final JTextField dbField = new JTextField("project");
    private final JTextField userField = new JTextField("Leomessi@10");
    private final JPasswordField passwordField = new JPasswordField();
    private final JLabel messageLabel = new JLabel(" ");
    private final JButton connectButton = new JButton("Connect");

    LoginDialog() {
        super((Frame) null, "EstateEase - Connect to MySQL", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Host"));
        form.add(hostField);
        form.add(new JLabel("Port"));
        form.add(portField);
        form.add(new JLabel("Database"));
        form.add(dbField);
        form.add(new JLabel("User"));
        form.add(userField);
        form.add(new JLabel("Password"));
        form.add(passwordField);
        form.setBorder(new EmptyBorder(12, 12, 4, 12));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(new EmptyBorder(0, 12, 12, 12));
        bottom.add(messageLabel, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(connectButton);
        bottom.add(buttons, BorderLayout.SOUTH);

        add(form, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(connectButton);
        connectButton.addActionListener(e -> connect());

        setPreferredSize(new Dimension(380, 300));
        pack();
        setLocationRelativeTo(null);
    }

    private void connect() {
        connectButton.setEnabled(false);
        messageLabel.setText("Connecting...");

        final String host = hostField.getText();
        final String port = portField.getText();
        final String db = dbField.getText();
        final String user = userField.getText();
        final String password = new String(passwordField.getPassword());

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                Database.connect(host, port, db, user, password);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // rethrows anything from doInBackground
                    dispose();
                    new MainFrame().setVisible(true);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    String text = cause.getMessage() == null ? cause.toString() : cause.getMessage();
                    text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
                    messageLabel.setText("<html><body style='width:320px'>" + text + "</body></html>");
                    connectButton.setEnabled(true);
                    pack();
                }
            }
        }.execute();
    }
}
