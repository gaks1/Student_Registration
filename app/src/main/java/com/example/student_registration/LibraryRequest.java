package com.example.student_registration;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Data model for a library book request.
 * Stored in Cloud Firestore at library/{studentIdNo}/{pushId}.
 * Field names match the shared schema used across the team's activities.
 */
public class LibraryRequest implements Serializable {
    private String isbn;
    private String author;
    private String title;
    private String dateBorrowed;
    private String dateReturn;
    private String studentIdNo;
    private long requestedAt;

    // Required default constructor for Firebase deserialization
    public LibraryRequest() {
    }

    public LibraryRequest(String isbn, String author, String title,
                          String dateBorrowed, String dateReturn,
                          String studentIdNo, long requestedAt) {
        this.isbn = isbn;
        this.author = author;
        this.title = title;
        this.dateBorrowed = dateBorrowed;
        this.dateReturn = dateReturn;
        this.studentIdNo = studentIdNo;
        this.requestedAt = requestedAt;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDateBorrowed() {
        return dateBorrowed;
    }

    public void setDateBorrowed(String dateBorrowed) {
        this.dateBorrowed = dateBorrowed;
    }

    public String getDateReturn() {
        return dateReturn;
    }

    public void setDateReturn(String dateReturn) {
        this.dateReturn = dateReturn;
    }

    public String getStudentIdNo() {
        return studentIdNo;
    }

    public void setStudentIdNo(String studentIdNo) {
        this.studentIdNo = studentIdNo;
    }

    public long getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(long requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("isbn", isbn);
        result.put("author", author);
        result.put("title", title);
        result.put("dateBorrowed", dateBorrowed);
        result.put("dateReturn", dateReturn);
        result.put("studentIdNo", studentIdNo);
        result.put("requestedAt", requestedAt);
        return result;
    }
}
