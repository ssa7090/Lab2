package at.fhv.sysarch.lab2.homeautomation.ui;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.AirCondition;
import at.fhv.sysarch.lab2.homeautomation.environment.TemperatureEnvironment;
import at.fhv.sysarch.lab2.homeautomation.environment.Weather;
import at.fhv.sysarch.lab2.homeautomation.environment.WeatherEnvironment;

import java.util.Optional;
import java.util.Scanner;

public class UI extends AbstractBehavior<Void> {

    private ActorRef<TemperatureEnvironment.TemperatureEnvironmentCommand> tempEnv;
    private ActorRef<WeatherEnvironment.WeatherEnvironmentCommand> weatherEnv;
    private ActorRef<AirCondition.AirConditionCommand> airCondition;

    public static Behavior<Void> create(ActorRef<TemperatureEnvironment.TemperatureEnvironmentCommand> tempEnv, ActorRef<WeatherEnvironment.WeatherEnvironmentCommand> weatherEnv, ActorRef<AirCondition.AirConditionCommand> airCondition) {
        return Behaviors.setup(context -> new UI(context, tempEnv, weatherEnv, airCondition));
    }

    private  UI(ActorContext<Void> context, ActorRef<TemperatureEnvironment.TemperatureEnvironmentCommand> tempEnv, ActorRef<WeatherEnvironment.WeatherEnvironmentCommand> weatherEnv, ActorRef<AirCondition.AirConditionCommand> airCondition) {
        super(context);
        // TODO: implement actor and behavior as needed
        // TODO: move UI initialization to appropriate place
        this.airCondition = airCondition;
        this.tempEnv = tempEnv;
        this.weatherEnv = weatherEnv;
        new Thread(() -> { this.runCommandLine(); }).start();

        getContext().getLog().info("UI started");
    }

    @Override
    public Receive<Void> createReceive() {
        return newReceiveBuilder().onSignal(PostStop.class, signal -> onPostStop()).build();
    }

    private UI onPostStop() {
        getContext().getLog().info("UI stopped");
        return this;
    }

    public void runCommandLine() {
        // TODO: Create Actor for UI Input-Handling
        Scanner scanner = new Scanner(System.in);
        String[] input = null;
        String reader = "";


        while (!reader.equalsIgnoreCase("quit") && scanner.hasNextLine()) {
            reader = scanner.nextLine();
            // TODO: change input handling
            String[] command = reader.split(" ");
            if(command[0].equals("t")) {
                this.tempEnv.tell(new TemperatureEnvironment.TemperatureChanger(Double.valueOf(command[1])));
            }
            if(command[0].equals("w")) {
                this.weatherEnv.tell(new WeatherEnvironment.WeatherChanger(Weather.valueOf(command[1])));
            }
            if(command[0].equals("a")) {
                this.airCondition.tell(new AirCondition.PowerAirCondition(Optional.of(Boolean.valueOf(command[1]))));
            }
            // TODO: process Input
        }
        getContext().getLog().info("UI done");
    }
}
