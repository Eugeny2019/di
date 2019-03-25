package io.khasang.reflection;

public class CarDashboard {
    private AirConditioner conditioner = null;

    public CarDashboard() {
    }

    public AirConditioner getConditioner(){
        return conditioner;
    }

    public void setConditioner(AirConditioner conditioner){
        this.conditioner = conditioner;
    }

    @Override
    public String toString() {
        return "AirConditioner{" +
                "conditioner='" + conditioner.getClass().getSimpleName() + '\'' +
                '}';
    }
}
