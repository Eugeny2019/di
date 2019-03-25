package io.khasang.reflection;

public class AirConditioner {
    private CarDashboard dashboard = null;

    public AirConditioner() {
    }

    public CarDashboard getDashboard(){
        return dashboard;
    }

    public void setDashboard(CarDashboard dashboard){
        this.dashboard = dashboard;
    }

    @Override
    public String toString() {
        return "AirConditioner{" +
                "dashboard='" + dashboard.getClass().getSimpleName() + dashboard.getClass().getSimpleName() + '\'' +
                '}';
    }
}
