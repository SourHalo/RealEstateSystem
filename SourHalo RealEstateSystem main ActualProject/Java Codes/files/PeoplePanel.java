import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

/**
 * One class serves both the Owners tab and the Tenants tab. It is given a
 * prototype Person (an Owner or a Tenant) and everything else is polymorphic.
 */
public class PeoplePanel extends BasePanel {

    private final Person prototype;
    private final Map<String, Comparator<Person>> sorters = new LinkedHashMap<>();
    private final JComboBox<String> sortBox;
    private volatile String sortKey = "ID";

    public PeoplePanel(Person prototype) {
        this.prototype = prototype;

        // Different ways of ordering the list, kept in a Map keyed by label.
        sorters.put("ID", Comparator.comparingInt(Person::getId));
        sorters.put("Name", Comparator.comparing(Person::getFullName, String.CASE_INSENSITIVE_ORDER));
        sorters.put("City", Comparator.comparing(Person::getCity, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Person::getFullName, String.CASE_INSENSITIVE_ORDER));

        addButton("Add " + prototype.getRole(), () -> openForm(null));
        addButton("Edit", () -> openForm(selectedPerson()));
        addButton("Delete", () -> deleteSelected());

        buttonBar.add(new JLabel("   Sort by:"));
        sortBox = new JComboBox<>(sorters.keySet().toArray(new String[0]));
        sortBox.addActionListener(e -> {
            sortKey = (String) sortBox.getSelectedItem();
            refresh();
        });
        buttonBar.add(sortBox);
    }

    @Override
    protected Database.Table loadData() throws EstateEaseException {
        List<Person> people = Database.getPeople(prototype);
        people.sort(sorters.get(sortKey));

        List<Object[]> rows = new ArrayList<>();
        for (Person p : people) {
            rows.add(new Object[]{p.getId(), p.getFullName(), p.getPhone(), p.getEmail(), p.getCity()});
        }
        return new Database.Table(new String[]{"ID", "Name", "Phone", "Email", "City"}, rows);
    }

    /** Rebuilds a Person from the selected table row. */
    private Person selectedPerson() throws EstateEaseException {
        Person p = prototype.createBlank();
        p.setId(selectedId());
        p.setFullName(text("Name"));
        p.setPhone(text("Phone"));
        p.setEmail(text("Email"));
        p.setCity(text("City"));
        return p;
    }

    /** existing == null means "add a new one". Re-opens with the typed values if validation fails. */
    private void openForm(Person existing) {
        String name = existing == null ? "" : existing.getFullName();
        String phone = existing == null ? "" : existing.getPhone();
        String email = existing == null ? "" : existing.getEmail();
        String city = existing == null ? "Pune" : existing.getCity();
        String title = (existing == null ? "Add " : "Edit ") + prototype.getRole();

        while (true) {
            JTextField fName = new JTextField(name, 22);
            JTextField fPhone = new JTextField(phone, 22);
            JTextField fEmail = new JTextField(email, 22);
            JTextField fCity = new JTextField(city, 22);

            boolean ok = showForm(title,
                    new String[]{"Full name", "Phone", "Email (optional)", "City"},
                    new JComponent[]{fName, fPhone, fEmail, fCity});
            if (!ok) {
                return; // cancelled
            }

            try {
                Person p = prototype.createBlank();
                if (existing != null) {
                    p.setId(existing.getId());
                }
                p.setFullName(fName.getText());
                p.setPhone(fPhone.getText());
                p.setEmail(fEmail.getText());
                p.setCity(fCity.getText());
                p.validate();
                runAndRefresh(() -> Database.savePerson(p), prototype.getRole() + " saved");
                return;
            } catch (EstateEaseException ex) {
                showError(ex);
                name = fName.getText();
                phone = fPhone.getText();
                email = fEmail.getText();
                city = fCity.getText();
            }
        }
    }

    private void deleteSelected() throws EstateEaseException {
        Person p = selectedPerson();
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete " + p.getFullName() + "?", "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            runAndRefresh(() -> Database.deletePerson(p), prototype.getRole() + " deleted");
        }
    }
}
