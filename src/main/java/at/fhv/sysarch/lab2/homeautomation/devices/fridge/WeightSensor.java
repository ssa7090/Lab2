package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.math.BigDecimal;

public class WeightSensor extends AbstractBehavior<WeightSensor.WeightSensorCommand> {

    public interface WeightSensorCommand {}

    public interface WeightSensorRequest extends WeightSensorCommand {

    }

    public interface WeightSensorResponse extends WeightSensorCommand {}

    public static final class WeightSensorValueRequest implements WeightSensorRequest {
        ActorRef<OrderProcessor.OrderProcessorCommand> replyTo;

        public WeightSensorValueRequest(ActorRef<OrderProcessor.OrderProcessorCommand> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static final class WeightSensorValueResponse implements WeightSensorResponse {
        final BigDecimal value;

        public WeightSensorValueResponse(BigDecimal value) {
            this.value = value;
        }
    }

    public static final class ChangeWeightSensor implements WeightSensorCommand {
        final BigDecimal weightToChange;

        public ChangeWeightSensor(BigDecimal weightToChange) {
            this.weightToChange = weightToChange;
        }
    }

    public static Behavior<WeightSensorCommand> create(String groupId, String deviceId) {
        return Behaviors.setup(context -> new WeightSensor(context, groupId, deviceId));
    }

    private final String groupId;
    private final String deviceId;
    private BigDecimal weight = BigDecimal.ZERO;

    public WeightSensor(ActorContext<WeightSensorCommand> context, String groupId, String deviceId) {
        super(context);
        this.groupId = groupId;
        this.deviceId = deviceId;

        getContext().getLog().info("WeightSensor started");
    }

    @Override
    public Receive<WeightSensorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ChangeWeightSensor.class, this::onChangeWeight)
                .onMessage(WeightSensorValueRequest.class, this::onWeightValueRequest)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<WeightSensorCommand> onWeightValueRequest(WeightSensorValueRequest wvr) {
        wvr.replyTo.tell(new OrderProcessor.WrappedWeightSensorResponse(new WeightSensorValueResponse(this.weight)));
        return this;
    }

    private Behavior<WeightSensorCommand> onChangeWeight(ChangeWeightSensor cw) {
        getContext().getLog().info("WeightSensor received weight to change: {}", cw.weightToChange);
        this.weight.add(cw.weightToChange);
        return this;
    }

    private WeightSensor onPostStop() {
        getContext().getLog().info("WeightSensor actor {}-{} stopped", groupId, deviceId);
        return this;
    }
}