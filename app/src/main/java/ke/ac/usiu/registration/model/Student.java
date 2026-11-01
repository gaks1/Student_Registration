package ke.ac.usiu.registration.model;

/** students/{idNo} - public fields + empty constructor so Firebase can (de)serialise it. */
public class Student {
    public String fullNames;
    public String idNo;
    public String regNo;
    public String gender;
    public String course;
    public String department;
    public long createdAt;

    public Student() {
    }
}
