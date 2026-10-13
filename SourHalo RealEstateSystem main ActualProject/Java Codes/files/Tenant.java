/** A tenant (table Tenant). */
public class Tenant extends Person {

    @Override
    public String getRole() {
        return "Tenant";
    }

    @Override
    public String getTableName() {
        return "Tenant";
    }

    @Override
    public String getIdColumn() {
        return "Tenant_ID";
    }

    @Override
    public Person createBlank() {
        return new Tenant();
    }
}
