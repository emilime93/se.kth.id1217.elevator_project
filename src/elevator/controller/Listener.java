package elevator.controller;

import elevator.Elevators;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;

/**
 * Title:        Elevator Controller Listener
 * Description:  The object that continuously listens for data from
 *               the TCP socket on port 4711 (default) and forwards it.
 * Company:      KTH
 * Course:       ID1217 Concurrent Programming.
 *
 * @authors Emil Lindholm Brandt & Sabina Hauzenberger
 * @version 1.0
 */

class Listener implements Runnable {

    private ElevatorController controller;

    Listener(ElevatorController controller) {
        this.controller = controller;
    }


    /**
     * The listener threads main loop, listening for any data from the TCP socket
     * and forwards any data to the correct controller method. This does not terminate.
     */
    @Override
    public void run() {
        Socket socket = controller.getSocket();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String str;
            while ((str = reader.readLine()) != null) {
                String[] command = str.split(" ");
                int elevatorID;
                switch (command[0].charAt(0)) {
                    // Elevator position update
                    case 'f':
                        double position;
                        elevatorID = Integer.parseInt(command[1]);
                        position = Double.parseDouble(command[2]);
                        int correctedPos = (int) (position * 100);
                        controller.updateElevatorPos(elevatorID, correctedPos);
                        break;
                    // When a floor button is pressed
                    case 'b':
                        int floorPressed, direction;
                        floorPressed = Integer.parseInt(command[1]) * 100;
                        direction = Integer.parseInt(command[2]);
                        controller.moveToFloor(floorPressed, direction);
                        break;
                    case 'v':
                        System.out.println("Velocity changed.");
                        break;
                    // When a panel button inside the elevator is pressed
                    case 'p':
                        int destination;
                        elevatorID = Integer.parseInt(command[1]);
                        destination = Integer.parseInt(command[2]);
                        if (destination == Elevators.SPECIAL_FOR_STOP)
                            controller.stop(elevatorID);
                        else
                            controller.moveElevator(elevatorID, destination*100);
                        break;
                    default:
                        System.out.println("default switch clause, something is wrong:\n" + Arrays.toString(command));
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}