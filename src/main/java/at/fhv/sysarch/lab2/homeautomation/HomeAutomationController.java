package at.fhv.sysarch.lab2.homeautomation;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.*;
import at.fhv.sysarch.lab2.homeautomation.environment.TemperatureEnvironment;
import at.fhv.sysarch.lab2.homeautomation.environment.WeatherEnvironment;
import at.fhv.sysarch.lab2.homeautomation.ui.UI;

public class HomeAutomationController extends AbstractBehavior<Void>{

    private ActorRef<TemperatureEnvironment.TemperatureEnvironmentCommand> tempEnv;
    private ActorRef<WeatherEnvironment.WeatherEnvironmentCommand> weatherEnv;
    private ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor;
    private ActorRef<WeatherSensor.WeatherCommand> weatherSensor;
    private ActorRef<AirCondition.AirConditionCommand> airCondition;
    private ActorRef<Blinds.BlindsCommand> blinds;
    private ActorRef<MediaStation.MediaStationCommand> mediaStation;

    public static Behavior<Void> create() {
        return Behaviors.setup(HomeAutomationController::new);
    }

    private  HomeAutomationController(ActorContext<Void> context) {
        super(context);
        // TODO: consider guardians and hierarchies. Who should create and communicate with which Actors?
        this.airCondition = getContext().spawn(AirCondition.create("1", "1"), "AirCondition");
        this.blinds = getContext().spawn(Blinds.create("2", "1"), "Blinds");
        this.mediaStation = getContext().spawn(MediaStation.create(this.blinds, "3", "1"), "MediaStation");
        this.temperatureSensor = getContext().spawn(TemperatureSensor.create(this.airCondition,"4", "1"), "TemperatureSensor");
        this.weatherSensor = getContext().spawn(WeatherSensor.create(this.blinds,"5", "1"), "WeatherSensor");
        this.tempEnv = getContext().spawn(TemperatureEnvironment.create(this.temperatureSensor), "TemperatureEnvironment");
        this.weatherEnv = getContext().spawn(WeatherEnvironment.create(this.weatherSensor), "WeatherEnvironment");

        ActorRef<Void> ui = getContext().spawn(UI.create(this.tempEnv, this.weatherEnv, this.airCondition, this.mediaStation), "UI");
        getContext().getLog().info("HomeAutomation Application started");
    }

    @Override
    public Receive<Void> createReceive() {
        return newReceiveBuilder().onSignal(PostStop.class, signal -> onPostStop()).build();
    }

    private HomeAutomationController onPostStop() {
        getContext().getLog().info("HomeAutomation Application stopped");
        return this;
    }
}
