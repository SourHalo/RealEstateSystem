/** A property owner (table Owner). */
public class Owner extends Person {

    @Override
    public String getRole() {
        return "Owner";
    }

    @Override
    public String getTableName() {
        return "Owner";
    }

    @Override
    public String getIdColumn() {
        return "Owner_ID";
    }

    @Override
    public Person createBlank() {
        return new Owner();
    }
}
