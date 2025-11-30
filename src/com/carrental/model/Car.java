package com.carrental.model;

public class Car {
    private int id;
    private String name;
    private double price;
    private String image;
    private String features;

    public Car() {
    }

    public Car(int id, String name, double price, String image, String features) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.image = image;
        this.features = features;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getFeatures() {
        return features;
    }

    public void setFeatures(String features) {
        this.features = features;
    }
}
