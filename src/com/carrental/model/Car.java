package com.carrental.model;

public class Car extends Vehicle {
    private boolean isAvailable;
    private String imagePath;

    public Car(int id, String make, String model, double basePricePerDay) {
        super(id, make, model, basePricePerDay);
        this.isAvailable = true;
        this.imagePath = "images/default.jpg"; // Default image
    }

    public Car(int id, String make, String model, double basePricePerDay, String imagePath) {
        super(id, make, model, basePricePerDay);
        this.isAvailable = true;
        this.imagePath = imagePath;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    @Override
    public double calculatePrice(int days) {
        return getBasePricePerDay() * days;
    }

    @Override
    public String toString() {
        return super.toString() + (isAvailable ? " [Available]" : " [Rented]");
    }
}
