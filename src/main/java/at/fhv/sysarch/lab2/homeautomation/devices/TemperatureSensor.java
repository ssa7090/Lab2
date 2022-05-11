/*package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.environment.Environment;
import at.fhv.sysarch.lab2.homeautomation.environment.Temperature;

import java.util.Optional;

public class TemperatureSensor extends AbstractBehavior<TemperatureSensor.TemperatureCommand> {

    public interface TemperatureCommand {}

    public static final class ReadTemperature implements TemperatureCommand {
        final Optional<Temperature> temperature;

        public ReadTemperature(Optional<Temperature> temperature) {
            this.temperature = temperature;
        }
    }

    public static final class WrappedEnvironmentTemperatureReadResponse implements TemperatureCommand {
        final Environment.TemperatureReadResponse response;

        public WrappedEnvironmentTemperatureReadResponse(Environment.TemperatureReadResponse response) {
            this.response = response;
        }
    }

    public static Behavior<TemperatureCommand> create(ActorRef<AirCondition.AirConditionCommand> airCondition, String groupId, String deviceId) {
        return Behaviors.setup(context -> new TemperatureSensor(context, airCondition, groupId, deviceId));
    }

    private final String groupId;
    private final String deviceId;
    private ActorRef<AirCondition.AirConditionCommand> airCondition;

    public TemperatureSensor(ActorContext<TemperatureCommand> context, ActorRef<AirCondition.AirConditionCommand> airCondition, String groupId, String deviceId) {
        super(context);
        this.airCondition = airCondition;
        this.groupId = groupId;
        this.deviceId = deviceId;

        context.messageAdapter(Environment.TemperatureReadResponse.class, WrappedEnvironmentTemperatureReadResponse::new);
        getContext().getLog().info("TemperatureSensor started");
    }

    @Override
    public Receive<TemperatureCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ReadTemperature.class, this::onReadTemperature)
                .onMessage(WrappedEnvironmentTemperatureReadResponse.class, this::onWrappedEnvironmentTemperatureReadResponse)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<TemperatureCommand> onReadTemperature(ReadTemperature r) {
        getContext().getLog().info("TemperatureSensor received {}", r.temperature.get());
        this.airCondition.tell(new AirCondition.EnrichedTemperature(Optional.of(r.temperature.get())));
        return this;
    }

    private Behavior<TemperatureCommand> onWrappedEnvironmentTemperatureReadResponse(WrappedEnvironmentTemperatureReadResponse r) {
        getContext().getLog().info("TemperatureSensor received {}", r.response.get());
        this.airCondition.tell(new AirCondition.EnrichedTemperature(Optional.of(r.temperature.get())));
        return this;
    }

    private TemperatureSensor onPostStop() {
        getContext().getLog().info("TemperatureSensor actor {}-{} stopped", groupId, deviceId);
        return this;
    }
}

 */
