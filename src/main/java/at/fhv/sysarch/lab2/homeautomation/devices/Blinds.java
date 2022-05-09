package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.environment.Weather;

import java.util.Optional;

//Rules:
//If the weather is sunny the blinds will close.
//If the weather is not sunny the blinds will open (unless a movie is playing).
//If a movie is playing the blinds are closed.

public class Blinds extends AbstractBehavior<Blinds.BlindsCommand> {

    public interface BlindsCommand {
    }

    public static final class BlindsMovieControl implements BlindsCommand {

        boolean movieIsPlaying;

        public BlindsMovieControl(boolean movieIsPlaying) {
            this.movieIsPlaying = movieIsPlaying;
        }
    }

    public static final class BlindsWeatherControl implements BlindsCommand {

        Weather weather;

        public BlindsWeatherControl(Weather weather) {
            this.weather = weather;
        }
    }

    private boolean blindsOpened;
    private final String groupId;
    private final String deviceId;
    private Optional<Weather> currentWeather;
    private Optional<Boolean> movieIsPlaying;


    public Blinds(ActorContext<BlindsCommand> context, String groupId, String deviceId) {
        super(context);
        this.blindsOpened = false;
        this.groupId = groupId;
        this.deviceId = deviceId;
        this.currentWeather = Optional.empty(); // warum
        this.movieIsPlaying = Optional.empty(); //warum

        getContext().getLog().info("Blinds started");
    }

    public static Behavior<BlindsCommand> create(String groupId, String deviceId) {
        return Behaviors.setup(context -> new Blinds(context, groupId, deviceId));
    }

    @Override
    public Receive<Blinds.BlindsCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(BlindsMovieControl.class, this::onControlBlindsMovie)
                .onMessage(BlindsWeatherControl.class, this::onControlBlindsWeather)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

   /* private Behavior<BlindsCommand> onControlBlinds() {
        getContext().getLog().info("Blinds reading the mediastation {}", movieIsPlaying);

        //Either a movie is playing or the weather is cloudy
        if (movieIsPlaying.isPresent() && movieIsPlaying.get() || currentWeather.get().equals(Weather.CLOUDY)) {
            blindsOpened = false;
            getContext().getLog().info("Blinds closed, because either a movie is playing or the weather is cloudy");
        }
        else {
            blindsOpened = true;
        }
        return  Behaviors.same();
    }*/

    private Behavior<BlindsCommand> onControlBlinds() {
        getContext().getLog().info("Blinds reading the weather {}", currentWeather);

        //If the weather is sunny the blinds will close
        if (currentWeather.isPresent()) {
            if (currentWeather.get().equals(Weather.SUNNY)) {
                blindsOpened = false;
                getContext().getLog().info("Blinds reading the weather is sunny, Blinds closed");
            }
            //If the weather is not sunny the blinds will open (unless a movie is playing)
            else if (currentWeather.get().equals(Weather.CLOUDY)) {
                //Movie is running: Close blinds
                if (movieIsPlaying.isPresent() && movieIsPlaying.get()) {
                    blindsOpened = false;
                    getContext().getLog().info("Blinds reading a movie is running, Blinds closed");
                }
                //No movie is running: Open blinds
                else {
                    blindsOpened = true;
                    getContext().getLog().info("Blinds reading no movie is running, Blinds opened");
                }
            }
        //If a movie is playing the blinds are closed
        } else if (movieIsPlaying.isPresent() && movieIsPlaying.get()) {
            blindsOpened = false;
            getContext().getLog().info("Blinds reading a movie is running, Blinds closed");
        }
        return Behaviors.same();
    }

    private Behavior<BlindsCommand> onControlBlindsMovie(BlindsMovieControl message) {
        this.movieIsPlaying = Optional.of(message.movieIsPlaying);
        onControlBlinds();
        return Behaviors.same();
    }

    private Behavior<BlindsCommand> onControlBlindsWeather(BlindsWeatherControl message) {
        this.currentWeather = Optional.of(message.weather);
        onControlBlinds();
        return Behaviors.same();
    }

    private Blinds onPostStop() {
        getContext().getLog().info("Blinds stopped");
        return this;
    }
}
