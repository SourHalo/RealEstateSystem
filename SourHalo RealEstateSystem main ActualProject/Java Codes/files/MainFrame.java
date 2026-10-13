
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

public class MainFrame extends JFrame {

    private static final long EXPIRY_INTERVAL_MILLIS = 5 * 60 * 1000L;

    private static final Color NAVY = new Color(20, 32, 56);
    private static final Color NAVY_HOVER = new Color(37, 53, 82);
    private static final Color BLUE = new Color(59, 130, 246);
    private static final Color PAGE_BG = new Color(245, 247, 251);
    private static final Color TEXT = new Color(31, 41, 55);
    private static final Color MUTED = new Color(148, 163, 184);

    private final List<BasePanel> panels = new ArrayList<>();
    private final List<JButton> navButtons = new ArrayList<>();

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);

    private final JLabel pageTitle = new JLabel("Dashboard");
    private final JLabel statusBar =
            new JLabel("  Connected. Waiting for the first expiry check...");

    private final ExpiryScheduler scheduler;

    private int selectedPage = 0;

    public MainFrame() {
        super("EstateEase | Property Rental Management");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 650));

        // Build the existing application screens.
        addPage("Dashboard", new ReportsPanel());
        addPage("Properties", new PropertyPanel());
        addPage("Agreements & Payments", new AgreementPanel());
        addPage("Maintenance", new MaintenancePanel());
        addPage("Owners", new PeoplePanel(new Owner()));
        addPage("Tenants", new PeoplePanel(new Tenant()));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(PAGE_BG);

        root.add(createSidebar(), BorderLayout.WEST);
        root.add(createMainArea(), BorderLayout.CENTER);

        setContentPane(root);

        statusBar.setOpaque(true);
        statusBar.setBackground(Color.WHITE);
        statusBar.setForeground(new Color(100, 116, 139));
        statusBar.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(
                        1, 0, 0, 0, new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        root.add(statusBar, BorderLayout.SOUTH);

        setSize(1250, 760);
        setLocationRelativeTo(null);

        scheduler = new ExpiryScheduler(
                EXPIRY_INTERVAL_MILLIS,
                (expired, error) -> onExpiryCheck(expired, error)
        );
        scheduler.start();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                scheduler.stop();
            }
        });

        showPage(0);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(235, 0));
        sidebar.setBackground(NAVY);

        JPanel branding = new JPanel(new BorderLayout());
        branding.setBackground(NAVY);
        branding.setBorder(
                BorderFactory.createEmptyBorder(25, 20, 25, 12));

        JLabel logo = new JLabel("  EstateEase");
        logo.setForeground(Color.WHITE);
        logo.setFont(new Font("SansSerif", Font.BOLD, 23));

        JLabel subtitle = new JLabel("  PROPERTY MANAGEMENT");
        subtitle.setForeground(MUTED);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 10));

        JPanel brandText = new JPanel(new BorderLayout(0, 8));
        brandText.setBackground(NAVY);
        brandText.add(logo, BorderLayout.NORTH);
        brandText.add(subtitle, BorderLayout.SOUTH);

        branding.add(brandText, BorderLayout.CENTER);
        sidebar.add(branding, BorderLayout.NORTH);

        JPanel navigation = new JPanel();
        navigation.setBackground(NAVY);
        navigation.setBorder(
                BorderFactory.createEmptyBorder(12, 10, 12, 10));
        navigation.setLayout(new javax.swing.BoxLayout(
                navigation, javax.swing.BoxLayout.Y_AXIS));

        String[] names = {
            "Dashboard",
            "Properties",
            "Agreements & Payments",
            "Maintenance",
            "Owners",
            "Tenants"
        };

        String[] icons = {"⌂", "▦", "▤", "⚒", "♙", "♧"};

        for (int i = 0; i < names.length; i++) {
            final int pageIndex = i;

            JButton button = new JButton(
                    icons[i] + "   " + names[i]);

            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setFont(new Font("SansSerif", Font.PLAIN, 13));
            button.setForeground(new Color(226, 232, 240));
            button.setBackground(NAVY);
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            button.setOpaque(true);
            button.setCursor(
                    java.awt.Cursor.getPredefinedCursor(
                            java.awt.Cursor.HAND_CURSOR));
            button.setMargin(new Insets(12, 12, 12, 8));
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
            button.setAlignmentX(LEFT_ALIGNMENT);

            button.addActionListener(e -> showPage(pageIndex));

            navButtons.add(button);
            navigation.add(button);
            navigation.add(javax.swing.Box.createVerticalStrut(5));
        }

        sidebar.add(navigation, BorderLayout.CENTER);

        JLabel footer = new JLabel(
                "<html><div style='padding:12px'>"
                + "ESTATEEASE<br>Management System"
                + "</div></html>"
        );
        footer.setForeground(MUTED);
        footer.setFont(new Font("SansSerif", Font.PLAIN, 11));
        sidebar.add(footer, BorderLayout.SOUTH);

        return sidebar;
    }

    private JPanel createMainArea() {
        JPanel main = new JPanel(new BorderLayout(0, 0));
        main.setBackground(PAGE_BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(
                        0, 0, 1, 0, new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(18, 25, 18, 25)
        ));

        pageTitle.setFont(new Font("SansSerif", Font.BOLD, 23));
        pageTitle.setForeground(TEXT);

        JLabel headerSubtitle = new JLabel("Manage your rental portfolio");
        headerSubtitle.setForeground(new Color(107, 114, 128));
        headerSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JPanel titles = new JPanel(new BorderLayout(0, 5));
        titles.setBackground(Color.WHITE);
        titles.add(pageTitle, BorderLayout.NORTH);
        titles.add(headerSubtitle, BorderLayout.SOUTH);

        JLabel badge = new JLabel("  ●  EstateEase  ");
        badge.setFont(new Font("SansSerif", Font.BOLD, 12));
        badge.setForeground(new Color(22, 101, 52));
        badge.setBackground(new Color(220, 252, 231));
        badge.setOpaque(true);
        badge.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));

        header.add(titles, BorderLayout.WEST);
        header.add(badge, BorderLayout.EAST);

        contentPanel.setBackground(PAGE_BG);

        main.add(header, BorderLayout.NORTH);
        main.add(contentPanel, BorderLayout.CENTER);

        return main;
    }

    private void addPage(String name, BasePanel panel) {
        panels.add(panel);

        // Keep each screen instance and its existing event handlers.
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        contentPanel.add(panel, name);
    }

    private void showPage(int index) {
        if (index < 0 || index >= panels.size()) {
            return;
        }

        selectedPage = index;
        String[] names = {
            "Dashboard", "Properties", "Agreements & Payments",
            "Maintenance", "Owners", "Tenants"
        };

        pageTitle.setText(names[index]);
        cardLayout.show(contentPanel, names[index]);

        for (int i = 0; i < navButtons.size(); i++) {
            JButton button = navButtons.get(i);
            boolean selected = (i == index);

            button.setBackground(selected ? BLUE : NAVY);
            button.setForeground(Color.WHITE);
            button.setFont(new Font(
                    "SansSerif",
                    selected ? Font.BOLD : Font.PLAIN,
                    13
            ));
        }

        panels.get(index).refresh();
    }

    private void onExpiryCheck(int expired, String error) {
        String time = LocalTime.now().withNano(0).toString();

        if (error != null) {
            statusBar.setText(
                    "  Automatic expiry check failed at "
                    + time + ": " + error
            );
            return;
        }

        statusBar.setText(
                "  Last automatic expiry check: " + time
                + "  |  Agreements expired this session: "
                + scheduler.getTotalExpired()
        );

        if (expired > 0) {
            for (BasePanel panel : panels) {
                panel.refresh();
            }
        }
    }
}
