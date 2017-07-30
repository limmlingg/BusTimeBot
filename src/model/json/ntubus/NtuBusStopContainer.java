package model.json.ntubus;

import java.util.ArrayList;

public class NtuBusStopContainer {
    public String external_id;
    public int id;
    public String name;
    //ignore name_en, name_ru, nameslug, resource_url
    public ArrayList<Coordinate> routevariant_geometry;
}
