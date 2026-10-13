import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Every database operation in the application lives here.
 * - Plain reads/writes use PreparedStatement.
 * - Booking, work orders, payments, terminating and expiring agreements go
 *   through the stored procedures from Procedures.sql (CallableStatement).
 * - Every SQLException is translated into one of the EstateEaseException types.
 * All methods are static and open/close their own connection, so they are safe
 * to call from background threads.
 */
public class Database {

    // ------------------------------------------------------------------
    // Small helper types
    // ------------------------------------------------------------------

    /** Result of a query: column names plus rows. */
    public static class Table {
        public final String[] columns;
        public final List<Object[]> rows;

        public Table(String[] columns, List<Object[]> rows) {
            this.columns = columns;
            this.rows = rows;
        }
    }

    /** An id + label pair used to fill combo boxes. */
    public static class Item {
        public final int id;
        public final String label;

        public Item(int id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    // ------------------------------------------------------------------
    // Connection handling
    // ------------------------------------------------------------------

    private static String url;
    private static String user;
    private static String password;

    /** Stores the settings and checks that a connection can really be made. */
    public static void connect(String host, String port, String dbName, String dbUser, String dbPassword)
            throws EstateEaseException {
        if (host.trim().isEmpty() || port.trim().isEmpty() || dbName.trim().isEmpty() || dbUser.trim().isEmpty()) {
            throw new EstateEaseException.Validation("Host, port, database and user are required.");
        }
        loadDriver();
        url = "jdbc:mysql://" + host.trim() + ":" + port.trim() + "/" + dbName.trim()
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        user = dbUser.trim();
        password = dbPassword;
        try (Connection con = DriverManager.getConnection(url, user, password)) {
            // opening and closing is the whole test
        } catch (SQLException e) {
            url = null;
            throw translate(e);
        }
    }

    private static void loadDriver() throws EstateEaseException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");          // Connector/J 8.x
        } catch (ClassNotFoundException e1) {
            try {
                Class.forName("com.mysql.jdbc.Driver");          // Connector/J 5.x
            } catch (ClassNotFoundException e2) {
                throw new EstateEaseException.DbError(
                        "MySQL JDBC driver not found. Put the connector jar in the lib folder "
                                + "and add it to the classpath (-cp \".;lib/*\").", e2);
            }
        }
    }

