import java.util.regex.Pattern;

/**
 * Abstract parent of Owner and Tenant. Both tables have the same columns,
 * so the shared fields and validation live here and the subclasses only say
 * which table/role they represent.
 */
public abstract class Person {

    private static final Pattern PHONE = Pattern.compile("^[+]?[0-9]{10,14}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private int id;
    private String fullName = "";
    private String phone = "";
    private String email = "";
    private String city = "";

    // ---- what each subclass must provide ----
    public abstract String getRole();

    public abstract String getTableName();

    public abstract String getIdColumn();

    public abstract Person createBlank();

    // ---- getters / setters (null becomes "" and text is trimmed) ----
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = clean(fullName);
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = clean(phone);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = clean(email);
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = clean(city);
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim();
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE.matcher(phone.trim()).matches();
    }

    /** Mirrors the CHECK constraints in Database.sql so errors show up before the DB call. */
    public void validate() throws EstateEaseException.Validation {
        if (fullName.isEmpty()) {
            throw new EstateEaseException.Validation(getRole() + " name is required.");
        }
        if (fullName.length() > 100) {
            throw new EstateEaseException.Validation("Name can be at most 100 characters.");
        }
        if (!isValidPhone(phone)) {
            throw new EstateEaseException.Validation(
                    "Phone must be 10 to 14 digits, optionally starting with +.");
        }
        if (!email.isEmpty() && (!EMAIL.matcher(email).matches() || email.length() > 100)) {
            throw new EstateEaseException.Validation("Email address is not valid.");
        }
        if (city.isEmpty()) {
            city = "Pune"; // same default as the table
        }
        if (city.length() > 50) {
            throw new EstateEaseException.Validation("City can be at most 50 characters.");
        }
    }

    @Override
    public String toString() {
        return fullName + " (" + getRole() + " #" + id + ")";
    }
}
