package models;

import java.time.LocalDateTime;

public class Earthquake {
    private String id;
    private double depth;
    private String magnitudeType;
    private double magnitude;
    private String state;
    private LocalDateTime time;

    public Earthquake() {}

    public Earthquake(String id, double depth, String magnitudeType,
                      double magnitude, String state, LocalDateTime time) {
        this.id = id;
        this.depth = depth;
        this.magnitudeType = magnitudeType;
        this.magnitude = magnitude;
        this.state = state;
        this.time = time;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getDepth() { return depth; }
    public void setDepth(double depth) { this.depth = depth; }

    public String getMagnitudeType() { return magnitudeType; }
    public void setMagnitudeType(String magnitudeType) { this.magnitudeType = magnitudeType; }

    public double getMagnitude() { return magnitude; }
    public void setMagnitude(double magnitude) { this.magnitude = magnitude; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public LocalDateTime getTime() { return time; }
    public void setTime(LocalDateTime time) { this.time = time; }

    @Override
    public String toString() {
        return String.format("Earthquake{id='%s', magnitude=%.2f, state='%s', time=%s}",
                id, magnitude, state, time);
    }
}