import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Property list with Status/Area filters plus add, edit and delete. */
public class PropertyPanel extends BasePanel {

    private final JComboBox<String> statusBox =
            new JComboBox<>(new String[]{"All", "Available", "Rented", "Under Maintenance"});
    private final JComboBox<String> areaBox = new JComboBox<>(new String[]{"All"});
    private volatile String filterStatus = null; // read by the background thread
    private volatile String filterArea = null;
    private boolean updatingAreas = false;

    public PropertyPanel() {
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        filters.add(new JLabel("Status:"));
        filters.add(statusBox);
        filters.add(new JLabel("Area:"));
        filters.add(areaBox);
        add(filters, BorderLayout.NORTH);

        statusBox.addActionListener(e -> {
            filterStatus = valueOrNull(statusBox);
            reloadTable();
        });
        areaBox.addActionListener(e -> {
            if (!updatingAreas) {
                filterArea = valueOrNull(areaBox);
                reloadTable();
            }
        });

        addButton("Add Property", () -> openForm(false));
        addButton("Edit", () -> openForm(true));
        addButton("Delete", () -> deleteSelected());
    }

    private static String valueOrNull(JComboBox<String> box) {
        String v = (String) box.getSelectedItem();
        return v == null || "All".equals(v) ? null : v;
    }

    @Override
    protected Database.Table loadData() throws EstateEaseException {
        return Database.getProperties(filterStatus, filterArea);
    }

    @Override
    protected String[] hiddenColumns() {
        return new String[]{"OwnerID", "TypeID"};
    }

    /** Reload only the table (used by the filter boxes). */
    private void reloadTable() {
        super.refresh();
    }

    /** Reload the table and also the list of areas for the Area filter. */
    @Override
    public void refresh() {
        super.refresh();
        background(() -> Database.getAreas(), areas -> fillAreas(areas));
    }

    private void fillAreas(Set<String> areas) {
        updatingAreas = true;
        Object selected = areaBox.getSelectedItem();
        areaBox.removeAllItems();
        areaBox.addItem("All");
        for (String a : areas) {
            areaBox.addItem(a);
        }
        if (selected != null) {
            areaBox.setSelectedItem(selected);
        }
        updatingAreas = false;
    }

    // ------------------------------------------------------------------
    // Add / edit / delete
    // ------------------------------------------------------------------

    private void openForm(boolean edit) throws EstateEaseException {
        final int id = edit ? selectedId() : 0;
        final String address = edit ? text("Address") : "";
        final String area = edit ? text("Area") : "";
        final String city = edit ? text("City") : "Pune";
        final String rent = edit ? text("Rent") : "";
        final int ownerId = edit ? ((Number) cell("OwnerID")).intValue() : -1;
        final int typeId = edit ? ((Number) cell("TypeID")).intValue() : -1;

        background(() -> Database.getPropertyLookups(),
                lookups -> showPropertyForm(id, lookups, address, area, city, rent, ownerId, typeId));
    }

    private void showPropertyForm(int id, Map<String, List<Database.Item>> lookups, String address, String area,
                          String city, String rent, int ownerId, int typeId) {
        JComboBox<Database.Item> owners = combo(lookups.get("owners"));
        JComboBox<Database.Item> types = combo(lookups.get("types"));
        select(owners, ownerId);
        select(types, typeId);
        JTextField fAddress = new JTextField(address, 22);
        JTextField fArea = new JTextField(area, 22);
        JTextField fCity = new JTextField(city, 22);
        JTextField fRent = new JTextField(rent, 22);

        boolean ok = showForm(id == 0 ? "Add Property" : "Edit Property",
                new String[]{"Owner", "Type", "Address", "Area", "City", "Monthly rent"},
                new JComponent[]{owners, types, fAddress, fArea, fCity, fRent});
        if (!ok) {
            return;
        }

        try {
            final Database.Item owner = (Database.Item) owners.getSelectedItem();
            final Database.Item type = (Database.Item) types.getSelectedItem();
            if (owner == null || type == null) {
                throw new EstateEaseException.Validation("An owner and a property type are required.");
            }
            final String a = required(fAddress.getText(), "Address");
            final String ar = required(fArea.getText(), "Area");
            final String c = fCity.getText().trim().isEmpty() ? "Pune" : fCity.getText().trim();
            final BigDecimal r = parseMoney(fRent.getText(), "Monthly rent", false, false);
            runAndRefresh(() -> Database.saveProperty(id, owner.id, type.id, a, ar, c, r), "Property saved");
        } catch (EstateEaseException ex) {
            showError(ex);
            Database.Item o = (Database.Item) owners.getSelectedItem();
            Database.Item t = (Database.Item) types.getSelectedItem();
            showPropertyForm(id, lookups, fAddress.getText(), fArea.getText(), fCity.getText(), fRent.getText(),
                    o == null ? -1 : o.id, t == null ? -1 : t.id);
        }
    }

    private void deleteSelected() throws EstateEaseException {
        final int id = selectedId();
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete property #" + id + "?", "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            runAndRefresh(() -> Database.deleteProperty(id), "Property deleted");
        }
    }
}
