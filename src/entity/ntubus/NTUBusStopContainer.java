package entity.ntubus;

import java.util.ArrayList;

public class NTUBusStopContainer {
    public String external_id;
    public int id;
    public String name;
    //ignore name_en, name_ru, nameslug, resource_url
    public ArrayList<Coordinate> routevariant_geometry;
}
