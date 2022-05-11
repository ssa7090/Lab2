package at.fhv.sysarch.lab2.homeautomation.environment;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.time.Duration;
import java.util.Random;

public class TemperatureEnvironment extends AbstractBehavior<TemperatureEnvironment.TemperatureEnvironmentCommand> {

    public interface TemperatureEnvironmentCommand {}

    public static final class TemperatureChanger implements TemperatureEnvironmentCommand {
        final double currentTemperature;

        public TemperatureChanger(double currentTemperature) {
            this.currentTemperature = currentTemperature;
        }
    }

    private double temperature = 10;

    private static final Random random = new Random();
    private static final double TEMP_MIN = -5.5;
    private static final double TEMP_MAX = 25;

    private final TimerScheduler<TemperatureEnvironmentCommand> temperatureTimeScheduler;

    public static Behavior<TemperatureEnvironmentCommand> create() {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new TemperatureEnvironment(context, timers)));
    }

    private TemperatureEnvironment(ActorContext<TemperatureEnvironmentCommand> context, TimerScheduler<TemperatureEnvironmentCommand> temperatureTimeScheduler) {
        super(context);
        this.temperatureTimeScheduler = temperatureTimeScheduler;
        this.temperatureTimeScheduler.startTimerAtFixedRate(new TemperatureChanger(temperature), Duration.ofSeconds(5));
        getContext().getLog().info("TemperatureEnvironment started");
    }

    @Override
    public Receive<TemperatureEnvironmentCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(TemperatureChanger.class, this::onTemperatureChange)
                .build();
    }

    private Behavior<TemperatureEnvironmentCommand> onTemperatureChange(TemperatureChanger tc) {
        double currTemp = tc.currentTemperature;
        // TODO: change temperature over time
        double nextTemp = random.nextDouble() * (TEMP_MAX - TEMP_MIN) + TEMP_MIN;
        getContext().getLog().info("TemperatureChange received {}", currTemp);

        this.temperature = nextTemp;
        getContext().getLog().info("Temperature changed to {}", nextTemp);
        return this;
    }
}
