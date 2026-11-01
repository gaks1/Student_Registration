package ke.ac.usiu.registration.model;

/** confirmations/{idNo} - only written after the M-Pesa payment succeeded. */
public class Confirmation {
    public String status;
    public long amount;
    public String phone;
    public String checkoutRequestId;
    public long confirmedAt;

    public Confirmation() {
    }
}
