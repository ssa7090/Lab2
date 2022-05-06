package at.fhv.sysarch.lab2.homeautomation.environment;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Optional;
import java.util.Random;


public class Environment extends AbstractBehavior<Environment.EnvironmentCommand> {

    public interface EnvironmentCommand {}

    public static final class SeasonalCycleChanger implements EnvironmentCommand {
        final Optional<Integer> currentSeasonalCycle;

        public SeasonalCycleChanger(Optional<Integer> currentSeasonalCycle) {
            this.currentSeasonalCycle = currentSeasonalCycle;
        }
    }

    public static final class TemperatureChanger implements EnvironmentCommand {
        final Optional<Double> currentTemperature;

        public TemperatureChanger(Optional<Double> currentTemperature) {
            this.currentTemperature = currentTemperature;
        }
    }

    public static final class WeatherChanger implements EnvironmentCommand {
        final Optional<Weather> currentWeather;

        public WeatherChanger(Optional<Weather> currentWeather) {
            this.currentWeather = currentWeather;
        }
    }

    private final Random random = new Random();
    private final double[] TEMP_SEASONAL_MEANS = { -2.2, -1.3, 7.4, 12.1, 15.9, 17.3, 17.1, 13.2, 9.2, 3.4, -1 };
    private final double TEMP_SEASONAL_MEAN_DELTA = 3.0;

    private int seasonalCycle = 0;
    private double temperature = nextRandomTemperatureDelta(TEMP_SEASONAL_MEANS[seasonalCycle]);
    private Weather weather = Weather.SUNNY;

    public static Behavior<EnvironmentCommand> create() {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new Environment(context, timers)));
    }

    private Environment(ActorContext<EnvironmentCommand> context, TimerScheduler<EnvironmentCommand> timerScheduler) {
        super(context);
        timerScheduler.startTimerAtFixedRate(new SeasonalCycleChanger(Optional.of(seasonalCycle)), Duration.ofSeconds(15));
        timerScheduler.startTimerAtFixedRate(new TemperatureChanger(Optional.of(temperature)), Duration.ofSeconds(5));
        timerScheduler.startTimerAtFixedRate(new WeatherChanger(Optional.of(weather)), Duration.ofSeconds(10));
        getContext().getLog().info("Environment started");
    }

    @Override
    public Receive<EnvironmentCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(SeasonalCycleChanger.class, this::onSeasonalCycleChange)
                .onMessage(TemperatureChanger.class, this::onTemperatureChange)
                .onMessage(WeatherChanger.class, this::onWeatherChange)
                .build();
    }

    private Behavior<EnvironmentCommand> onSeasonalCycleChange(SeasonalCycleChanger scc) {
        int currSeasonalCycle = scc.currentSeasonalCycle.get();
        int nextSeasonalCycle = (seasonalCycle + 1) % TEMP_SEASONAL_MEANS.length;
        getContext().getLog().info("SeasonalCycleChange received {}", currSeasonalCycle);
        this.seasonalCycle = nextSeasonalCycle;
        getContext().getLog().info("SeasonalCycle changed to {}", nextSeasonalCycle);
        return this;
    }

    private Behavior<EnvironmentCommand> onTemperatureChange(TemperatureChanger tc) {
        double seasonalMeanTemp = TEMP_SEASONAL_MEANS[seasonalCycle];
        double currTemp = tc.currentTemperature.get();
        double nextTemp;
        do { nextTemp = nextRandomTemperatureDelta(seasonalMeanTemp); } while (nextTemp == currTemp);
        getContext().getLog().info("TemperatureChange received {}", currTemp);
        this.temperature = nextTemp;
        getContext().getLog().info("Temperature changed to {}", nextTemp);
        return this;
    }

    // generate random temperature delta from temperature-delta to temperature+delta
    private double nextRandomTemperatureDelta(double temperature) {
        double delta = Math.abs(TEMP_SEASONAL_MEAN_DELTA);
        double randomTempChange = (delta*2) * random.nextDouble() - delta;
        double nextRandomTemp = temperature + randomTempChange;

        BigDecimal bd = BigDecimal.valueOf(nextRandomTemp);
        bd = bd.setScale(1, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    private Behavior<EnvironmentCommand> onWeatherChange(WeatherChanger wc) {
        Weather currWeather = wc.currentWeather.get();
        Weather nextWeather = currWeather == Weather.SUNNY ? Weather.RAINY : Weather.SUNNY;
        getContext().getLog().info("WeatherChange received {}", currWeather);
        this.weather = nextWeather;
        getContext().getLog().info("Weather changed to {}", nextWeather);
        return this;
    }
}
