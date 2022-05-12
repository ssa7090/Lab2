package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import java.math.BigDecimal;

public class Product {
    private final String productName;
    private final BigDecimal price;
    private final BigDecimal weight;

    public Product(String productName, BigDecimal price, BigDecimal weight) {
        this.productName = productName;
        this.price = price;
        this.weight = weight;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        return "Product{" +
                "productName='" + productName + '\'' +
                ", price=" + price +
                ", weight=" + weight +
                '}';
    }
}
