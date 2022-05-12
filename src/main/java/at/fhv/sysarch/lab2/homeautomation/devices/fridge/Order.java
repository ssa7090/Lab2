package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import java.math.BigDecimal;

public class Order {
    private final Product product;
    private final int amount;

    public Order(Product product, int amount) {
        this.product = product;
        this.amount = amount;
    }

    public Product getProduct() {
        return product;
    }

    public int getAmount() {
        return amount;
    }

    public BigDecimal getTotalWeight() {
        return product.getWeight().multiply(BigDecimal.valueOf(amount));
    }

    public BigDecimal getTotalPrice() {
        return product.getPrice().multiply(BigDecimal.valueOf(amount));
    }

    @Override
    public String toString() {
        return "Order{" +
                "product=" + product +
                ", amount=" + amount +
                '}';
    }
}
