package entity.ntubus;

import java.util.ArrayList;

public class Node {
    public String edge_id;
    public String element_id;
    public int id;
    public boolean is_stop_point;
    public double lat;
    public double lon;
    public String name;
    public int node_id;
    public int platform_id;
    public ArrayList<Coordinate> points;
    public String short_direction;
}
