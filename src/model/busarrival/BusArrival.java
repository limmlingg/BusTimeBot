package model.busarrival;

public class BusArrival {
    public static final String LABEL_ARRIVING = "Arr";
    public static final long TIME_ARRIVING = 0;

    public static final String LABEL_NA = "N/A";
    public static final String LABEL_NA_BLANK = "";
    public static final long TIME_NA = Long.MAX_VALUE;

    public String serviceNo;
    public long arrivalTime1;
    public boolean isWab1;
    public long arrivalTime2;
    public boolean isWab2;

    public BusArrival() {
        this(false);
    }

    public BusArrival(boolean defaultWab) {
        isWab1 = defaultWab;
        isWab2 = defaultWab;
    }
}
