package at.fhv.sysarch.lab2.homeautomation.ui;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.AirCondition;
import at.fhv.sysarch.lab2.homeautomation.devices.fridge.Fridge;
import at.fhv.sysarch.lab2.homeautomation.devices.MediaStation;
import at.fhv.sysarch.lab2.homeautomation.devices.fridge.Receipt;
import at.fhv.sysarch.lab2.homeautomation.environment.TemperatureEnvironment;
import at.fhv.sysarch.lab2.homeautomation.environment.Weather;
import at.fhv.sysarch.lab2.homeautomation.environment.WeatherEnvironment;

import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class UI extends AbstractBehavior<UI.UICommand> {

    public interface UICommand {}

    public static final class WrappedFridgeResponse implements UI.UICommand {
        final Fridge.FridgeResponse fridgeResponse;

        public WrappedFridgeResponse(Fridge.FridgeResponse fridgeResponse) {
            this.fridgeResponse = fridgeResponse;
        }
    }

    private ActorRef<TemperatureEnvironment.TemperatureEnvironmentCommand> tempEnv;
    private ActorRef<WeatherEnvironment.WeatherEnvironmentCommand> weatherEnv;
    private ActorRef<AirCondition.AirConditionCommand> airCondition;
    private ActorRef<MediaStation.MediaStationCommand> mediaStation;
    private ActorRef<Fridge.FridgeCommand> fridge;

    public static Behavior<UICommand> create(ActorRef<TemperatureEnvironment.TemperatureEnvironmentCommand> tempEnv, ActorRef<WeatherEnvironment.WeatherEnvironmentCommand> weatherEnv, ActorRef<AirCondition.AirConditionCommand> airCondition, ActorRef<MediaStation.MediaStationCommand> mediaStation, ActorRef<Fridge.FridgeCommand> fridge) {
        return Behaviors.setup(context -> new UI(context, tempEnv, weatherEnv, airCondition, mediaStation, fridge));
    }

    private  UI(ActorContext<UICommand> context, ActorRef<TemperatureEnvironment.TemperatureEnvironmentCommand> tempEnv, ActorRef<WeatherEnvironment.WeatherEnvironmentCommand> weatherEnv, ActorRef<AirCondition.AirConditionCommand> airCondition, ActorRef<MediaStation.MediaStationCommand> mediaStation, ActorRef<Fridge.FridgeCommand> fridge) {
        super(context);
        this.airCondition = airCondition;
        this.tempEnv = tempEnv;
        this.weatherEnv = weatherEnv;
        this.mediaStation = mediaStation;
        this.fridge = fridge;
        new Thread(() -> { this.runCommandLine(); }).start();

        getContext().getLog().info("UI started");
    }

    @Override
    public Receive<UICommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(WrappedFridgeResponse.class, this::onWrappedFridgeResponse)
                .onSignal(PostStop.class, signal -> onPostStop()).build();
    }

    private Behavior<UICommand> onWrappedFridgeResponse(WrappedFridgeResponse wrapped) {
        Fridge.FridgeResponse response = wrapped.fridgeResponse;
        if (response instanceof Fridge.OrderResponse) {
            Fridge.OrderResponse orderResponse = (Fridge.OrderResponse) response;
            Optional<Receipt> receiptOpt = orderResponse.receiptOpt;

            if (receiptOpt.isPresent()) {
                getContext().getLog().info("Order processed: " + receiptOpt.get());
            } else {
                getContext().getLog().error(orderResponse.errorMessage);
            }
        } else if (response instanceof Fridge.StoredProductsResponse) {
            getContext().getLog().info("Listing stored products:");
            for (var product : ((Fridge.StoredProductsResponse) response).storedProducts) {
                getContext().getLog().info(String.valueOf(product));
            }
        } else if (response instanceof Fridge.OrderHistoryResponse) {
            getContext().getLog().info("Listing order history:");
            for (var order : ((Fridge.OrderHistoryResponse) response).orderHistory) {
                getContext().getLog().info(String.valueOf(order));
            }
        } else {
            return Behaviors.unhandled();
        }
        return this;
    }

    private UI onPostStop() {
        getContext().getLog().info("UI stopped");
        return this;
    }

    public void runCommandLine() {
        Scanner scanner = new Scanner(System.in);
        String[] input = null;
        String reader = "";


        while (!reader.equalsIgnoreCase("quit") && scanner.hasNextLine()) {
            reader = scanner.nextLine();
            String[] command = reader.split(" ");
            // environments
            if(command[0].equals("t")) {
                this.tempEnv.tell(new TemperatureEnvironment.TemperatureManualChange(Double.valueOf(command[1])));
            }
            if(command[0].equals("w")) {
                this.weatherEnv.tell(new WeatherEnvironment.WeatherManualChange(Weather.valueOf(command[1])));
            }
            // air condition
            if(command[0].equals("a")) {
                this.airCondition.tell(new AirCondition.PowerAirCondition(Boolean.valueOf(command[1])));
            }
            // media station
            if(command[0].equals("p")) {
                this.mediaStation.tell(new MediaStation.StartStopMovie(true, command[1]));
            }
            if(command[0].equals("s")) {
                this.mediaStation.tell(new MediaStation.StartStopMovie(false, null));
            }
            if(command[0].equals("o")) {
                this.fridge.tell(new Fridge.OrderRequest(getContext().getSelf(), command[1], Integer.valueOf(command[2])));
            }
            if(command[0].equals("qp")) {
                this.fridge.tell(new Fridge.StoredProductsRequest(getContext().getSelf()));
            }
            if(command[0].equals("qo")) {
                this.fridge.tell(new Fridge.OrderHistoryRequest(getContext().getSelf()));
            }
            if(command[0].equals("c")) {
                this.fridge.tell(new Fridge.ConsumeRequest(getContext().getSystem().ignoreRef(), command[1]));
            }
        }
        getContext().getLog().info("UI done");
    }
}
