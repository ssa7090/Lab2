package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import java.math.BigDecimal;

public class Receipt {
    private final Order order;
    private final BigDecimal totalPrice;

    public Receipt(Order order, BigDecimal totalPrice) {
        this.order = order;
        this.totalPrice = totalPrice;
    }

    public Order getOrder() {
        return order;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    @Override
    public String toString() {
        return "Receipt{" +
                "order=" + order +
                ", totalPrice=" + totalPrice +
                '}';
    }
}
