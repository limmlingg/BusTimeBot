package model;

/** Responsible for holding information of the information station from a specific terminal */
public class BusInfoDirection {
    public String fromTerminal;
    public String weekdayFirstBus;
    public String weekdayLastBus;
    public String satFirstBus;
    public String satLastBus;
    public String sunAndPhFirstBus;
    public String sunAndPhLastBus;
    public String friSatEvePhFirstBus;
    public String friSatEvePhLastBus;


    /** Constructor for NR buses */
    public BusInfoDirection(String fromTerminal, String friSatEvePhFirstBus, String friSatEvePhLastBus) {
        this.fromTerminal = fromTerminal;
        this.friSatEvePhFirstBus = friSatEvePhFirstBus;
        this.friSatEvePhLastBus = friSatEvePhLastBus;
    }

    /** Constructor for standard buses */
    public BusInfoDirection(String fromTerminal, String weekdayFirstBus, String weekdayLastBus, String satFirstBus,
            String satLastBus, String sunAndPhFirstBus, String sunAndPhLastBus) {
        this.fromTerminal = fromTerminal;
        this.weekdayFirstBus = weekdayFirstBus;
        this.weekdayLastBus = weekdayLastBus;
        this.satFirstBus = satFirstBus;
        this.satLastBus = satLastBus;
        this.sunAndPhFirstBus = sunAndPhFirstBus;
        this.sunAndPhLastBus = sunAndPhLastBus;
    }

}
