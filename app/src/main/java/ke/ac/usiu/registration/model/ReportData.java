package ke.ac.usiu.registration.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Everything needed to build a printed report, keyed by student ID number. */
public class ReportData {
    public final Map<String, Student> students = new LinkedHashMap<>();
    public final Map<String, List<LibraryRequest>> library = new LinkedHashMap<>();
    public final Map<String, HostelBooking> hostels = new LinkedHashMap<>();
    public final Map<String, Confirmation> confirmations = new LinkedHashMap<>();
}
