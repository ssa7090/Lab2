package at.fhv.sysarch.lab2.homeautomation.environment;

public class Temperature {
    private double temperatureValue;
    private String temperatureUnit;

    public Temperature(double temperatureValue, String temperatureUnit) {
        this.temperatureValue = temperatureValue;
        this.temperatureUnit = temperatureUnit;
    }

    public double getTemperatureValue() {
        return temperatureValue;
    }

    public String getTemperatureUnit() {
        return temperatureUnit;
    }

    @Override
    public String toString() {
        return temperatureValue + " " + temperatureUnit;
    }
}
