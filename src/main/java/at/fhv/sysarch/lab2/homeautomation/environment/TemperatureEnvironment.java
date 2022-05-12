package at.fhv.sysarch.lab2.homeautomation.environment;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.devices.TemperatureSensor;
import at.fhv.sysarch.lab2.homeautomation.devices.WeatherSensor;

import java.time.Duration;
import java.util.Random;

public class TemperatureEnvironment extends AbstractBehavior<TemperatureEnvironment.TemperatureEnvironmentCommand> {

    public interface TemperatureEnvironmentCommand {}

    public static final class TemperatureAutoChange implements TemperatureEnvironmentCommand {

    }

    public static final class TemperatureManualChange implements  TemperatureEnvironmentCommand {
        final double temperature;

        public TemperatureManualChange(double temperature) {
            this.temperature = temperature;
        }
    }

    private double temperature = 10;
    private boolean increasing = true;

    private static final Random random = new Random();
    private static final double TEMP_MIN = -5.5;
    private static final double TEMP_MAX = 25;

    private final ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor;
    private final TimerScheduler<TemperatureEnvironmentCommand> temperatureTimeScheduler;

    public static Behavior<TemperatureEnvironmentCommand> create(ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor) {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new TemperatureEnvironment(context, temperatureSensor, timers)));
    }

    private TemperatureEnvironment(ActorContext<TemperatureEnvironmentCommand> context, ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor, TimerScheduler<TemperatureEnvironmentCommand> temperatureTimeScheduler) {
        super(context);
        this.temperatureSensor = temperatureSensor;
        this.temperatureTimeScheduler = temperatureTimeScheduler;
        this.temperatureTimeScheduler.startTimerAtFixedRate(new TemperatureAutoChange(), Duration.ofSeconds(20));
        getContext().getLog().info("TemperatureEnvironment started");
    }

    @Override
    public Receive<TemperatureEnvironmentCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(TemperatureAutoChange.class, this::onAutoTemperatureChange)
                .onMessage(TemperatureManualChange.class, this::onManualTemperatureChange)
                .build();
    }

    private Behavior<TemperatureEnvironmentCommand> onAutoTemperatureChange(TemperatureAutoChange tac) {
        double currTemp = temperature;
        // one tenth temperature change fluctuation range
        double tempChange = random.nextDouble() * ((TEMP_MAX - TEMP_MIN) / 10);
        double nextTemp;

        getContext().getLog().info("TemperatureChange received {}", currTemp);

        if (increasing) {
            nextTemp = currTemp + tempChange;

            if (nextTemp > TEMP_MAX) {
                nextTemp = currTemp - tempChange;
                increasing = false;
            }
        } else {
            nextTemp = currTemp - tempChange;

            if (nextTemp < TEMP_MIN) {
                nextTemp = currTemp + tempChange;
                increasing = true;
            }
        }

        this.temperature = nextTemp;
        getContext().getLog().info("Temperature changed to {}", nextTemp);
        this.temperatureSensor.tell(new TemperatureSensor.ChangeTemperature(this.temperature));
        return this;
    }

    private Behavior<TemperatureEnvironmentCommand> onManualTemperatureChange(TemperatureManualChange tmc) {
        this.temperature = tmc.temperature;
        this.temperatureSensor.tell(new TemperatureSensor.ChangeTemperature(this.temperature));
        return this;
    }
}
