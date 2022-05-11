package at.fhv.sysarch.lab2.homeautomation.environment;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.time.Duration;


public class WeatherEnvironment extends AbstractBehavior<WeatherEnvironment.WeatherEnvironmentCommand> {

    public interface WeatherEnvironmentCommand {}

    public static final class WeatherChanger implements WeatherEnvironmentCommand {
        final Weather currentWeather;

        public WeatherChanger(Weather currentWeather) {
            this.currentWeather = currentWeather;
        }
    }

    private Weather weather = Weather.SUNNY;

    private final TimerScheduler<WeatherEnvironment.WeatherEnvironmentCommand> weatherTimeScheduler;

    public static Behavior<WeatherEnvironmentCommand> create() {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new WeatherEnvironment(context,timers)));
    }

    private WeatherEnvironment(ActorContext<WeatherEnvironmentCommand> context, TimerScheduler<WeatherEnvironment.WeatherEnvironmentCommand> weatherTimeScheduler) {
        super(context);
        this.weatherTimeScheduler = weatherTimeScheduler;
        this.weatherTimeScheduler.startTimerAtFixedRate(new WeatherChanger(weather), Duration.ofSeconds(10));
        getContext().getLog().info("WeatherEnvironment started");
    }

    @Override
    public Receive<WeatherEnvironmentCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(WeatherChanger.class, this::onWeatherChange)
                .build();
    }

    private Behavior<WeatherEnvironmentCommand> onWeatherChange(WeatherChanger wc) {
        Weather currentWeather = wc.currentWeather;
        Weather nextWeather = currentWeather == Weather.SUNNY ? Weather.CLOUDY : Weather.SUNNY;
        getContext().getLog().info("WeatherChange received {}", currentWeather);

        this.weather = nextWeather;
        getContext().getLog().info("Weather changed to {}", nextWeather);
        return this;
    }
}
