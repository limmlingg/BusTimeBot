package entity;

import java.util.HashMap;

public class BusStopMapping {

    public static HashMap<String, String> NUSToPublic;
    public static HashMap<String, String> NTUToPublic;
    static {
        //Initialize HashMaps
        NUSToPublic = new HashMap<String, String>(20);
        NTUToPublic = new HashMap<String, String>(30);

        //NUS Mappings
        NUSToPublic.put("CGH", "41029");
        NUSToPublic.put("BG-MRT", "41021");
        NUSToPublic.put("CENLIB", "16181");
        NUSToPublic.put("COMCEN", "16189");
        NUSToPublic.put("KR-BT", "16009");
        NUSToPublic.put("KR-MRT", "18331");
        NUSToPublic.put("MUSEUM", "16161");
        NUSToPublic.put("S17", "18309");
        NUSToPublic.put("UHALL", "18311");
        NUSToPublic.put("STAFFCLUB", "18329");
        NUSToPublic.put("YIH", "16171");
        NUSToPublic.put("STAFFCLUB-OPP", "18321");
        NUSToPublic.put("UHALL-OPP", "18319");
        NUSToPublic.put("RAFFLES", "16169");
        NUSToPublic.put("YIH-OPP", "16179");
        NUSToPublic.put("KR-MRT-OPP", "18339");
        NUSToPublic.put("LT29", "18301");
        NUSToPublic.put("KV", "16151");
        NUSToPublic.put("BLK-EA-OPP", "16159");

        //NTU Mappings (stops that start with 63XXX has no public buses)
        //=================== Red
        NTUToPublic.put("378224", "27211");
        //NTUToPublic.put("382995", "63741");
        NTUToPublic.put("378227", "27231");
        NTUToPublic.put("378228", "27241");
        NTUToPublic.put("378229", "27251");
        NTUToPublic.put("378230", "27261");
        NTUToPublic.put("378233", "27281");
        NTUToPublic.put("378237", "27311");
        NTUToPublic.put("382998", "27209");
        NTUToPublic.put("378202", "27011");
        NTUToPublic.put("378204", "27031");
        //=================== Blue
        NTUToPublic.put("378225", "27219");
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
        NTUToPublic.put("377906", "22529");
        NTUToPublic.put("378233", "27281");
        NTUToPublic.put("378237", "27311");
        //NTUToPublic.put("383011", "63735");
        //NTUToPublic.put("383013", "63737");
        //NTUToPublic.put("383014", "63725");
        //=================== Brown
        NTUToPublic.put("377906", "22529");
        NTUToPublic.put("378233", "27281");
        NTUToPublic.put("378237", "27311");
        //NTUToPublic.put("383011", "63735");
        //NTUToPublic.put("383013", "63737");
        NTUToPublic.put("378207", "27061");
        NTUToPublic.put("378224", "27211");
        NTUToPublic.put("383015", "27221");
        //NTUToPublic.put("382995", "63741");
        NTUToPublic.put("378227", "27231");
        NTUToPublic.put("378228", "27241");
        NTUToPublic.put("378229", "27251");
        NTUToPublic.put("378230", "27261");
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
        if (NUSToPublic.containsKey(key)) {
            return NUSToPublic.get(key);
        } else if (NTUToPublic.containsKey(key)) {
            return NTUToPublic.get(key);
        } else {
            return null;
        }
    }
}
