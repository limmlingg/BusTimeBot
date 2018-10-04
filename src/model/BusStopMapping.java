package model;

import java.util.HashMap;

/**
 * Handles the manual mapping of school bus stops to the public bus stops as some of these bus stops are not mapped properly
 */
public class BusStopMapping {

    public static HashMap<String, String> NusToPublic;
    public static HashMap<String, String> NtuToPublic;
    static {
        //Initialize HashMaps
        NusToPublic = new HashMap<String, String>(20);
        NtuToPublic = new HashMap<String, String>(30);

        //NUS Mappings
        NusToPublic.put("CGH", "41029");
        NusToPublic.put("BG-MRT", "41021");
        NusToPublic.put("CENLIB", "16181");
        NusToPublic.put("COMCEN", "16189");
        NusToPublic.put("KR-BT", "16009");
        NusToPublic.put("KR-MRT", "18331");
        NusToPublic.put("MUSEUM", "16161");
        NusToPublic.put("S17", "18309");
        NusToPublic.put("UHALL", "18311");
        NusToPublic.put("STAFFCLUB", "18329");
        NusToPublic.put("YIH", "16171");
        NusToPublic.put("STAFFCLUB-OPP", "18321");
        NusToPublic.put("UHALL-OPP", "18319");
        NusToPublic.put("RAFFLES", "16169");
        NusToPublic.put("YIH-OPP", "16179");
        NusToPublic.put("KR-MRT-OPP", "18339");
        NusToPublic.put("LT27", "18301");
        NusToPublic.put("KV", "16151");
        NusToPublic.put("BLK-EA-OPP", "16159");

        //NTU Mappings (stops that start with 63XXX has no public buses)
        //=================== Red
        NtuToPublic.put("378224", "27211");
        //NTUToPublic.put("382995", "63741");
        NtuToPublic.put("378227", "27231");
        NtuToPublic.put("378228", "27241");
        NtuToPublic.put("378229", "27251");
        NtuToPublic.put("378230", "27261");
        NtuToPublic.put("378233", "27281");
        NtuToPublic.put("378237", "27311");
        NtuToPublic.put("382998", "27209");
        NtuToPublic.put("378202", "27011");
        NtuToPublic.put("378204", "27031");
        //=================== Blue
        NtuToPublic.put("378225", "27219");
        //NTUToPublic.put("382999", "63713");
        //NTUToPublic.put("378203", "63717");
        //NTUToPublic.put("378222", "63721");
        //NTUToPublic.put("383003", "63723");
        //NTUToPublic.put("378234", "63727");
        //NTUToPublic.put("383004", "63731");
        //NTUToPublic.put("383006", "63701");
        //NTUToPublic.put("383009", "63703");
        //NTUToPublic.put("383010", "63707");
        //NTUToPublic.put("378226", "63711");
        //=================== Green
        NtuToPublic.put("377906", "22529");
        NtuToPublic.put("378233", "27281");
        NtuToPublic.put("378237", "27311");
        //NTUToPublic.put("383011", "63735");
        //NTUToPublic.put("383013", "63737");
        //NTUToPublic.put("383014", "63725");
        //=================== Brown
        NtuToPublic.put("377906", "22529");
        NtuToPublic.put("378233", "27281");
        NtuToPublic.put("378237", "27311");
        //NTUToPublic.put("383011", "63735");
        //NTUToPublic.put("383013", "63737");
        NtuToPublic.put("378207", "27061");
        NtuToPublic.put("378224", "27211");
        NtuToPublic.put("383015", "27221");
        //NTUToPublic.put("382995", "63741");
        NtuToPublic.put("378227", "27231");
        NtuToPublic.put("378228", "27241");
        NtuToPublic.put("378229", "27251");
        NtuToPublic.put("378230", "27261");
        //NTUToPublic.put("383018", "63747");
    }

    /**
     * Returns the public stop code from the given NTU/NUS bus stop code
     *
     * @param key
     *            bus stop code of NTU/NUS
     * @return bus stop code of public transit
     */
    public static String getValue(String key) {
        if (NusToPublic.containsKey(key)) {
            return NusToPublic.get(key);
        } else if (NtuToPublic.containsKey(key)) {
            return NtuToPublic.get(key);
        } else {
            return null;
        }
    }
}
