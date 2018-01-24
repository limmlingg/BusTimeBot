package model.busarrival;

public class BusArrival {
    public static final String LABEL_ARRIVING = "Arr";
    public static final long TIME_ARRIVING = 0;

    public static final String LABEL_NA = "N/A";
    public static final String LABEL_NA_BLANK = "";
    public static final long TIME_NA = Long.MAX_VALUE;

    public String serviceNo;
    public long arrivalTime1;
    public long arrivalTime2;
}
