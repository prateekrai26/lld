package elevator;

import java.util.*;

/*
Elevator System Design

Goal
Design an elevator system for a building with multiple elevators and floors.

------------------------------------------------------------
SYSTEM STRUCTURE
------------------------------------------------------------

Building
   |
   |---- ElevatorController
   |          |
   |          |---- Elevator (multiple)
   |
   |---- Floor (external buttons)

ElevatorController
    - receives external requests
    - selects best elevator
    - uses scheduling strategy

Elevator
    - maintains current floor
    - maintains direction
    - processes requests
    - has two queues
        upQueue
        downQueue


------------------------------------------------------------
REQUEST TYPES
------------------------------------------------------------

1. External Request (from floor)

User presses:
    UP button
    DOWN button

Example:
    floor 5 -> UP

Flow:
    Floor -> Controller -> Elevator


2. Internal Request (inside elevator)

User selects floor inside elevator.

Example:
    Elevator panel -> floor 10

Flow:
    ElevatorPanel -> Elevator


------------------------------------------------------------
REQUEST FLOW
------------------------------------------------------------

Step 1
User presses floor button

Step 2
Controller receives request

Step 3
Controller selects best elevator
(using scheduling strategy)

Step 4
Selected elevator adds request to queue

Step 5
Elevator processes queue

Step 6
Elevator moves to destination floor


------------------------------------------------------------
ELEVATOR QUEUE STRUCTURE
------------------------------------------------------------

Two priority queues used:

upQueue      -> Min Heap
downQueue    -> Max Heap

Example

currentFloor = 5

Requests:
8
10
3
1

Queues become:

upQueue   = [8,10]
downQueue = [3,1]

Movement order:

5 -> 8 -> 10 -> 3 -> 1

This mimics the SCAN algorithm
(similar to disk scheduling).


------------------------------------------------------------
DESIGN PATTERNS USED
------------------------------------------------------------

1. Strategy Pattern
Used for elevator scheduling

Example strategies
    Nearest elevator
    SCAN scheduling
    Zone scheduling


2. Controller Pattern
Central controller assigning elevators.


------------------------------------------------------------
TIME COMPLEXITY
------------------------------------------------------------

Selecting elevator
O(number_of_elevators)

Insert request
O(log N)

Processing request
O(log N)


------------------------------------------------------------
FUTURE EXTENSIONS
------------------------------------------------------------

VIP elevators
Freight elevators
Peak traffic optimization
Elevator capacity limits
Fault tolerance

*/

public class ElevatorSystem {

    enum Direction {
        UP,
        DOWN,
        IDLE
    }

    enum ElevatorState {
        MOVING,
        IDLE
    }

    /*
    Request object representing
    an external elevator request.
    */
    static class Request {

        int floor;
        Direction direction;

        Request(int floor, Direction direction) {
            this.floor = floor;
            this.direction = direction;
        }
    }


    /*
    Strategy Pattern

    Allows different elevator selection algorithms.
    */
    interface ElevatorSelectionStrategy {

        Elevator selectElevator(
                List<Elevator> elevators,
                Request request
        );
    }


    /*
    Example scheduling strategy

    Choose elevator with minimum distance
    from requested floor.
    */
    static class NearestElevatorStrategy
            implements ElevatorSelectionStrategy {

        public Elevator selectElevator(
                List<Elevator> elevators,
                Request request
        ) {

            Elevator best = null;
            int minDistance = Integer.MAX_VALUE;

            for (Elevator elevator : elevators) {

                int distance =
                        Math.abs(
                                elevator.currentFloor
                                        - request.floor
                        );

                if (distance < minDistance) {
                    minDistance = distance;
                    best = elevator;
                }
            }

            return best;
        }
    }


    /*
    Elevator Class

    Responsible for

    - maintaining current floor
    - processing requests
    - moving elevator
    */
    static class Elevator {

        int id;
        int currentFloor;
        Direction direction;
        ElevatorState state;

        /*
        Min heap for upward movement
        */
        PriorityQueue<Integer> upQueue =
                new PriorityQueue<>();

        /*
        Max heap for downward movement
        */
        PriorityQueue<Integer> downQueue =
                new PriorityQueue<>(
                        Collections.reverseOrder()
                );

        Elevator(int id) {

            this.id = id;
            this.currentFloor = 0;
            this.direction = Direction.IDLE;
            this.state = ElevatorState.IDLE;
        }

        /*
        Add floor request
        to appropriate queue.
        */
        void addRequest(int floor) {

            if (floor > currentFloor) {
                upQueue.add(floor);
            }
            else {
                downQueue.add(floor);
            }
        }

        /*
        Elevator movement logic
        */
        void move() {

            if (!upQueue.isEmpty()) {

                direction = Direction.UP;
                state = ElevatorState.MOVING;

                currentFloor = upQueue.poll();
            }

            else if (!downQueue.isEmpty()) {

                direction = Direction.DOWN;
                state = ElevatorState.MOVING;

                currentFloor = downQueue.poll();
            }

            else {

                direction = Direction.IDLE;
                state = ElevatorState.IDLE;
            }

            System.out.println(
                    "Elevator " + id +
                            " moved to floor " +
                            currentFloor
            );
        }
    }


    /*
    Controller

    Responsible for

    - receiving external requests
    - assigning elevators
    */
    static class ElevatorController {

        List<Elevator> elevators;

        ElevatorSelectionStrategy strategy;

        ElevatorController(int numberOfElevators) {

            elevators = new ArrayList<>();

            for (int i = 0; i < numberOfElevators; i++) {
                elevators.add(new Elevator(i));
            }

            strategy = new NearestElevatorStrategy();
        }


        /*
        External request submission
        */
        void submitExternalRequest(Request request) {

            Elevator elevator =
                    strategy.selectElevator(
                            elevators,
                            request
                    );

            elevator.addRequest(request.floor);
        }


        /*
        Simulate elevator movement
        */
        void step() {

            for (Elevator elevator : elevators) {
                elevator.move();
            }
        }
    }


    /*
    Simulation
    */
    public static void main(String[] args) {

        ElevatorController controller =
                new ElevatorController(3);

        controller.submitExternalRequest(
                new Request(7, Direction.UP)
        );

        controller.submitExternalRequest(
                new Request(2, Direction.DOWN)
        );

        controller.submitExternalRequest(
                new Request(10, Direction.UP)
        );

        controller.step();
        controller.step();
        controller.step();
    }
}