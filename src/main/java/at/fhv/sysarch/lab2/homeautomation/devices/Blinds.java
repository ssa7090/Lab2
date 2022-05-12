package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.environment.Weather;

public class Blinds extends AbstractBehavior<Blinds.BlindsCommand> {

    public interface BlindsCommand {
    }

    public static final class UpdateMoviePlayStatus implements BlindsCommand {
        final boolean movieIsPlaying;

        public UpdateMoviePlayStatus(boolean movieIsPlaying) {
            this.movieIsPlaying = movieIsPlaying;
        }
    }

    public static final class UpdateWeather implements BlindsCommand {
        final Weather weather;

        public UpdateWeather(Weather weather) {
            this.weather = weather;
        }
    }

    private final String groupId;
    private final String deviceId;
    private boolean blindsClosed = false;
    private boolean movieIsPlaying = false;
    private Weather currentWeather;

    public Blinds(ActorContext<BlindsCommand> context, String groupId, String deviceId) {
        super(context);
        this.groupId = groupId;
        this.deviceId = deviceId;

        getContext().getLog().info("Blinds started");
    }

    public static Behavior<BlindsCommand> create(String groupId, String deviceId) {
        return Behaviors.setup(context -> new Blinds(context, groupId, deviceId));
    }

    @Override
    public Receive<Blinds.BlindsCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(UpdateMoviePlayStatus.class, this::onUpdateMoviePlayStatus)
                .onMessage(UpdateWeather.class, this::onUpdateWeather)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<BlindsCommand> controlBlinds() {
        //Either a movie is playing or the weather is cloudy
        if (movieIsPlaying || currentWeather.equals(Weather.SUNNY)) {
            blindsClosed = true;
            getContext().getLog().info("Blinds closed");
        }
        else {
            blindsClosed = false;
            getContext().getLog().info("Blinds opened");
        }
        return this;
    }

    private Behavior<BlindsCommand> onUpdateMoviePlayStatus(UpdateMoviePlayStatus umps) {
        getContext().getLog().info("Blinds received movie play status {}", umps.movieIsPlaying);
        this.movieIsPlaying = umps.movieIsPlaying;
        controlBlinds();
        return this;
    }

    private Behavior<BlindsCommand> onUpdateWeather(UpdateWeather uw) {
        getContext().getLog().info("Blinds received weather {}", uw.weather);
        this.currentWeather = uw.weather;
        controlBlinds();
        return this;
    }

    private Blinds onPostStop() {
        getContext().getLog().info("Blinds stopped");
        return this;
    }
}
