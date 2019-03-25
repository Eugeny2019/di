package io.khasang.reflection;

import io.khasang.reflection.di.Auto;

public class Car {
//    @Deprecated

    private String vin = null;

//    @Auto(isRequired = true)
    @Auto
    private Engine engine = null;

//    @Auto(isRequired = true)
    @Auto
    private Gear gear = null;

//    @Auto(isRequired = true)
    @Auto
    private AirConditioner conditioner = null;

//    @Auto(isRequired = true)
    @Auto
    private CarDashboard dashboard = null;

    public Car() {
    }

//    @Auto
    public Engine getEngine() {
        return engine;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    public Gear getGear() {
        return gear;
    }

    public void setGear(Gear gear) {
        this.gear = gear;
    }

    @Override
    public String toString() {
        return "Car{" +
                "vin=" + vin +
                ", engine=" + engine +
                ", gear=" + gear +
                ", conditioner=" + conditioner +
                ", dashboard=" + dashboard +
                '}';
    }

    public AirConditioner getConditioner() {
        return conditioner;
    }

    public void setConditioner(AirConditioner conditioner) {
        this.conditioner = conditioner;
    }

    public CarDashboard getDashboard() {
        return dashboard;
    }

    public void setDashboard(CarDashboard dashboard) {
        this.dashboard = dashboard;
    }
}
