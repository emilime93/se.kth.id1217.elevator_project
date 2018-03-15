package elevator.controller;

import elevator.Elevators;

import java.util.LinkedList;

public class ElevatorState implements Runnable {

    /* The floor variable will repserent level 4 as 400 etc, due to double vs integer rounding comparison problems */
    private int floor;
    /* 1 = UP | -1 = DOWN */
    private int direction = Elevators.UP;
    /* My ID, ranges from 1 to N */
    private int id;

    /* The up & down queues containing requests for each direction. */
    private LinkedList<Integer> upQueue;
    private LinkedList<Integer> downQueue;

    private ElevatorController controller;
    private boolean idle = true;

    /**
     * Creates a state of a elevator, containing its current floor, direction and a unique id.
     * @param floor The initial floor of the elevator. Should be 0.
     * @param id The ID of the elevator, to tell them apart.
     * @param controller The controller which the elevator sends its commands to.
     */
    ElevatorState(int floor, int id, ElevatorController controller) {
        this.floor = floor;
        this.id = id;
        this.controller = controller;

        upQueue = new LinkedList<>();
        downQueue = new LinkedList<>();
    }

    /**
     * Ads a request on the down queue in the proper order.
     * @param level The level which to go to.
     */
    private void addDownRequest(int level) {
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

    /**
     * Ads a request on the up queue in the proper order.
     * @param level The level which to go to.
     */
    private void addUpRequest(int level) {
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

    /**
     * Used for when someone outside the elevator wants to travel in a direction
     * @param destination The floor where the button was pressed
     * @param direction The direction they want to travel in
     */
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
        System.out.println("Adding request on elevator" + id + ", destination = " + destination);
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
        } else {
            System.out.println("i got elsed");
        }
    }

    boolean isIdle() {
        return idle;
    }

    int calcStopsBeforeService(int targetFloor, int requestedDirection) {
        // We are counting # of stops. Not distance.
        int sum = 0;

        // If we're there...
        if (reachedFloor(targetFloor))
            return 0;

        // Optimization if we are already on our way there, in the right direction
        if (upQueue.contains(targetFloor) || downQueue.contains(targetFloor))
            return 0;

        // If we're going up
        if (this.direction == Elevators.UP) {

            if (requestedDirection == Elevators.UP) {

                // If they are above us, and wants to go in the same direction
                if (targetFloor - floor > 0) {
                    for (Integer floor : upQueue) {
                        if (floor > targetFloor)
                            break;
                        sum++;
                    }
                    return 0;
                } else { // They are below us and wants to go with us
                    // Calc the cost for going up
                    sum += !upQueue.isEmpty() ? upQueue.size() : 0;

                    // Cost the cost for going down
                    sum += !downQueue.isEmpty() ? downQueue.size() : 0;
                }

            } else { // They want to go down
                // Calc the cost for going up
                sum += !upQueue.isEmpty() ? upQueue.size() : 0;
            }
        } else { // If we're going down

            if (requestedDirection == Elevators.DOWN) {
                // They are below us, and wants to go in the same direction
                if (floor - targetFloor > 0) {
                    for (Integer floor : downQueue) {
                        if (floor < targetFloor)
                            break;
                        sum++;
                    }
                } else {
                    // Cost the cost for going down
                    sum += !downQueue.isEmpty() ? downQueue.size() : 0;

                    // Calc the cost for going up
                    sum += !upQueue.isEmpty() ? upQueue.size() : 0;
                }
            } else {
                // Cost for going down
                sum += !downQueue.isEmpty() ? downQueue.size() : 0;
            }
        }

        System.out.printf("==== DEBUG ===\nElevator #%d calculated the cost for going to %d to: %d\n", id, targetFloor, sum);

        return sum;
    }

    void stop() {
        System.out.println(this);
        printMyQueues();
        System.out.println();
        controller.sendCommand("m " + id + " 0");
    }

    private void toggleDoors() {
        controller.sendCommand("d " + id + " 1");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e){
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

    void setFloor(int floor) {
        this.floor = floor;
    }

    @Override
    public String toString() {
        return String.format("Elevator #%d (idle:%b), at floor %d, moving %d", id, idle, floor, direction);
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
        if (target == -1) return false;
        // Test value, may be subject to change
        if (Math.abs(floor - target) <= 6)
            return true;
        return false;
    }

    @Override
    public void run() {
        // These initialize to different numbers to make if statement work and not spam the elevator with the same cmd
        int target = -1;
        int prevTarget = -2;
        while (true) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Check to see if it reached it's floor
            if (reachedFloor(target)) {
                stop();
                toggleDoors();
                upQueue.removeFirstOccurrence(target);
                downQueue.removeFirstOccurrence(target);
            }

            // If we have no target, we want a new one.
            target = getNextDestination();
            if (target != prevTarget && target != -1) {
                System.out.printf("Elevator #%d got destination %d.\n", id, target);
                idle = false;
                goTowardsTarget(target);
                prevTarget = target;
            } else if (target == -1) {
                idle = true;
            }
        }
    }

    private int getNextDestination() {
        /**
         * Algorithm explanation:
         * - 1st consideration:
         * Is there a request for the same direction that we are traveling in,
         * which is ahead of us as well?
         * - 2nd consideration:
         * Is there a request in the other direction at all?
         * - 3rd consideration:
         * Is there a request in the "original (1st)" direction at all, (but that wasn't ahead of us)?
         */
        if (direction == Elevators.UP) {
            for (Integer integer : upQueue) {
                if (integer > floor) {
                    return integer;
                }
            }
            if (downQueue.peek() != null) {
                return downQueue.peek();
            } else if(upQueue.peek() != null) {
                return upQueue.peek();
            }
        } else {
            // We're going down
            for (Integer integer : downQueue) {
                if (integer < floor) {
                    return integer;
                }
            }
            if (upQueue.peek() != null) {
                return upQueue.peek();
            } else if(downQueue.peek() != null) {
                return downQueue.peek();
            }
        }
        // We have no target, pls give
        return -1;
    }

    private void goTowardsTarget(int target) {
        if (floor - target > 0) {
            direction = Elevators.DOWN;
            controller.sendCommand("m " + id + " -1");
        } else {
            direction = Elevators.UP;
            controller.sendCommand("m " + id + " 1");
        }
    }

}