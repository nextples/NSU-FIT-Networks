package org.nextples.places.model;

public class Location {
    private final String name;
    private final double lat;
    private final double lon;
    private final String country;

    public Location(String name, double lat, double lon, String country) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.country = country;
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

    public String getCountry() {
        return country;
    }
}
