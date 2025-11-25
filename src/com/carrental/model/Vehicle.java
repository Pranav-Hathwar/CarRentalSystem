package com.carrental.model;

public abstract class Vehicle {
    private int id;
    private String make;
    private String model;
    private double basePricePerDay;

    public Vehicle(int id, String make, String model, double basePricePerDay) {
        this.id = id;
        this.make = make;
        this.model = model;
        this.basePricePerDay = basePricePerDay;
    }

    public int getId() {
        return id;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public double getBasePricePerDay() {
        return basePricePerDay;
    }

    public abstract double calculatePrice(int days);

    @Override
    public String toString() {
        return id + ": " + make + " " + model + " ($" + basePricePerDay + "/day)";
    }
}
