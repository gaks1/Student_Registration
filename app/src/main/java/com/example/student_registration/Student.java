package com.example.student_registration;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Data model for a First Year Student Registration.
 * Stored in Cloud Firestore at students/{idNo}.
 * The document ID carries the student's ID number, so there is no separate id field.
 * Field names match the shared schema used across the team's activities.
 */
public class Student implements Serializable {
    private String fullNames;
    private String idNo;
    private String regNo;
    private String gender;
    private String course;
    private String department;
    private long createdAt;

    // Required default constructor for Firebase deserialization
    public Student() {
    }

    public Student(String fullNames, String idNo, String regNo, String gender,
                   String course, String department, long createdAt) {
        this.fullNames = fullNames;
        this.idNo = idNo;
        this.regNo = regNo;
        this.gender = gender;
        this.course = course;
        this.department = department;
        this.createdAt = createdAt;
    }

    public String getFullNames() {
        return fullNames;
    }

    public void setFullNames(String fullNames) {
        this.fullNames = fullNames;
    }

    public String getIdNo() {
        return idNo;
    }

    public void setIdNo(String idNo) {
        this.idNo = idNo;
    }

    public String getRegNo() {
        return regNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("fullNames", fullNames);
        result.put("idNo", idNo);
        result.put("regNo", regNo);
        result.put("gender", gender);
        result.put("course", course);
        result.put("department", department);
        result.put("createdAt", createdAt);
        return result;
    }
}
