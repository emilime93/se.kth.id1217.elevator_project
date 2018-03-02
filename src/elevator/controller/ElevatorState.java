package elevator.controller;

import elevator.Elevator;
import elevator.Elevators;

import java.util.LinkedList;
import java.util.Queue;

public class ElevatorState implements Runnable {

    private int direction;
    private double floor;
    private int id;

    private LinkedList<Integer> upQueue;
    private LinkedList<Integer> downQueue;

    private ElevatorController controller;
    private boolean idle = true;

    ElevatorState(int direction, double floor, int id, ElevatorController controller) {
        this.direction = direction;
        this.floor = floor;
        this.id = id;
        this.controller = controller;

        upQueue = new LinkedList<>();
        downQueue = new LinkedList<>();
    }

    public void addDownRequest(int level) {
        if (downQueue.contains(level))
            return;
        for (int i = 0; i < downQueue.size(); i++) {
            if (level > downQueue.get(i)) {
                downQueue.add(i, level);
                return;
            }
        }
        downQueue.add(level);
    }

    public void addUpRequest(int level) {
        if (upQueue.contains(level))
            return;
        for (int i = 0; i < upQueue.size(); i++) {
            if (level < upQueue.get(i)) {
                upQueue.add(i, level);
                return;
            }
        }
        upQueue.add(level);
    }

    void addRequest(int destination, int direction) {
        if (direction == Elevators.UP)
            addUpRequest(destination);
        else
            addDownRequest(destination);
    }

    /**
     * Used for when someone inside the elevator pushes a button
     * @param destination The destination floor.
     */
    void addRequest(int destination) {
        if (this.direction == Elevators.UP) {
            if ((destination - floor) > 0) {
                // Your destination is on your way up...
                addRequest(destination, Elevators.UP);
            } else if ((destination - floor) < 0) {
                // Your destination was passed and is served on the way down
                addRequest(destination, Elevators.DOWN);
            }
        } else if (this.direction == Elevators.DOWN) {
            if ((floor - destination) > 0) {
                // Your destination if on your way down...
                addRequest(destination, Elevators.DOWN);
            } else if ((floor - destination) < 0) {
                // Your destination was passed and is served on the way up
                addRequest(destination, Elevators.UP);
            }
        }
    }

    public double calculateDistance(int targetFloor, int direction) {
        // TODO take direction into consideration
        double sum = 0.0;
        
        // If the elevator is idle, it may as well take requests
        if (this.idle)
            return 0.0;

        // If we're there...
        if (reachedFloor(targetFloor))
            return 0.0;
        // Optimization if we are already on our way there, in the right direction
        if (upQueue.contains(targetFloor) || downQueue.contains(targetFloor))
            return 0.0;

        // If we're going up
        if (this.direction == Elevators.UP) {

            if (direction == Elevators.UP) {

                // They are above us, and wants to go in the same direction
                if (targetFloor - floor > 0) {
                    return 0.0;
                } else { // They are below us and wants to go with us
                    // Calc the cost for going up
                    sum += !upQueue.isEmpty() ? upQueue.getLast() - upQueue.getFirst() : 0;

                    // Cost the cost for going down
                    sum += !downQueue.isEmpty() ? downQueue.getLast() - downQueue.getFirst() : 0;

                    // Ad the last part
//                    sum += Math.abs(downQueue.get(downQueue.size() - 1) - targetFloor);
                }

            } else { // They want to go down
                // Calc the cost for going up
                sum += !upQueue.isEmpty() ? upQueue.getLast() - upQueue.getFirst() : 0;

                // Ad the last part
//                sum += Math.abs(upQueue.get(upQueue.size() - 1) - targetFloor );
            }
        } else { // If we're going down

            if (direction == Elevators.DOWN) {
                // They are below us, and wants to go in the same direction
                if (floor - targetFloor > 0) {
                    return 0.0;
                } else {
                    // Cost the cost for going down
                    sum += !downQueue.isEmpty() ? downQueue.getLast() - downQueue.getFirst() : 0;

                    // Calc the cost for going up
                    sum += !upQueue.isEmpty() ? upQueue.getLast() - upQueue.getFirst() : 0;

                    // Ad the last part
//                    sum += Math.abs(upQueue.get(upQueue.size() - 1) - targetFloor);
                }
            } else {
                // Cost for going down
                sum += !downQueue.isEmpty() ? downQueue.getLast() - downQueue.getFirst() : 0;

                // And then going up
//                sum += Math.abs(downQueue.get(downQueue.size() - 1) - targetFloor);
            }
        }

        System.out.printf("==== DEBUG ===\nElevator #%d calculated the cost for going to %d to: %f\n", id, targetFloor, sum);

        return sum;
    }

    void stop() {
        controller.sendCommand("m " + id + " 0");
    }

    void toggleDoors() {
        controller.sendCommand("d " + id + " 1");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        controller.sendCommand("d " + id + " -1");
        try {   // For good looks
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    int getId() {
        return id;
    }

    int getDirection() {
        return direction;
    }

    void setDirection(int direction) {
        this.direction = direction;
    }

    double getFloor() {
        return floor;
    }

    void setFloor(double floor) {
        this.floor = floor;
    }

    @Override
    public String toString() {
        return String.format("Elevator #%d, at floor %f, moving %d", id, floor, direction);
    }

    public void printMyQueues() {
        System.out.println("---> ELEVATOR #"+id);
        System.out.printf("Upqueue:\n[");
        for (Integer i : upQueue) {
            System.out.printf("%d, ", i);
        }
        System.out.printf("]\nDownqueue:\n[");
        for (Integer i : downQueue) {
            System.out.printf("%d, ", i);
        }
        System.out.printf("]\n");
    }

    private boolean reachedFloor(int target) {
        // Test value, may be subject to change TODO!!!
        if (Math.abs(floor - target) < 0.04) {
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        int target = -1;
        while (true) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Flip floor direction in case of top and bottom reached.
            if (reachedFloor(Elevators.topFloor)) {
                direction = Elevators.DOWN;
            }
            if (reachedFloor(0)) {
                direction = Elevators.UP;
            }

            if (direction == Elevators.UP) {
                // Check to see if it reached it's floor
                if (reachedFloor(target)) {
                    stop();
                    toggleDoors();
                    upQueue.pollFirst();
                    target = -1;
                }

                // Check to see if it's currently moving towards its target. If not, get the next command
                if (upQueue.peek() != null) {
                    idle = false;
                    target = upQueue.peek();
                    direction = Elevators.UP;
                    controller.sendCommand("m " + id + " " + direction);
                } else if (downQueue.peek() != null) {
                    idle = false;
                    target = downQueue.peek();
                    direction = Elevators.DOWN;
                    controller.sendCommand("m " + id + " " + direction);
                } else {
                    idle = true;
                }

            } else { // If it's going down
                // Check to see if it reached it's floor
                if (reachedFloor(target)) {
                    stop();
                    toggleDoors();
                    downQueue.pollFirst();
                    target = -1;
                }

                // Check to see if it's currently moving towards its target. If not, get the next command
                if (downQueue.peek() != null) {
                    idle = false;
                    target = downQueue.peek();
                    direction = Elevators.DOWN;
                    controller.sendCommand("m " + id + " " + direction);
                }
                else if (upQueue.peek() != null) {
                    idle = false;
                    target = upQueue.peek();
                    direction = Elevators.UP;
                    controller.sendCommand("m " + id + " " + direction);
                } else {
                    idle = true;
                }
            }

        }
    }

}