    public static Connection getConnection() throws EstateEaseException {
        if (url == null) {
            throw new EstateEaseException.DbError("Not connected to a database yet.");
        }
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    /** Converts a raw SQLException into one of our own exception types. */
    private static EstateEaseException translate(SQLException e) {
        int code = e.getErrorCode();
        if ("45000".equals(e.getSQLState()) || code == 1644) {
            // raised by SIGNAL in the stored procedures / triggers
            return new EstateEaseException.BusinessRule(e.getMessage());
        }
        switch (code) {
            case 1062:
                return new EstateEaseException.Duplicate(
                        "A record with the same unique value (for example the same email) already exists.");
            case 1451:
                return new EstateEaseException.Reference(
                        "This record is still used by other records, so it cannot be deleted.");
            case 1452:
                return new EstateEaseException.Reference("A referenced record does not exist.");
            case 3819:
                return new EstateEaseException.Validation("A value was rejected by a database CHECK constraint.");
            default:
                return new EstateEaseException.DbError("Database error: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------
    // Generic helpers
    // ------------------------------------------------------------------

    private static void bind(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            if (p == null) {
                ps.setNull(i + 1, Types.NULL);
            } else if (p instanceof LocalDate) {
                ps.setString(i + 1, p.toString()); // yyyy-MM-dd, avoids time zone shifts
            } else {
                ps.setObject(i + 1, p);
            }
        }
    }

    public static Table query(String sql, Object... params) throws EstateEaseException {
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                String[] cols = new String[n];
                int[] types = new int[n];
                for (int i = 0; i < n; i++) {
                    cols[i] = md.getColumnLabel(i + 1);
                    types[i] = md.getColumnType(i + 1);
                }
                List<Object[]> rows = new ArrayList<>();
                while (rs.next()) {
                    Object[] row = new Object[n];
                    for (int i = 0; i < n; i++) {
                        if (types[i] == Types.DATE || types[i] == Types.TIMESTAMP || types[i] == Types.TIME) {
                            String s = rs.getString(i + 1); // read as text: no time zone conversion
                            if (s != null && s.endsWith(".0")) {
                                s = s.substring(0, s.length() - 2);
                            }
                            row[i] = s;
                        } else {
                            row[i] = rs.getObject(i + 1);
                        }
                    }
                    rows.add(row);
                }
                return new Table(cols, rows);
            }
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    public static int update(String sql, Object... params) throws EstateEaseException {
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    /** Runs a stored procedure. If hasOut is true the LAST parameter is an INT OUT parameter and is returned. */
    private static int callProcedure(String name, boolean hasOut, Object... in) throws EstateEaseException {
        int total = in.length + (hasOut ? 1 : 0);
        StringBuilder sql = new StringBuilder("{CALL ").append(name).append("(");
        for (int i = 0; i < total; i++) {
            sql.append(i == 0 ? "?" : ",?");
        }
        sql.append(")}");
        try (Connection con = getConnection(); CallableStatement cs = con.prepareCall(sql.toString())) {
            bind(cs, in);
            if (hasOut) {
                cs.registerOutParameter(in.length + 1, Types.INTEGER);
            }
            cs.execute();
            return hasOut ? cs.getInt(in.length + 1) : 0;
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    public static List<Item> items(String sql, Object... params) throws EstateEaseException {
        Table t = query(sql, params);
        List<Item> list = new ArrayList<>();
        for (Object[] r : t.rows) {
            list.add(new Item(((Number) r[0]).intValue(), String.valueOf(r[1])));
        }
        return list;
    }

    // ------------------------------------------------------------------
    // Owners and tenants (polymorphic: works for any Person subclass)
    // ------------------------------------------------------------------

    public static List<Person> getPeople(Person proto) throws EstateEaseException {
        String sql = "SELECT " + proto.getIdColumn() + ", Full_Name, Phone, Email, City FROM "
                + proto.getTableName() + " ORDER BY " + proto.getIdColumn();
        List<Person> list = new ArrayList<>();
        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Person p = proto.createBlank();
                p.setId(rs.getInt(1));
                p.setFullName(rs.getString(2));
                p.setPhone(rs.getString(3));
                p.setEmail(rs.getString(4));
                p.setCity(rs.getString(5));
                list.add(p);
            }
        } catch (SQLException e) {
            throw translate(e);
        }
        return list;
    }

    /** Inserts when the id is 0, otherwise updates. */
    public static void savePerson(Person p) throws EstateEaseException {
        String email = p.getEmail().isEmpty() ? null : p.getEmail(); // empty text would break UNIQUE
        if (p.getId() == 0) {
            update("INSERT INTO " + p.getTableName() + " (Full_Name, Phone, Email, City) VALUES (?,?,?,?)",
                    p.getFullName(), p.getPhone(), email, p.getCity());
        } else {
            update("UPDATE " + p.getTableName() + " SET Full_Name=?, Phone=?, Email=?, City=? WHERE "
                    + p.getIdColumn() + "=?",
                    p.getFullName(), p.getPhone(), email, p.getCity(), p.getId());
        }
    }

    public static void deletePerson(Person p) throws EstateEaseException {
        update("DELETE FROM " + p.getTableName() + " WHERE " + p.getIdColumn() + "=?", p.getId());
    }

    // ------------------------------------------------------------------
    // Properties
    // ------------------------------------------------------------------

    /** status / area may be null meaning "no filter". */
    public static Table getProperties(String status, String area) throws EstateEaseException {
        StringBuilder sql = new StringBuilder(
                "SELECT p.Property_ID AS ID, p.Address, p.Area, p.City, t.Type_Name AS Type, "
                        + "o.Full_Name AS Owner, p.Monthly_Rent AS Rent, p.Status, "
                        + "p.Owner_ID AS OwnerID, p.Type_ID AS TypeID "
                        + "FROM Property p "
                        + "JOIN Owner o ON o.Owner_ID = p.Owner_ID "
                        + "JOIN Property_Type t ON t.Type_ID = p.Type_ID WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (status != null) {
            sql.append(" AND p.Status = ?");
            params.add(status);
        }
        if (area != null) {
            sql.append(" AND p.Area = ?");
            params.add(area);
        }
        sql.append(" ORDER BY p.Property_ID");
        return query(sql.toString(), params.toArray());
    }

    public static Set<String> getAreas() throws EstateEaseException {
        Table t = query("SELECT DISTINCT Area FROM Property");
        Set<String> areas = new TreeSet<>();
        for (Object[] r : t.rows) {
            areas.add(String.valueOf(r[0]));
        }
        return areas;
    }

    public static Map<String, List<Item>> getPropertyLookups() throws EstateEaseException {
        Map<String, List<Item>> m = new LinkedHashMap<>();
        m.put("owners", items("SELECT Owner_ID, CONCAT(Full_Name, ' (', Phone, ')') FROM Owner ORDER BY Full_Name"));
        m.put("types", items("SELECT Type_ID, Type_Name FROM Property_Type ORDER BY Type_ID"));
        return m;
    }

    /** id == 0 inserts a new property (status defaults to Available); otherwise updates it. Status is never edited here. */
    public static void saveProperty(int id, int ownerId, int typeId, String address, String area, String city,
                                    BigDecimal rent) throws EstateEaseException {
        if (id == 0) {
            update("INSERT INTO Property (Owner_ID, Type_ID, Address, Area, City, Monthly_Rent) VALUES (?,?,?,?,?,?)",
                    ownerId, typeId, address, area, city, rent);
        } else {
            update("UPDATE Property SET Owner_ID=?, Type_ID=?, Address=?, Area=?, City=?, Monthly_Rent=? "
                    + "WHERE Property_ID=?", ownerId, typeId, address, area, city, rent, id);
        }
    }

    public static void deleteProperty(int id) throws EstateEaseException {
        update("DELETE FROM Property WHERE Property_ID=?", id);
    }

    // ------------------------------------------------------------------
    // Agreements and payments
    // ------------------------------------------------------------------

    public static Table getAgreements() throws EstateEaseException {
        return query("SELECT a.Agreement_ID AS ID, p.Address AS Property, t.Full_Name AS Tenant, "
                + "a.Start_Date, a.End_Date, a.Rent_Amount AS Rent, a.Agreement_Status AS Status "
                + "FROM Rental_Agreement a "
                + "JOIN Property p ON p.Property_ID = a.Property_ID "
                + "JOIN Tenant t ON t.Tenant_ID = a.Tenant_ID "
                + "ORDER BY a.Agreement_ID DESC");
    }

    public static Map<String, List<Item>> getBookingLookups() throws EstateEaseException {
        Map<String, List<Item>> m = new LinkedHashMap<>();
        m.put("properties", items("SELECT Property_ID, CONCAT('#', Property_ID, ' - ', Address, ', ', Area, "
                + "' (Rs.', Monthly_Rent, ')') FROM Property WHERE Status = 'Available' ORDER BY Property_ID"));
        m.put("tenants", items("SELECT Tenant_ID, CONCAT(Full_Name, ' (', Phone, ')') FROM Tenant ORDER BY Full_Name"));
        return m;
    }

    /** rent == null makes the procedure use the property's own Monthly_Rent. Returns the new agreement id. */
    public static int bookProperty(int propertyId, int tenantId, LocalDate start, LocalDate end, BigDecimal rent)
            throws EstateEaseException {
        return callProcedure("sp_book_property", true, propertyId, tenantId, start, end, rent);
    }

    public static void terminateAgreement(int agreementId, String newStatus) throws EstateEaseException {
        callProcedure("sp_terminate_agreement", false, agreementId, newStatus);
    }

    /** Returns how many agreements were expired. */
    public static int expireAgreements() throws EstateEaseException {
        return callProcedure("sp_expire_agreements", true);
    }

    public static Table getPayments(int agreementId) throws EstateEaseException {
        return query("SELECT Payment_ID AS ID, Payment_Date, Amount, Payment_Mode AS Mode, Payment_Status AS Status "
                + "FROM Payment WHERE Agreement_ID = ? ORDER BY Payment_Date DESC, Payment_ID DESC", agreementId);
    }

    /** Returns the new payment id. */
    public static int recordPayment(int agreementId, LocalDate date, BigDecimal amount, String mode, String status)
            throws EstateEaseException {
        return callProcedure("sp_record_payment", true, agreementId, date, amount, mode, status);
    }

    // ------------------------------------------------------------------
    // Contractors and work orders
    // ------------------------------------------------------------------

    public static Table getContractors() throws EstateEaseException {
        return query("SELECT Contractor_ID AS ID, Contractor_Name AS Name, Specialization, Phone, "
                + "Charges_Per_Job AS Charges FROM Contractor ORDER BY Contractor_ID");
    }

    public static void saveContractor(int id, String name, String specialization, String phone, BigDecimal charges)
            throws EstateEaseException {
        if (id == 0) {
            update("INSERT INTO Contractor (Contractor_Name, Specialization, Phone, Charges_Per_Job) VALUES (?,?,?,?)",
                    name, specialization, phone, charges);
        } else {
            update("UPDATE Contractor SET Contractor_Name=?, Specialization=?, Phone=?, Charges_Per_Job=? "
                    + "WHERE Contractor_ID=?", name, specialization, phone, charges, id);
        }
    }

    public static void deleteContractor(int id) throws EstateEaseException {
        update("DELETE FROM Contractor WHERE Contractor_ID=?", id);
    }

    public static Table getWorkOrders() throws EstateEaseException {
        return query("SELECT w.WorkOrder_ID AS ID, p.Address AS Property, c.Contractor_Name AS Contractor, "
                + "w.Issue_Description AS Issue, w.Request_Date, w.Cost, w.Status "
                + "FROM Maintenance_Work_Order w "
                + "JOIN Property p ON p.Property_ID = w.Property_ID "
                + "JOIN Contractor c ON c.Contractor_ID = w.Contractor_ID "
                + "ORDER BY w.WorkOrder_ID DESC");
    }

    public static Map<String, List<Item>> getWorkOrderLookups() throws EstateEaseException {
        Map<String, List<Item>> m = new LinkedHashMap<>();
        m.put("properties", items("SELECT Property_ID, CONCAT('#', Property_ID, ' - ', Address, ' [', Status, ']') "
                + "FROM Property ORDER BY Property_ID"));
        m.put("contractors", items("SELECT Contractor_ID, CONCAT(Contractor_Name, ' - ', Specialization, "
                + "' (Rs.', Charges_Per_Job, ')') FROM Contractor ORDER BY Contractor_Name"));
        return m;
    }

    /** cost == null makes the procedure use the contractor's Charges_Per_Job. Returns the new work order id. */
    public static int assignWorkOrder(int propertyId, int contractorId, String issue, LocalDate date,
                                      BigDecimal cost) throws EstateEaseException {
        return callProcedure("sp_assign_work_order", true, propertyId, contractorId, issue, date, cost);
    }

    /** finalCost == null keeps the existing cost. */
    public static void completeWorkOrder(int workOrderId, BigDecimal finalCost) throws EstateEaseException {
        callProcedure("sp_complete_work_order", false, workOrderId, finalCost);
    }

    // ------------------------------------------------------------------
    // Dashboard and audit log
    // ------------------------------------------------------------------

    /** Label -> value, in display order. */
    public static Map<String, Object> getDashboardCounts() throws EstateEaseException {
        Table t = query("SELECT "
                + "(SELECT COUNT(*) FROM Property WHERE Status = 'Available') AS `Available properties`, "
                + "(SELECT COUNT(*) FROM Property WHERE Status = 'Rented') AS `Rented properties`, "
                + "(SELECT COUNT(*) FROM Property WHERE Status = 'Under Maintenance') AS `Under maintenance`, "
                + "(SELECT COUNT(*) FROM Rental_Agreement WHERE Agreement_Status = 'Active') AS `Active agreements`, "
                + "(SELECT COUNT(*) FROM Payment WHERE Payment_Status = 'Pending') AS `Pending payments`, "
                + "(SELECT COUNT(*) FROM Maintenance_Work_Order WHERE Status <> 'Completed') AS `Open work orders`, "
                + "(SELECT COALESCE(SUM(Amount), 0) FROM Payment WHERE Payment_Status = 'Paid') AS `Rent collected (Rs.)`");
        Map<String, Object> counts = new LinkedHashMap<>();
        if (!t.rows.isEmpty()) {
            Object[] row = t.rows.get(0);
            for (int i = 0; i < t.columns.length; i++) {
                counts.put(t.columns[i], row[i]);
            }
        }
        return counts;
    }

    public static Table getAuditLog() throws EstateEaseException {
        return query("SELECT l.Log_ID AS ID, l.Property_ID, p.Address, l.Old_Status, l.New_Status, l.Changed_At "
                + "FROM Property_Status_Log l JOIN Property p ON p.Property_ID = l.Property_ID "
                + "ORDER BY l.Log_ID DESC LIMIT 200");
    }
}
