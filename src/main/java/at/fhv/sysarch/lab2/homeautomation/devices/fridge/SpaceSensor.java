package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.math.BigDecimal;

public class SpaceSensor extends AbstractBehavior<SpaceSensor.SpaceSensorCommand> {

    public interface SpaceSensorCommand {}

    public interface SpaceSensorRequest extends SpaceSensorCommand {

    }

    public interface SpaceSensorResponse extends SpaceSensorCommand {}

    public static final class SpaceSensorAmountRequest implements SpaceSensorCommand {
        ActorRef<OrderProcessor.OrderProcessorCommand> replyTo;

        public SpaceSensorAmountRequest(ActorRef<OrderProcessor.OrderProcessorCommand> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static final class SpaceSensorAmountResponse implements SpaceSensorResponse {
        final int amount;

        public SpaceSensorAmountResponse(int amount) {
            this.amount = amount;
        }
    }

    public static final class ChangeSpaceSensor implements SpaceSensorCommand {
        final int spaceToChange;

        public ChangeSpaceSensor(int spaceToChange) {
            this.spaceToChange = spaceToChange;
        }
    }

    public static Behavior<SpaceSensorCommand> create(String groupId, String deviceId) {
        return Behaviors.setup(context -> new SpaceSensor(context, groupId, deviceId));
    }

    private final String groupId;
    private final String deviceId;
    private int space = 0;

    public SpaceSensor(ActorContext<SpaceSensorCommand> context, String groupId, String deviceId) {
        super(context);
        this.groupId = groupId;
        this.deviceId = deviceId;

        getContext().getLog().info("SpaceSensor started");
    }

    @Override
    public Receive<SpaceSensorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ChangeSpaceSensor.class, this::onChangeSpace)
                .onMessage(SpaceSensorAmountRequest.class, this::onSpaceAmountRequest)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<SpaceSensorCommand> onSpaceAmountRequest(SpaceSensorAmountRequest sar) {
        sar.replyTo.tell(new OrderProcessor.WrappedSpaceSensorResponse(new SpaceSensorAmountResponse(this.space)));
        return this;
    }

    private Behavior<SpaceSensorCommand> onChangeSpace(ChangeSpaceSensor cs) {
        getContext().getLog().info("SpaceSensor received space to change: {}", cs.spaceToChange);
        this.space += (cs.spaceToChange);
        return this;
    }

    private SpaceSensor onPostStop() {
        getContext().getLog().info("SpaceSensor actor {}-{} stopped", groupId, deviceId);
        return this;
    }
}