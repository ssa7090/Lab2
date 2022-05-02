package at.fhv.sysarch.lab2.homeautomation.environment;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.time.Duration;
import java.util.Optional;


public class Environment extends AbstractBehavior<Environment.EnvironmentCommand> {

    public interface EnvironmentCommand {}

    public static final class TemperatureChange implements EnvironmentCommand {
        final Optional<Double> value;

        public TemperatureChange (Optional<Double> value) {
            this.value = value;
        }
    }

    public static final class WeatherChange implements EnvironmentCommand {
        final Optional<Weather> value;

        public WeatherChange (Optional<Weather> value) {
            this.value = value;
        }
    }

    private TimerScheduler<EnvironmentCommand> temperatureTimeScheduler;
    private TimerScheduler<EnvironmentCommand> weatherTimeScheduler;

    private double temperature = 20.0;
    private Weather weather = Weather.SUNNY;

    public static Behavior<EnvironmentCommand> create() {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new Environment(context, timers, timers)));
    }

    private Environment(ActorContext<EnvironmentCommand> context, TimerScheduler<EnvironmentCommand> temperatureTimeScheduler, TimerScheduler<EnvironmentCommand> weatherTimeScheduler) {
        super(context);
        this.temperatureTimeScheduler = temperatureTimeScheduler;
        this.weatherTimeScheduler = weatherTimeScheduler;
        this.temperatureTimeScheduler.startTimerAtFixedRate(new TemperatureChange(Optional.of(temperature)), Duration.ofSeconds(5));
        this.weatherTimeScheduler.startTimerAtFixedRate(new WeatherChange(Optional.of(Weather.SUNNY)), Duration.ofSeconds(20));

        getContext().getLog().info("Environment started");
    }

    @Override
    public Receive<EnvironmentCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(TemperatureChange.class, this::onTemperatureChange)
                .onMessage(WeatherChange.class, this::onWeatherChange)
                .build();
    }

    private Behavior<EnvironmentCommand> onTemperatureChange (TemperatureChange tc) {
        getContext().getLog().info("TemperatureChange received {}", tc.value.get());
        this.temperature = tc.value.get();
        return this;
    }

    private Behavior<EnvironmentCommand> onWeatherChange (WeatherChange wc) {
        getContext().getLog().info("WeatherChange received {}", wc.value.get());
        this.weather = wc.value.get();
        return this;
    }
}
