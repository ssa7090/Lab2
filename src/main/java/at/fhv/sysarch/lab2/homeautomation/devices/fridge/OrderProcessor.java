package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.math.BigDecimal;
import java.util.Optional;

public class OrderProcessor extends AbstractBehavior<OrderProcessor.OrderProcessorCommand> {
    public interface OrderProcessorCommand {}

    public static final class UpdateWeight implements OrderProcessorCommand {
        final BigDecimal weight;

        public UpdateWeight(BigDecimal weight) {
            this.weight = weight;
        }
    }

    public static final class UpdateSpace implements OrderProcessorCommand {
        final int amount;

        public UpdateSpace(int amount) {
            this.amount = amount;
        }
    }

    public static final class WrappedWeightSensorResponse implements OrderProcessor.OrderProcessorCommand {
        final WeightSensor.WeightSensorResponse weightSensorResponse;

        public WrappedWeightSensorResponse(WeightSensor.WeightSensorResponse weightSensorResponse) {
            this.weightSensorResponse = weightSensorResponse;
        }
    }

    public static final class WrappedSpaceSensorResponse implements OrderProcessor.OrderProcessorCommand {
        final SpaceSensor.SpaceSensorResponse spaceSensorResponse;

        public WrappedSpaceSensorResponse(SpaceSensor.SpaceSensorResponse spaceSensorResponse) {
            this.spaceSensorResponse = spaceSensorResponse;
        }
    }

    private static final BigDecimal MAX_WEIGHT = BigDecimal.valueOf(10000);
    private static final int MAX_SPACE = 25;

    private final Order order;

    private final ActorRef originator;

    private final ActorRef<Fridge.FridgeCommand> fridge;

    private Optional<BigDecimal> actualWeight = Optional.empty();

    private Optional<Integer> actualAmount = Optional.empty();

    public OrderProcessor(ActorContext<OrderProcessorCommand> context, Order order, ActorRef originator, ActorRef<Fridge.FridgeCommand> fridge, ActorRef<WeightSensor.WeightSensorCommand> weightSensor, ActorRef<SpaceSensor.SpaceSensorCommand> spaceSensor) {
        super(context);
        getContext().getLog().info("OrderProcessor started");

        this.originator = originator;
        this.order = order;
        this.fridge = fridge;
        weightSensor.tell(new WeightSensor.WeightSensorValueRequest(getContext().getSelf()));
        spaceSensor.tell(new SpaceSensor.SpaceSensorAmountRequest(getContext().getSelf()));
    }

    public static Behavior<OrderProcessorCommand> create(Order order, ActorRef originator, ActorRef<Fridge.FridgeCommand> fridge, ActorRef<WeightSensor.WeightSensorCommand> weightSensor, ActorRef<SpaceSensor.SpaceSensorCommand> spaceSensor) {
        return Behaviors.setup(context -> new OrderProcessor(context, order, originator, fridge, weightSensor, spaceSensor));
    }

    @Override
    public Receive<OrderProcessorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(WrappedWeightSensorResponse.class, this::onWrappedWeightSensorResponse)
                .onMessage(WrappedSpaceSensorResponse.class, this::onWrappedSpaceSensorResponse)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<OrderProcessorCommand> onWrappedWeightSensorResponse(WrappedWeightSensorResponse wrapped) {
        WeightSensor.WeightSensorResponse response = wrapped.weightSensorResponse;
        if (response instanceof WeightSensor.WeightSensorValueResponse) {
            this.actualWeight = Optional.of(((WeightSensor.WeightSensorValueResponse)response).value);
        } else {
            return Behaviors.unhandled();
        }
        return attemptOrderProcess();
    }

    private Behavior<OrderProcessorCommand> onWrappedSpaceSensorResponse(WrappedSpaceSensorResponse wrapped) {
        SpaceSensor.SpaceSensorResponse response = wrapped.spaceSensorResponse;
        if (response instanceof SpaceSensor.SpaceSensorAmountResponse) {
            this.actualAmount = Optional.of(((SpaceSensor.SpaceSensorAmountResponse)response).amount);
        } else {
            return Behaviors.unhandled();
        }
        return attemptOrderProcess();
    }

    private Behavior<OrderProcessorCommand> attemptOrderProcess() {
        if (this.actualWeight.isPresent() && this.actualAmount.isPresent()) {
            BigDecimal availableWeight = MAX_WEIGHT.subtract(this.actualWeight.get());
            int availableSpaceAmount = MAX_SPACE - this.actualAmount.get();

            boolean isEnoughWeightAvailable = order.getTotalWeight().compareTo(availableWeight) <= 0;
            boolean isEnoughSpaceAvailable = order.getAmount() <= availableSpaceAmount;

            if (isEnoughWeightAvailable && isEnoughSpaceAvailable) {
                this.fridge.tell(new Fridge.OrderProcessSuccess(originator, new Receipt(order, order.getTotalPrice())));
            } else {
                this.fridge.tell(new Fridge.OrderProcessFailure(originator, !isEnoughWeightAvailable ? "Not enough weight available to process order" : "Not enough space available to process order"));
            }

            return Behaviors.stopped();
        }
        return this;
    }

    private OrderProcessor onPostStop() {
        getContext().getLog().info("OrderProcessor child actor stopped");
        return this;
    }
}
