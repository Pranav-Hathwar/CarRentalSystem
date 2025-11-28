package com.carrental.service;

import com.carrental.model.Car;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class VehicleService {
    private List<Car> cars;

    public VehicleService() {
        this.cars = new ArrayList<>();
        // Pre-populate with some cars
        addCar(new Car(1, "Toyota", "Camry", 5000, "images/camry.jpg"));
        addCar(new Car(2, "Honda", "Civic", 4500, "images/civic.jpg"));
        addCar(new Car(3, "Ford", "Mustang", 8000, "images/mustang.jpg"));
        addCar(new Car(4, "Tesla", "Model 3", 5000, "images/tesla.jpg"));
        addCar(new Car(5, "Chevrolet", "Malibu", 900, "images/malibu.jpg"));
    }

    public void addCar(Car car) {
        cars.add(car);
    }

    public List<Car> getAvailableCars() {
        return cars.stream()
                .filter(Car::isAvailable)
                .collect(Collectors.toList());
    }

    public Optional<Car> findCarById(int id) {
        return cars.stream()
                .filter(c -> c.getId() == id)
                .findFirst();
    }

    public List<Car> getAllCars() {
        return new ArrayList<>(cars);
    }

    public boolean deleteCar(int id) {
        return cars.removeIf(car -> car.getId() == id);
    }
}
