import java.awt.Dialog;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Maintenance work orders (assign / complete) plus a pop-up to manage
 * contractors. The contractor pop-up is another BasePanel subclass.
 */
public class MaintenancePanel extends BasePanel {

    public MaintenancePanel() {
        addButton("Assign Work Order", () -> openAssignForm());
        addButton("Complete Work Order", () -> openCompleteForm());
        addButton("Manage Contractors", () -> openContractors());
    }

    @Override
    protected Database.Table loadData() throws EstateEaseException {
        return Database.getWorkOrders();
    }

    // ------------------------------------------------------------------
    // Assign
    // ------------------------------------------------------------------

    private void openAssignForm() {
        String today = LocalDate.now().toString();
        background(() -> Database.getWorkOrderLookups(),
                lookups -> showAssignForm(lookups, -1, -1, "", today, ""));
    }

    private void showAssignForm(Map<String, List<Database.Item>> lookups, int propertyId, int contractorId,
                                String issue, String date, String cost) {
        if (lookups.get("properties").isEmpty() || lookups.get("contractors").isEmpty()) {
            showError(new EstateEaseException.Validation("You need at least one property and one contractor."));
            return;
        }

        JComboBox<Database.Item> properties = combo(lookups.get("properties"));
        JComboBox<Database.Item> contractors = combo(lookups.get("contractors"));
        select(properties, propertyId);
        select(contractors, contractorId);
        JTextField fIssue = new JTextField(issue, 22);
        JTextField fDate = new JTextField(date, 22);
        JTextField fCost = new JTextField(cost, 22);

        boolean ok = showForm("Assign Work Order",
                new String[]{"Property", "Contractor", "Issue", "Request date (yyyy-MM-dd)",
                        "Cost (blank = contractor's charge)"},
                new JComponent[]{properties, contractors, fIssue, fDate, fCost});
        if (!ok) {
            return;
        }

        Database.Item property = (Database.Item) properties.getSelectedItem();
        Database.Item contractor = (Database.Item) contractors.getSelectedItem();
        try {
            String issueText = required(fIssue.getText(), "Issue description");
            if (issueText.length() > 255) {
                throw new EstateEaseException.Validation("Issue description can be at most 255 characters.");
            }
            LocalDate requestDate = parseDate(fDate.getText(), "Request date");
            BigDecimal costValue = parseMoney(fCost.getText(), "Cost", true, true);

            background(() -> Database.assignWorkOrder(property.id, contractor.id, issueText, requestDate, costValue),
                    workOrderId -> {
                        setStatus("Work order #" + workOrderId + " assigned");
                        refresh();
                    });
        } catch (EstateEaseException ex) {
            showError(ex);
            showAssignForm(lookups, property.id, contractor.id, fIssue.getText(), fDate.getText(), fCost.getText());
        }
    }

    // ------------------------------------------------------------------
    // Complete
    // ------------------------------------------------------------------

    private void openCompleteForm() throws EstateEaseException {
        showCompleteForm(selectedId(), text("Cost"));
    }

    private void showCompleteForm(int workOrderId, String cost) {
        JTextField fCost = new JTextField(cost, 22);
        boolean ok = showForm("Complete Work Order #" + workOrderId,
                new String[]{"Final cost (blank = keep current)"},
                new JComponent[]{fCost});
        if (!ok) {
            return;
        }
        try {
            BigDecimal finalCost = parseMoney(fCost.getText(), "Final cost", true, true);
            runAndRefresh(() -> Database.completeWorkOrder(workOrderId, finalCost),
                    "Work order #" + workOrderId + " completed");
        } catch (EstateEaseException ex) {
            showError(ex);
            showCompleteForm(workOrderId, fCost.getText());
        }
    }

    // ------------------------------------------------------------------
    // Contractors pop-up
    // ------------------------------------------------------------------

    private void openContractors() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Contractors",
                Dialog.ModalityType.APPLICATION_MODAL);
        ContractorPanel panel = new ContractorPanel();
        dialog.add(panel);
        dialog.setSize(800, 450);
        dialog.setLocationRelativeTo(this);
        panel.refresh();
        dialog.setVisible(true); // blocks until the dialog is closed
        refresh();               // contractor names may have changed
    }

    /** Same base class, different table: shows how much BasePanel gives for free. */
    private static class ContractorPanel extends BasePanel {

        ContractorPanel() {
            addButton("Add Contractor", () -> showContractorForm(0, "", "", "", ""));
            addButton("Edit", () -> showContractorForm(selectedId(), text("Name"), text("Specialization"),
                    text("Phone"), text("Charges")));
            addButton("Delete", () -> deleteSelected());
        }

        @Override
        protected Database.Table loadData() throws EstateEaseException {
            return Database.getContractors();
        }

        private void showContractorForm(int id, String name, String specialization, String phone, String charges) {
            JTextField fName = new JTextField(name, 22);
            JTextField fSpec = new JTextField(specialization, 22);
            JTextField fPhone = new JTextField(phone, 22);
            JTextField fCharges = new JTextField(charges, 22);

            boolean ok = showForm(id == 0 ? "Add Contractor" : "Edit Contractor",
                    new String[]{"Name", "Specialization", "Phone", "Charges per job"},
                    new JComponent[]{fName, fSpec, fPhone, fCharges});
            if (!ok) {
                return;
            }

            try {
                String n = required(fName.getText(), "Contractor name");
                String s = required(fSpec.getText(), "Specialization");
                String p = fPhone.getText().trim();
                if (n.length() > 100 || s.length() > 50) {
                    throw new EstateEaseException.Validation("Name (max 100) or specialization (max 50) is too long.");
                }
                if (!Person.isValidPhone(p)) {
                    throw new EstateEaseException.Validation(
                            "Phone must be 10 to 14 digits, optionally starting with +.");
                }
                BigDecimal c = parseMoney(fCharges.getText(), "Charges per job", true, true);
                BigDecimal charge = c == null ? new BigDecimal("0.00") : c;
                runAndRefresh(() -> Database.saveContractor(id, n, s, p, charge), "Contractor saved");
            } catch (EstateEaseException ex) {
                showError(ex);
                showContractorForm(id, fName.getText(), fSpec.getText(), fPhone.getText(), fCharges.getText());
            }
        }

        private void deleteSelected() throws EstateEaseException {
            final int id = selectedId();
            int answer = JOptionPane.showConfirmDialog(this,
                    "Delete contractor #" + id + "?", "Confirm delete", JOptionPane.YES_NO_OPTION);
            if (answer == JOptionPane.YES_OPTION) {
                runAndRefresh(() -> Database.deleteContractor(id), "Contractor deleted");
            }
        }
    }
}
