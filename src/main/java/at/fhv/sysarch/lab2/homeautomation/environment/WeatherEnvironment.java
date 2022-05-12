package at.fhv.sysarch.lab2.homeautomation.environment;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.devices.AirCondition;
import at.fhv.sysarch.lab2.homeautomation.devices.WeatherSensor;

import java.time.Duration;
import java.util.Optional;


public class WeatherEnvironment extends AbstractBehavior<WeatherEnvironment.WeatherEnvironmentCommand> {

    public interface WeatherEnvironmentCommand {}

    public static final class WeatherAutoChange implements WeatherEnvironmentCommand {

    }

    public static final class WeatherManualChange implements WeatherEnvironmentCommand {
        final Weather weather;

        public WeatherManualChange(Weather weather) {
            this.weather = weather;
        }
    }

    private Weather weather = Weather.SUNNY;

    private final ActorRef<WeatherSensor.WeatherCommand> weatherSensor;
    private final TimerScheduler<WeatherEnvironment.WeatherEnvironmentCommand> weatherTimeScheduler;

    public static Behavior<WeatherEnvironmentCommand> create(ActorRef<WeatherSensor.WeatherCommand> weatherSensor) {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new WeatherEnvironment(context, weatherSensor, timers)));
    }

    private WeatherEnvironment(ActorContext<WeatherEnvironmentCommand> context, ActorRef<WeatherSensor.WeatherCommand> weatherSensor, TimerScheduler<WeatherEnvironment.WeatherEnvironmentCommand> weatherTimeScheduler) {
        super(context);
        this.weatherSensor = weatherSensor;
        // TODO: Herausfinden, warum die initialen Werte erhalten bleiben bzw. wie man auf die geänderten Werte kommen kann
        this.weatherTimeScheduler = weatherTimeScheduler;
        this.weatherTimeScheduler.startTimerAtFixedRate(new WeatherAutoChange(), Duration.ofSeconds(30));
        getContext().getLog().info("WeatherEnvironment started");
    }

    @Override
    public Receive<WeatherEnvironmentCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(WeatherAutoChange.class, this::onWeatherAutoChange)
                .onMessage(WeatherManualChange.class, this::onWeatherManualChange)
                .build();
    }

    private Behavior<WeatherEnvironmentCommand> onWeatherAutoChange(WeatherAutoChange wac) {
        Weather currWeather = weather;
        Weather nextWeather = currWeather == Weather.SUNNY ? Weather.CLOUDY : Weather.SUNNY;
        getContext().getLog().info("WeatherChange received {}", currWeather);

        this.weather = nextWeather;
        getContext().getLog().info("Weather changed to {}", nextWeather);
        this.weatherSensor.tell(new WeatherSensor.ChangeWeather(this.weather));
        return this;
    }

    private Behavior<WeatherEnvironmentCommand> onWeatherManualChange(WeatherManualChange wmc) {
        this.weather = wmc.weather;
        this.weatherSensor.tell(new WeatherSensor.ChangeWeather(this.weather));
        return this;
    }
}
