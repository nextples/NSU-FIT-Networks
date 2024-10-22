package org.nextples.places.model;

public class Weather {
    private final String locationName;
    private final String description;
    private final double temperature;

    public Weather(String locationName, String description, double temperature) {
        this.locationName = locationName;
        this.description = description;
        this.temperature = temperature;
    }

    public String getLocationName() {
        return locationName;
    }

    public String getDescription() {
        return description;
    }

    public double getTemperature() {
        return temperature;
    }
}
