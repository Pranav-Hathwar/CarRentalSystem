package com.carrental.model;

public class Booking {
    private long id;
    private int carId;
    private String customerName;
    private String pickupTime;
    private String dropoffTime;
    private double totalPrice;
    private String status;
    private String bookingDate;

    public Booking(long id, int carId, String customerName, String pickupTime, String dropoffTime, double totalPrice) {
        this.id = id;
        this.carId = carId;
        this.customerName = customerName;
        this.pickupTime = pickupTime;
        this.dropoffTime = dropoffTime;
        this.totalPrice = totalPrice;
        this.status = "Confirmed";
        this.bookingDate = new java.util.Date().toString();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getCarId() {
        return carId;
    }

    public void setCarId(int carId) {
        this.carId = carId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPickupTime() {
        return pickupTime;
    }

    public void setPickupTime(String pickupTime) {
        this.pickupTime = pickupTime;
    }

    public String getDropoffTime() {
        return dropoffTime;
    }

    public void setDropoffTime(String dropoffTime) {
        this.dropoffTime = dropoffTime;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(String bookingDate) {
        this.bookingDate = bookingDate;
    }
}
