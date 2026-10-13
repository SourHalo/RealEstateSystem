import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * Parent of every screen. It provides a table, a button bar, a status line,
 * background loading with SwingWorker, central error dialogs and small helpers
 * for forms. Subclasses only supply loadData() and their own buttons.
 */
public abstract class BasePanel extends JPanel {

    private static final BigDecimal MAX_MONEY = new BigDecimal("99999999.99");

    /** Work that returns a value and may throw one of our exceptions. */
    protected interface Job<T> {
        T run() throws EstateEaseException;
    }

    /** Work with no result. */
    protected interface Task {
        void run() throws EstateEaseException;
    }

    protected final DefaultTableModel model;
    protected final JTable table;
    protected final JScrollPane scroll;
    protected final JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
    private final JLabel statusLabel = new JLabel(" ");
    private int pendingJobs = 0;
    private String flashMessage = null;

    protected BasePanel() {
        super(new BorderLayout());

        model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            // Lets the row sorter sort numbers as numbers instead of text.
            @Override
            public Class<?> getColumnClass(int column) {
                for (int r = 0; r < getRowCount(); r++) {
                    Object v = getValueAt(r, column);
                    if (v != null) {
                        return v.getClass();
                    }
                }
                return Object.class;
            }
        };

        table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Modern table appearance
        table.setRowHeight(32);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(new java.awt.Color(219, 234, 254));
        table.setSelectionForeground(new java.awt.Color(30, 41, 59));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(
                new Dimension(0, 36));

        scroll = new JScrollPane(table);
        scroll.setBorder(
                javax.swing.BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.add(buttonBar, BorderLayout.NORTH);
        south.add(statusLabel, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        addButton("Refresh", () -> refresh());
    }

    // ------------------------------------------------------------------
    // To be provided by subclasses
    // ------------------------------------------------------------------

    /** Runs on a background thread. Must not touch Swing components. */
    protected abstract Database.Table loadData() throws EstateEaseException;

    /**
     * Names of columns that should be loaded but not shown (e.g. foreign key ids).
     */
    protected String[] hiddenColumns() {
        return new String[0];
    }

    // ------------------------------------------------------------------
    // Loading and background work
    // ------------------------------------------------------------------

    public void refresh() {
        setStatus("Loading...");
        background(() -> loadData(), t -> {
            fillTable(t);
            String rows = t.rows.size() + " row(s)";
            setStatus(flashMessage == null ? rows : flashMessage + "   (" + rows + ")");
            flashMessage = null;
        });
    }

    /**
     * Runs the job on a SwingWorker thread, then hands the result to onSuccess on
     * the Swing thread.
     */
    protected <T> void background(Job<T> job, Consumer<T> onSuccess) {
        pendingJobs++;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return job.run();
            }

            @Override
            protected void done() {
                pendingJobs--;
                if (pendingJobs == 0) {
                    setCursor(Cursor.getDefaultCursor());
                }
                try {
                    onSuccess.accept(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    showError(ex.getCause()); // the exception thrown inside doInBackground
                }
            }
        }.execute();
    }

    /**
     * Runs a change on a background thread, then reloads the table and shows a
     * message.
     */
    protected void runAndRefresh(Task task, String successMessage) {
        background(() -> {
            task.run();
            return Boolean.TRUE;
        }, done -> {
            flashMessage = successMessage;
            refresh();
        });
    }

    protected void fillTable(Database.Table t) {
        model.setDataVector(t.rows.toArray(new Object[0][]), t.columns);
        for (String hidden : hiddenColumns()) {
            if (model.findColumn(hidden) >= 0) {
                table.removeColumn(table.getColumn(hidden)); // hidden in the view, still in the model
            }
        }
    }

    // ------------------------------------------------------------------
    // Buttons, selection, status
    // ------------------------------------------------------------------

    protected JButton addButton(String text, Task task) {
        JButton b = new JButton(text);
        b.addActionListener(e -> {
            try {
                task.run();
            } catch (EstateEaseException ex) {
                showError(ex);
            }
        });
        buttonBar.add(b);
        return b;
    }

    protected void setStatus(String text) {
        statusLabel.setText(" " + text);
    }

