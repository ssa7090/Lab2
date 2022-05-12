package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.environment.Temperature;
import at.fhv.sysarch.lab2.homeautomation.environment.Weather;

import java.util.HashSet;
import java.util.Set;

//A new movie cannot be started if another movie is already playing
public class MediaStation extends AbstractBehavior<MediaStation.MediaStationCommand> {

    public interface MediaStationCommand {}

    public static final class StartStopMovie implements MediaStationCommand {
        final boolean movieIsPlaying;
        final String movieTitle;

        public StartStopMovie(boolean movieIsPlaying, String movieTitle) {
            this.movieIsPlaying = movieIsPlaying;
            this.movieTitle = movieTitle;
        }
    }

    public static Behavior<MediaStationCommand> create(ActorRef<Blinds.BlindsCommand> blinds, String groupId, String deviceId) {
        return Behaviors.setup(context -> new MediaStation(context, blinds, groupId, deviceId));
    }

    private final String groupId;
    private final String deviceId;
    private final ActorRef<Blinds.BlindsCommand> blinds;

    private String playingMovieTitle;

    public MediaStation(ActorContext<MediaStationCommand> context, ActorRef<Blinds.BlindsCommand> blinds, String groupId, String deviceId) {
        super(context);
        this.blinds = blinds;
        this.groupId = groupId;
        this.deviceId = deviceId;

        getContext().getLog().info("MediaStation started");
    }

    @Override
    public Receive<MediaStationCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(StartStopMovie.class, this::onStartStopMovie)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<MediaStation.MediaStationCommand> onStartStopMovie(StartStopMovie ssm) {
        getContext().getLog().info("MediaStation received {}", ssm.movieTitle);

        if (ssm.movieIsPlaying) {
            playingMovieTitle = ssm.movieTitle;
        } else {
            playingMovieTitle = null;
        }

        this.blinds.tell(new Blinds.UpdateMoviePlayStatus(ssm.movieIsPlaying));
        return this;
    }

    private MediaStation onPostStop() {
        getContext().getLog().info("MediaStation actor {}-{} stopped", groupId, deviceId);
        return this;
    }
}
