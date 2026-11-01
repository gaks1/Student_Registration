package ke.ac.usiu.registration.report;

import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import ke.ac.usiu.registration.model.Confirmation;
import ke.ac.usiu.registration.model.HostelBooking;
import ke.ac.usiu.registration.model.LibraryRequest;
import ke.ac.usiu.registration.model.ReportData;
import ke.ac.usiu.registration.model.Student;

/** Builds the plain-text preview and the printable HTML for one student or for everyone. */
public final class ReportBuilder {

    private static final String CSS =
            "body{font-family:sans-serif;color:#1B1F27;margin:24px}"
                    + "h1{color:#0B2F5B;font-size:22px;margin:0}"
                    + "h2{color:#0B2F5B;font-size:15px;border-bottom:2px solid #F2A900;padding-bottom:4px;margin-top:22px}"
                    + ".sub{color:#5B6573;font-size:12px;margin-bottom:14px}"
                    + "table{border-collapse:collapse;width:100%}"
                    + "th,td{border:1px solid #C9CFD8;padding:6px 8px;font-size:12px;text-align:left;vertical-align:top}"
                    + "th{background:#0B2F5B;color:#fff}"
                    + "td.k{background:#F3F5F9;font-weight:bold;width:32%}"
                    + ".ok{color:#1E8E3E;font-weight:bold}.bad{color:#C5221F;font-weight:bold}"
                    + ".foot{margin-top:18px;font-size:11px;color:#5B6573}";

    private ReportBuilder() {
    }

    // ------------------------------------------------------------------ one student

    public static boolean isConfirmed(ReportData d, String id) {
        return d.confirmations.containsKey(id);
    }

    public static String statusText(ReportData d, String id) {
        return isConfirmed(d, id)
                ? "REGISTERED - payment confirmed"
                : "NOT CONFIRMED - payment pending";
    }

    public static String studentText(ReportData d, String id) {
        Student s = d.students.get(id);
        StringBuilder b = new StringBuilder();
        b.append("STATUS: ").append(statusText(d, id)).append("\n\n");
        b.append("PERSONAL DETAILS\n");
        b.append("Name: ").append(nz(s.fullNames)).append('\n');
        b.append("ID No: ").append(nz(s.idNo)).append('\n');
        b.append("Reg No: ").append(nz(s.regNo)).append('\n');
        b.append("Gender: ").append(nz(s.gender)).append('\n');
        b.append("Course: ").append(nz(s.course)).append('\n');
        b.append("Department: ").append(nz(s.department)).append("\n\n");

        b.append("LIBRARY\n");
        List<LibraryRequest> books = d.library.get(id);
        if (books == null || books.isEmpty()) {
            b.append("No book requested\n");
        } else {
            for (LibraryRequest r : books) {
                b.append("- ").append(nz(r.title)).append(" by ").append(nz(r.author))
                        .append(" (ISBN ").append(nz(r.isbn)).append(")\n  Borrowed ")
                        .append(nz(r.dateBorrowed)).append(", return ").append(nz(r.dateReturn)).append('\n');
            }
        }

        b.append("\nHOSTEL\n");
        HostelBooking h = d.hostels.get(id);
        if (h == null) {
            b.append("No hostel selected\n");
        } else {
            b.append(nz(h.location)).append(" - ").append(nz(h.roomType))
                    .append(h.sharable ? " (sharable)" : " (not sharable)").append('\n');
        }

        b.append("\nPAYMENT\n");
        Confirmation c = d.confirmations.get(id);
        if (c == null) {
            b.append("No successful payment yet\n");
        } else {
            b.append("Kshs ").append(c.amount).append(" paid from ").append(nz(c.phone))
                    .append(" on ").append(dateTime(c.confirmedAt)).append('\n');
        }
        return b.toString();
    }

