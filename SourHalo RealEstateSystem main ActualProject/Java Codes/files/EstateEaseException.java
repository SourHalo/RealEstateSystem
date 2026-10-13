/**
 * Base class for every error the application reports to the user.
 * The nested subclasses describe what kind of problem happened, and each one
 * overrides getCategory()/isUserError() (polymorphism) so the UI can show the
 * right dialog without a chain of instanceof checks.
 */
public class EstateEaseException extends Exception {

    public EstateEaseException(String message) {
        super(message);
    }

    public EstateEaseException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Short title for the error dialog. */
    public String getCategory() {
        return "Error";
    }

    /** true = the user can fix it (warning dialog), false = system problem (error dialog). */
    public boolean isUserError() {
        return false;
    }

    /** Bad input, caught in Java before it reaches the database. */
    public static class Validation extends EstateEaseException {
        public Validation(String message) {
            super(message);
        }

        @Override
        public String getCategory() {
            return "Invalid input";
        }

        @Override
        public boolean isUserError() {
            return true;
        }
    }

    /** A rule enforced by a stored procedure or trigger (SQLSTATE 45000). */
    public static class BusinessRule extends EstateEaseException {
        public BusinessRule(String message) {
            super(message);
        }

        @Override
        public String getCategory() {
            return "Action not allowed";
        }

        @Override
        public boolean isUserError() {
            return true;
        }
    }

    /** Unique constraint violated (MySQL error 1062), e.g. duplicate email. */
    public static class Duplicate extends EstateEaseException {
        public Duplicate(String message) {
            super(message);
        }

        @Override
        public String getCategory() {
            return "Duplicate entry";
        }

        @Override
        public boolean isUserError() {
            return true;
        }
    }

    /** Foreign key problem (MySQL errors 1451 / 1452). */
    public static class Reference extends EstateEaseException {
        public Reference(String message) {
            super(message);
        }

        @Override
        public String getCategory() {
            return "Record in use";
        }

        @Override
        public boolean isUserError() {
            return true;
        }
    }

    /** Any other database or connection problem. */
    public static class DbError extends EstateEaseException {
        public DbError(String message) {
            super(message);
        }

        public DbError(String message, Throwable cause) {
            super(message, cause);
        }

        @Override
        public String getCategory() {
            return "Database error";
        }
    }
}
