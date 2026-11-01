package ke.ac.usiu.registration.model;

/** library/{idNo}/{pushId} */
public class LibraryRequest {
    public String isbn;
    public String author;
    public String title;
    public String dateBorrowed;
    public String dateReturn;
    public String studentIdNo;
    public long requestedAt;

    public LibraryRequest() {
    }
}
