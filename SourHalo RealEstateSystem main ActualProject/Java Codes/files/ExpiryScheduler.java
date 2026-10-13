import javax.swing.SwingUtilities;

/**
 * Background thread that calls sp_expire_agreements now and then, so agreements
 * whose End_Date has passed are expired automatically while the app is open.
 * It never touches Swing directly: results are handed to the listener on the
 * Event Dispatch Thread through SwingUtilities.invokeLater.
 */
public class ExpiryScheduler implements Runnable {

    /** Called on the Swing thread after every check. error is null when the check worked. */
    public interface Listener {
        void onCheck(int expired, String error);
    }

    private final long intervalMillis;
    private final Listener listener;
    private volatile boolean running = false;
    private Thread thread;
    private int totalExpired = 0; // shared between threads, so access is synchronized

    public ExpiryScheduler(long intervalMillis, Listener listener) {
        this.intervalMillis = intervalMillis;
        this.listener = listener;
    }

    public void start() {
        if (thread != null) {
            return;
        }
        running = true;
        thread = new Thread(this, "ExpiryScheduler");
        thread.setDaemon(true); // never keeps the JVM alive on its own
        thread.start();
    }

    public void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt(); // wakes it up if it is sleeping
        }
    }

    public synchronized int getTotalExpired() {
        return totalExpired;
    }

    private synchronized void addExpired(int n) {
        totalExpired += n;
    }

    @Override
    public void run() {
        while (running) {
            int expired = 0;
            String error = null;
            try {
                expired = Database.expireAgreements();
                addExpired(expired);
            } catch (EstateEaseException e) {
                error = e.getMessage();
            }

            final int finalExpired = expired;
            final String finalError = error;
            SwingUtilities.invokeLater(() -> listener.onCheck(finalExpired, finalError));

            try {
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return; // stop() was called
            }
        }
    }
}