    /** Value from the selected row, looked up by column name. */
    protected Object cell(String columnName) throws EstateEaseException.Validation {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            throw new EstateEaseException.Validation("Please select a row in the table first.");
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return model.getValueAt(modelRow, model.findColumn(columnName));
    }

    protected String text(String columnName) throws EstateEaseException.Validation {
        Object v = cell(columnName);
        return v == null ? "" : v.toString();
    }

    protected int selectedId() throws EstateEaseException.Validation {
        return ((Number) cell("ID")).intValue();
    }

    // ------------------------------------------------------------------
    // Error dialog: one place, polymorphic through EstateEaseException
    // ------------------------------------------------------------------

    protected void showError(Throwable t) {
        String title;
        String message;
        int type;
        if (t instanceof EstateEaseException) {
            EstateEaseException ex = (EstateEaseException) t;
            title = ex.getCategory();
            message = ex.getMessage();
            type = ex.isUserError() ? JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE;
        } else {
            title = "Unexpected error";
            message = String.valueOf(t);
            type = JOptionPane.ERROR_MESSAGE;
            if (t != null) {
                t.printStackTrace();
            }
        }
        setStatus("Error: " + message);
        JOptionPane.showMessageDialog(this, message, title, type);
    }

    // ------------------------------------------------------------------
    // Form helpers
    // ------------------------------------------------------------------

    protected boolean showForm(String title, String[] labels, JComponent[] fields) {
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        for (int i = 0; i < labels.length; i++) {
            p.add(new JLabel(labels[i]));
            p.add(fields[i]);
        }
        int answer = JOptionPane.showConfirmDialog(this, p, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return answer == JOptionPane.OK_OPTION;
    }

    protected void showTableDialog(String title, Database.Table t) {
        DefaultTableModel m = new DefaultTableModel(t.rows.toArray(new Object[0][]), t.columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable tbl = new JTable(m);
        tbl.setAutoCreateRowSorter(true);
        JScrollPane sp = new JScrollPane(tbl);
        sp.setPreferredSize(new Dimension(620, 240));
        JOptionPane.showMessageDialog(this, sp, title, JOptionPane.PLAIN_MESSAGE);
    }

    protected static JComboBox<Database.Item> combo(List<Database.Item> items) {
        return new JComboBox<>(items.toArray(new Database.Item[0]));
    }

    protected static void select(JComboBox<Database.Item> box, int id) {
        for (int i = 0; i < box.getItemCount(); i++) {
            if (box.getItemAt(i).id == id) {
                box.setSelectedIndex(i);
                return;
            }
        }
    }

    // ------------------------------------------------------------------
    // Input parsing (all throw Validation with a clear message)
    // ------------------------------------------------------------------

    protected static String required(String value, String field) throws EstateEaseException.Validation {
        String s = value == null ? "" : value.trim();
        if (s.isEmpty()) {
            throw new EstateEaseException.Validation(field + " is required.");
        }
        return s;
    }

    /** Blank returns null when optional is true. Result has 2 decimals. */
    protected static BigDecimal parseMoney(String text, String field, boolean optional, boolean allowZero)
            throws EstateEaseException.Validation {
        String s = text == null ? "" : text.trim();
        if (s.isEmpty()) {
            if (optional) {
                return null;
            }
            throw new EstateEaseException.Validation(field + " is required.");
        }
        BigDecimal value;
        try {
            value = new BigDecimal(s);
        } catch (NumberFormatException e) {
            throw new EstateEaseException.Validation(field + " must be a number.");
        }
        if (value.signum() < 0 || (value.signum() == 0 && !allowZero)) {
            throw new EstateEaseException.Validation(
                    field + (allowZero ? " cannot be negative." : " must be greater than zero."));
        }
        value = value.setScale(2, RoundingMode.HALF_UP);
        if (value.compareTo(MAX_MONEY) > 0) {
            throw new EstateEaseException.Validation(field + " is too large.");
        }
        return value;
    }

    protected static LocalDate parseDate(String text, String field) throws EstateEaseException.Validation {
        try {
            return LocalDate.parse(text == null ? "" : text.trim());
        } catch (DateTimeParseException e) {
            throw new EstateEaseException.Validation(field + " must be a date in yyyy-MM-dd format.");
        }
    }
}
