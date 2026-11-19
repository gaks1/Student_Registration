package com.example.student_registration;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Data model for hostel selection (3rd Activity).
 * Stored in Cloud Firestore at hostels/{studentIdNo}.
 * Aligns with Wilson's branch database schema.
 */
public class HostelBooking implements Serializable {
    private String studentIdNo;
    private String location;
    private String roomType;
    private boolean sharable;
    private long bookedAt;

    // Required empty default constructor for Firebase deserialization
    public HostelBooking() {
    }

    public HostelBooking(String studentIdNo, String location, String roomType,
                         boolean sharable, long bookedAt) {
        this.studentIdNo = studentIdNo;
        this.location = location;
        this.roomType = roomType;
        this.sharable = sharable;
        this.bookedAt = bookedAt;
    }

    public String getStudentIdNo() {
        return studentIdNo;
    }

    public void setStudentIdNo(String studentIdNo) {
        this.studentIdNo = studentIdNo;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public boolean isSharable() {
        return sharable;
    }

    public void setSharable(boolean sharable) {
        this.sharable = sharable;
    }

    public long getBookedAt() {
        return bookedAt;
    }

    public void setBookedAt(long bookedAt) {
        this.bookedAt = bookedAt;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("studentIdNo", studentIdNo);
        result.put("location", location);
        result.put("roomType", roomType);
        result.put("sharable", sharable);
        result.put("bookedAt", bookedAt);
        return result;
    }
}