    public static String studentHtml(ReportData d, String id) {
        Student s = d.students.get(id);
        boolean ok = isConfirmed(d, id);
        StringBuilder h = new StringBuilder();
        h.append("<html><head><meta charset='utf-8'><style>").append(CSS).append("</style></head><body>");
        h.append("<h1>USIU - Student Registration Summary</h1>");
        h.append("<div class='sub'>Generated ").append(dateTime(System.currentTimeMillis())).append("</div>");
        h.append("<p>Status: <span class='").append(ok ? "ok" : "bad").append("'>")
                .append(esc(statusText(d, id))).append("</span></p>");

        h.append("<h2>Personal details</h2><table>");
        row(h, "Full names", s.fullNames);
        row(h, "ID number", s.idNo);
        row(h, "Registration number", s.regNo);
        row(h, "Gender", s.gender);
        row(h, "Course", s.course);
        row(h, "Department", s.department);
        h.append("</table>");

        h.append("<h2>Library requests</h2>");
        List<LibraryRequest> books = d.library.get(id);
        if (books == null || books.isEmpty()) {
            h.append("<p>No book requested.</p>");
        } else {
            h.append("<table><tr><th>ISBN</th><th>Title</th><th>Author</th><th>Borrowed</th><th>Return</th></tr>");
            for (LibraryRequest r : books) {
                h.append("<tr><td>").append(esc(r.isbn)).append("</td><td>").append(esc(r.title))
                        .append("</td><td>").append(esc(r.author)).append("</td><td>")
                        .append(esc(r.dateBorrowed)).append("</td><td>").append(esc(r.dateReturn))
                        .append("</td></tr>");
            }
            h.append("</table>");
        }

        h.append("<h2>Hostel</h2>");
        HostelBooking hb = d.hostels.get(id);
        if (hb == null) {
            h.append("<p>No hostel selected.</p>");
        } else {
            h.append("<table>");
            row(h, "Location", hb.location);
            row(h, "Room type", hb.roomType);
            row(h, "Sharing", hb.sharable ? "Sharable" : "Not sharable");
            h.append("</table>");
        }

        h.append("<h2>Fees payment (M-Pesa)</h2>");
        Confirmation c = d.confirmations.get(id);
        if (c == null) {
            h.append("<p>No successful payment recorded yet.</p>");
        } else {
            h.append("<table>");
            row(h, "Amount paid", "Kshs " + c.amount);
            row(h, "Paid from", c.phone);
            row(h, "Confirmed on", dateTime(c.confirmedAt));
            h.append("</table>");
        }
        h.append("<div class='foot'>This summary was produced by the USIU Student Registration app.</div>");
        h.append("</body></html>");
        return h.toString();
    }

    // ------------------------------------------------------------------ all students

    public static String allHtml(ReportData d) {
        List<Student> list = new ArrayList<>(d.students.values());
        Collections.sort(list, (a, b) -> nz(a.fullNames).compareToIgnoreCase(nz(b.fullNames)));

        int confirmed = 0;
        long total = 0;
        StringBuilder h = new StringBuilder();
        h.append("<html><head><meta charset='utf-8'><style>").append(CSS).append("</style></head><body>");
        h.append("<h1>USIU - Registered Students Summary</h1>");
        h.append("<div class='sub'>Generated ").append(dateTime(System.currentTimeMillis())).append("</div>");
        h.append("<table><tr><th>#</th><th>Full names</th><th>ID No</th><th>Reg No</th><th>Gender</th>"
                + "<th>Course</th><th>Department</th><th>Hostel</th><th>Books</th>"
                + "<th>Status</th><th>Paid (Kshs)</th></tr>");

        int n = 0;
        for (Student s : list) {
            n++;
            String id = s.idNo != null ? s.idNo : "";
            Confirmation c = d.confirmations.get(id);
            HostelBooking hb = d.hostels.get(id);
            List<LibraryRequest> books = d.library.get(id);
            boolean ok = c != null;
            if (ok) {
                confirmed++;
                total += c.amount;
            }
            h.append("<tr><td>").append(n).append("</td><td>").append(esc(s.fullNames))
                    .append("</td><td>").append(esc(s.idNo)).append("</td><td>").append(esc(s.regNo))
                    .append("</td><td>").append(esc(s.gender)).append("</td><td>").append(esc(s.course))
                    .append("</td><td>").append(esc(s.department)).append("</td><td>")
                    .append(hb == null ? "-" : esc(hb.location + ", " + hb.roomType))
                    .append("</td><td>").append(books == null ? 0 : books.size())
                    .append("</td><td class='").append(ok ? "ok" : "bad").append("'>")
                    .append(ok ? "Registered" : "Pending payment").append("</td><td>")
                    .append(ok ? String.valueOf(c.amount) : "-").append("</td></tr>");
        }
        h.append("</table>");
        h.append("<p><b>Total students:</b> ").append(list.size())
                .append(" &nbsp; <b>Confirmed:</b> ").append(confirmed)
                .append(" &nbsp; <b>Pending:</b> ").append(list.size() - confirmed)
                .append(" &nbsp; <b>Total collected:</b> Kshs ").append(total).append("</p>");
        h.append("<div class='foot'>Produced by the USIU Student Registration app.</div>");
        h.append("</body></html>");
        return h.toString();
    }

    // ------------------------------------------------------------------ small helpers

    private static void row(StringBuilder h, String key, String value) {
        h.append("<tr><td class='k'>").append(esc(key)).append("</td><td>").append(esc(value)).append("</td></tr>");
    }

    private static String esc(String s) {
        return TextUtils.htmlEncode(s == null ? "" : s);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String dateTime(long millis) {
        if (millis <= 0) {
            return "-";
        }
        SimpleDateFormat f = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ENGLISH);
        f.setTimeZone(TimeZone.getTimeZone("Africa/Nairobi"));
        return f.format(millis);
    }
}
