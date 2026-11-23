package com.example.student_registration;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * One M-Pesa payment attempt.
 * Stored in Cloud Firestore at payments/{checkoutRequestId}.
 *
 * status is PENDING until Safaricom confirms, then SUCCESS or FAILED.
 */
public class PaymentRecord implements Serializable {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    private String studentIdNo;
    private String phone;
    private long amount;
    private String checkoutRequestId;
    private String merchantRequestId;
    private String status;
    private String message;
    private long createdAt;

    // Required default constructor for Firebase deserialization
    public PaymentRecord() {
    }

    public PaymentRecord(String studentIdNo, String phone, long amount,
                         String checkoutRequestId, String merchantRequestId, String status,
                         String message, long createdAt) {
        this.studentIdNo = studentIdNo;
        this.phone = phone;
        this.amount = amount;
        this.checkoutRequestId = checkoutRequestId;
        this.merchantRequestId = merchantRequestId;
        this.status = status;
        this.message = message;
        this.createdAt = createdAt;
    }

    public String getStudentIdNo() {
        return studentIdNo;
    }

    public void setStudentIdNo(String studentIdNo) {
        this.studentIdNo = studentIdNo;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getCheckoutRequestId() {
        return checkoutRequestId;
    }

    public void setCheckoutRequestId(String checkoutRequestId) {
        this.checkoutRequestId = checkoutRequestId;
    }

    public String getMerchantRequestId() {
        return merchantRequestId;
    }

    public void setMerchantRequestId(String merchantRequestId) {
        this.merchantRequestId = merchantRequestId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("studentIdNo", studentIdNo);
        result.put("phone", phone);
        result.put("amount", amount);
        result.put("checkoutRequestId", checkoutRequestId);
        result.put("merchantRequestId", merchantRequestId);
        result.put("status", status);
        result.put("message", message);
        result.put("createdAt", createdAt);
        return result;
    }
}
