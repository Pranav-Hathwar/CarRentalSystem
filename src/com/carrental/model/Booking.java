package com.carrental.model;

import java.sql.Date;
import java.sql.Timestamp;

public class Booking {
    private int id;
    private int userId;
    private int carId;
    private Date startDate;
    private Date endDate;
    private double totalPrice;
    private String status;
    private Timestamp pickupDatetime;
    private Timestamp dropoffDatetime;
    private String drivingLicensePath;
    private Timestamp createdAt;
    private String paymentStatus; // UNPAID, PAYMENT_REQUESTED, PAID

    public Booking() {
    }

    public Booking(int id, int userId, int carId, Date startDate, Date endDate, double totalPrice, String status) {
        this.id = id;
        this.userId = userId;
        this.carId = carId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalPrice = totalPrice;
        this.status = status;
    }

    public Booking(int id, int userId, int carId, Date startDate, Date endDate, Timestamp pickupDatetime, Timestamp dropoffDatetime, double totalPrice, String status, String drivingLicensePath, Timestamp createdAt) {
        this.id = id;
        this.userId = userId;
        this.carId = carId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.pickupDatetime = pickupDatetime;
        this.dropoffDatetime = dropoffDatetime;
        this.totalPrice = totalPrice;
        this.status = status;
        this.drivingLicensePath = drivingLicensePath;
        this.createdAt = createdAt;
        this.paymentStatus = "UNPAID";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getCarId() {
        return carId;
    }

    public void setCarId(int carId) {
        this.carId = carId;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
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

    public Timestamp getPickupDatetime() {
        return pickupDatetime;
    }

    public void setPickupDatetime(Timestamp pickupDatetime) {
        this.pickupDatetime = pickupDatetime;
    }

    public Timestamp getDropoffDatetime() {
        return dropoffDatetime;
    }

    public void setDropoffDatetime(Timestamp dropoffDatetime) {
        this.dropoffDatetime = dropoffDatetime;
    }

    public String getDrivingLicensePath() {
        return drivingLicensePath;
    }

    public void setDrivingLicensePath(String drivingLicensePath) {
        this.drivingLicensePath = drivingLicensePath;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
}
