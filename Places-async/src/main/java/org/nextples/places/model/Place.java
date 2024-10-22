package org.nextples.places.model;

public class Place {
    private final String xid;
    private final String name;
    private final double lat;
    private final double lon;
    private String description;


    public Place(String xid, String name, double lat, double lon) {
        this.xid = xid;
        this.name = name;
        this.lat = lat;
        this.lon = lon;
    }

    public String getXid() {
        return xid;
    }

    public String getName() {
        return name;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
