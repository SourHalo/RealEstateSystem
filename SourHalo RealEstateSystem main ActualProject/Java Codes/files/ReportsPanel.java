import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Dashboard: summary counts, properties grouped by area, and below them the
 * Property_Status_Log table filled by the audit trigger.
 */
public class ReportsPanel extends BasePanel {

    private final JPanel countsPanel = new JPanel(new GridLayout(0, 2, 8, 4));
    private final JTextArea areaText = new JTextArea(8, 40);

    public ReportsPanel() {
        countsPanel.setBorder(BorderFactory.createTitledBorder("Summary"));

        areaText.setEditable(false);
        JScrollPane areaScroll = new JScrollPane(areaText);
        areaScroll.setBorder(BorderFactory.createTitledBorder("Properties by area"));

        JPanel top = new JPanel(new GridLayout(1, 2, 10, 0));
        top.add(countsPanel);
        top.add(areaScroll);
        add(top, BorderLayout.NORTH);

        scroll.setBorder(BorderFactory.createTitledBorder("Property status audit log (latest 200)"));
    }

    @Override
    protected Database.Table loadData() throws EstateEaseException {
        return Database.getAuditLog();
    }

    /** Loads the audit log (via the parent) and the two summary sections, each on its own worker. */
    @Override
    public void refresh() {
        super.refresh();
        background(() -> Database.getDashboardCounts(), counts -> showCounts(counts));
        background(() -> Database.getProperties(null, null), t -> showAreas(t));
    }

    private void showCounts(Map<String, Object> counts) {
        countsPanel.removeAll();
        for (Map.Entry<String, Object> entry : counts.entrySet()) {
            countsPanel.add(new JLabel(entry.getKey() + ":"));
            JLabel value = new JLabel(String.valueOf(entry.getValue()));
            value.setFont(value.getFont().deriveFont(Font.BOLD));
            countsPanel.add(value);
        }
        countsPanel.revalidate();
        countsPanel.repaint();
    }

    /** Groups properties by area in a TreeMap (sorted by area name). */
    private void showAreas(Database.Table t) {
        List<String> cols = Arrays.asList(t.columns);
        int addressCol = cols.indexOf("Address");
        int areaCol = cols.indexOf("Area");
        int statusCol = cols.indexOf("Status");

        Map<String, List<String>> byArea = new TreeMap<>();
        for (Object[] row : t.rows) {
            byArea.computeIfAbsent(String.valueOf(row[areaCol]), k -> new ArrayList<>())
                    .add(row[addressCol] + "  [" + row[statusCol] + "]");
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : byArea.entrySet()) {
            sb.append(entry.getKey()).append(" (").append(entry.getValue().size()).append(")\n");
            for (String line : entry.getValue()) {
                sb.append("    - ").append(line).append("\n");
            }
        }
        areaText.setText(sb.length() == 0 ? "No properties yet." : sb.toString());
        areaText.setCaretPosition(0);
    }
}
