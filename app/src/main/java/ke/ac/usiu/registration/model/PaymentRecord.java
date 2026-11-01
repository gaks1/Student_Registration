package ke.ac.usiu.registration.model;

/** payments/{idNo}/{checkoutRequestId} - one row per M-Pesa attempt. */
public class PaymentRecord {
    public static final String PENDING = "PENDING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";

    public String phone;
    public long amount;
    public String status;
    public String message;
    public long createdAt;
    public long updatedAt;

    public PaymentRecord() {
    }
}
