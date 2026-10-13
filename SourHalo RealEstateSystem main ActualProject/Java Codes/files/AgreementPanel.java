import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

/**
 * Rental agreements: book a property, terminate/expire, and record or view
 * payments. All changes go through the stored procedures.
 */
public class AgreementPanel extends BasePanel {

    public AgreementPanel() {
        addButton("Book Property", () -> openBookForm());
        addButton("Terminate", () -> endAgreement("Terminated"));
        addButton("Mark Expired", () -> endAgreement("Expired"));
        addButton("Record Payment", () -> openPaymentForm());
        addButton("View Payments", () -> viewPayments());
        addButton("Run Expiry Check", () -> runExpiryCheck());
    }

    @Override
    protected Database.Table loadData() throws EstateEaseException {
        return Database.getAgreements();
    }

    // ------------------------------------------------------------------
    // Booking
    // ------------------------------------------------------------------

    private void openBookForm() {
        String start = LocalDate.now().toString();
        String end = LocalDate.now().plusMonths(11).toString();
        background(() -> Database.getBookingLookups(), lookups -> showBookForm(lookups, -1, -1, start, end, ""));
    }

    private void showBookForm(Map<String, List<Database.Item>> lookups, int propertyId, int tenantId,
                              String start, String end, String rent) {
        if (lookups.get("properties").isEmpty()) {
            showError(new EstateEaseException.BusinessRule("There are no available properties to book."));
            return;
        }
        if (lookups.get("tenants").isEmpty()) {
            showError(new EstateEaseException.Validation("Add a tenant first."));
            return;
        }

        JComboBox<Database.Item> properties = combo(lookups.get("properties"));
        JComboBox<Database.Item> tenants = combo(lookups.get("tenants"));
        select(properties, propertyId);
        select(tenants, tenantId);
        JTextField fStart = new JTextField(start, 22);
        JTextField fEnd = new JTextField(end, 22);
        JTextField fRent = new JTextField(rent, 22);

        boolean ok = showForm("Book Property",
                new String[]{"Property", "Tenant", "Start date (yyyy-MM-dd)", "End date (yyyy-MM-dd)",
                        "Monthly rent (blank = property rent)"},
                new JComponent[]{properties, tenants, fStart, fEnd, fRent});
        if (!ok) {
            return;
        }

        Database.Item property = (Database.Item) properties.getSelectedItem();
        Database.Item tenant = (Database.Item) tenants.getSelectedItem();
        try {
            LocalDate startDate = parseDate(fStart.getText(), "Start date");
            LocalDate endDate = parseDate(fEnd.getText(), "End date");
            if (!endDate.isAfter(startDate)) {
                throw new EstateEaseException.Validation("End date must be after the start date.");
            }
            BigDecimal rentAmount = parseMoney(fRent.getText(), "Monthly rent", true, false);

            background(() -> Database.bookProperty(property.id, tenant.id, startDate, endDate, rentAmount),
                    agreementId -> {
                        setStatus("Agreement #" + agreementId + " created");
                        refresh();
                    });
        } catch (EstateEaseException ex) {
            showError(ex);
            showBookForm(lookups, property.id, tenant.id, fStart.getText(), fEnd.getText(), fRent.getText());
        }
    }

    // ------------------------------------------------------------------
    // Ending agreements
    // ------------------------------------------------------------------

    private void endAgreement(String newStatus) throws EstateEaseException {
        final int id = selectedId();
        int answer = JOptionPane.showConfirmDialog(this,
                "Mark agreement #" + id + " as " + newStatus + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            runAndRefresh(() -> Database.terminateAgreement(id, newStatus),
                    "Agreement #" + id + " marked " + newStatus);
        }
    }

    private void runExpiryCheck() {
        background(() -> Database.expireAgreements(), count -> {
            setStatus(count + " agreement(s) expired");
            refresh();
        });
    }

    // ------------------------------------------------------------------
    // Payments
    // ------------------------------------------------------------------

    private void openPaymentForm() throws EstateEaseException {
        int id = selectedId();
        showPaymentForm(id, text("Rent"), LocalDate.now().toString(), "UPI", "Paid");
    }

    private void showPaymentForm(int agreementId, String amount, String date, String mode, String status) {
        JTextField fAmount = new JTextField(amount, 22);
        JTextField fDate = new JTextField(date, 22);
        JComboBox<String> modes = new JComboBox<>(new String[]{"UPI", "Card", "Net Banking", "Cash"});
        JComboBox<String> statuses = new JComboBox<>(new String[]{"Paid", "Pending", "Failed"});
        modes.setSelectedItem(mode);
        statuses.setSelectedItem(status);

        boolean ok = showForm("Record Payment - Agreement #" + agreementId,
                new String[]{"Amount", "Date (yyyy-MM-dd)", "Mode", "Status"},
                new JComponent[]{fAmount, fDate, modes, statuses});
        if (!ok) {
            return;
        }

        String chosenMode = (String) modes.getSelectedItem();
        String chosenStatus = (String) statuses.getSelectedItem();
        try {
            BigDecimal amountValue = parseMoney(fAmount.getText(), "Amount", false, false);
            LocalDate dateValue = parseDate(fDate.getText(), "Payment date");
            background(() -> Database.recordPayment(agreementId, dateValue, amountValue, chosenMode, chosenStatus),
                    paymentId -> setStatus("Payment #" + paymentId + " recorded for agreement #" + agreementId));
        } catch (EstateEaseException ex) {
            showError(ex);
            showPaymentForm(agreementId, fAmount.getText(), fDate.getText(), chosenMode, chosenStatus);
        }
    }

    private void viewPayments() throws EstateEaseException {
        final int id = selectedId();
        background(() -> Database.getPayments(id), t -> {
            // total of the payments that actually went through
            List<String> cols = Arrays.asList(t.columns);
            int amountCol = cols.indexOf("Amount");
            int statusCol = cols.indexOf("Status");
            BigDecimal totalPaid = BigDecimal.ZERO;
            for (Object[] row : t.rows) {
                if ("Paid".equals(row[statusCol])) {
                    totalPaid = totalPaid.add((BigDecimal) row[amountCol]);
                }
            }
            showTableDialog("Payments for agreement #" + id + " (total paid: Rs." + totalPaid + ")", t);
        });
    }
}
