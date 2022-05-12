package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.ui.UI;

import java.math.BigDecimal;
import java.util.*;

public class Fridge extends AbstractBehavior<Fridge.FridgeCommand> {
    public interface FridgeCommand {}

    public interface FridgeRequest extends FridgeCommand {}

    public interface FridgeResponse extends FridgeCommand {}

    public static final class OrderRequest implements FridgeRequest {
        ActorRef<UI.UICommand> replyTo;
        String productName;
        int amount;

        public OrderRequest(ActorRef<UI.UICommand> respondTo, String productName, int amount) {
            this.replyTo = respondTo;
            this.productName = productName;
            this.amount = amount;
        }
    }

    public static final class OrderResponse implements FridgeResponse {
        public final Optional<Receipt> receiptOpt;

        public final String errorMessage;

        public OrderResponse(Receipt receipt) {
            this.receiptOpt = Optional.of(receipt);
            errorMessage = "";
        }

        public OrderResponse(String errorMessage) {
            receiptOpt = Optional.empty();
            this.errorMessage = errorMessage;
        }
    }

    public static final class OrderProcessSuccess implements FridgeCommand {
        final ActorRef originator;
        final Receipt receipt;

        public OrderProcessSuccess(ActorRef originator, Receipt receipt) {
            this.originator = originator;
            this.receipt = receipt;
        }
    }

    public static final class OrderProcessFailure implements FridgeCommand {
        final ActorRef originator;
        final String failureMessage;

        public OrderProcessFailure(ActorRef originator, String failureMessage) {
            this.originator = originator;
            this.failureMessage = failureMessage;
        }
    }

    public static final class StoredProductsRequest implements FridgeRequest {
        final ActorRef<UI.UICommand> replyTo;

        public StoredProductsRequest(ActorRef<UI.UICommand> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static final class StoredProductsResponse implements FridgeResponse {
        public final List<Product> storedProducts;

        public StoredProductsResponse(List<Product> storedProducts) {
            this.storedProducts = storedProducts;
        }
    }

    private static final List<Product> PRODUCTS = Arrays.asList(
            new Product("apple", BigDecimal.valueOf(0.99), BigDecimal.valueOf(100)),
            new Product("pear", BigDecimal.valueOf(0.75), BigDecimal.valueOf(150)),
            new Product("cheese", BigDecimal.valueOf(11.99), BigDecimal.valueOf(1000)),
            new Product("sausage", BigDecimal.valueOf(9.50), BigDecimal.valueOf(400)),
            new Product("beer", BigDecimal.valueOf(3.50), BigDecimal.valueOf(500)),
            new Product("fish", BigDecimal.valueOf(11.99), BigDecimal.valueOf(600))
    );

    private final String groupId;
    private final String deviceId;
    private final ActorRef<WeightSensor.WeightSensorCommand> weightSensor;
    private final ActorRef<SpaceSensor.SpaceSensorCommand> spaceSensor;

    private List<Order> orderHistory = new ArrayList<>();
    private List<Product> storedProducts = new ArrayList<>();

    public Fridge(ActorContext<FridgeCommand> context, String groupId, String deviceId, ActorRef<WeightSensor.WeightSensorCommand> weightSensor, ActorRef<SpaceSensor.SpaceSensorCommand> spaceSensor) {
        super(context);
        this.groupId = groupId;
        this.deviceId = deviceId;
        this.weightSensor = weightSensor;
        this.spaceSensor = spaceSensor;
        Collections.copy(PRODUCTS, storedProducts);
        getContext().getLog().info("Fridge started");
    }

    public static Behavior<FridgeCommand> create(String groupId, String deviceId, ActorRef<WeightSensor.WeightSensorCommand> weightSensor, ActorRef<SpaceSensor.SpaceSensorCommand> spaceSensor) {
        return Behaviors.setup(context -> new Fridge(context, groupId, deviceId, weightSensor, spaceSensor));
    }

    @Override
    public Receive<FridgeCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(OrderRequest.class, this::onOrderRequest)
                .onMessage(OrderProcessSuccess.class, this::onOrderProcessSuccess)
                .onMessage(OrderProcessFailure.class, this::onOrderProcessFailure)
                .onMessage()
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<FridgeCommand> onOrderRequest(OrderRequest req) {
        Optional<Product> product = PRODUCTS.stream()
                .filter(p -> p.getProductName().equals(req.productName))
                .findFirst();

        if (product.isEmpty()) {
            req.replyTo.tell(new UI.WrappedFridgeResponse(new OrderResponse("Requested product to order not found in products list")));
        } else {
            getContext().getLog().info("Order request received, processing order..");
            Order order = new Order(product.get(), req.amount);
            getContext().spawn(OrderProcessor.create(order, req.replyTo, getContext().getSelf(), weightSensor, spaceSensor), "OrderProcessor_" + UUID.randomUUID());
        }

        return this;
    }

    private Behavior<FridgeCommand> onOrderProcessSuccess(OrderProcessSuccess ops) {
        getContext().getLog().info("Order successfully processed");
        Order order = ops.receipt.getOrder();
        orderHistory.add(order);
        storedProducts.add(order.getProduct());
        weightSensor.tell(new WeightSensor.ChangeWeightSensor(order.getTotalWeight()));
        spaceSensor.tell(new SpaceSensor.ChangeSpaceSensor(order.getAmount()));

        ops.originator.tell(new UI.WrappedFridgeResponse(new OrderResponse(ops.receipt)));
        return this;
    }

    private Behavior<FridgeCommand> onOrderProcessFailure(OrderProcessFailure opf) {
        getContext().getLog().info("Order processing failure");

        opf.originator.tell(new UI.WrappedFridgeResponse(new OrderResponse(opf.failureMessage)));
        return this;
    }

    private Fridge onPostStop() {
        getContext().getLog().info("Fridge actor {}-{} stopped", groupId, deviceId);
        return this;
    }
}
