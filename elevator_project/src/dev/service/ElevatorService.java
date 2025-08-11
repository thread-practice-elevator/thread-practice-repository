package dev.service;

import dev.model.Direction;
import dev.model.Elevator;
import dev.model.Passenger;
import dev.controller.logger.LoggerFactory;
import java.util.*;
import java.util.concurrent.*;

public class ElevatorService {
    private final Elevator elevator;
    private final PassengerService passengerService;
    private final LoggerFactory loggerFactory;
    private volatile boolean running;
    private int totalSteps;

    // 스레드 상태 변화를 GUI에 알리기 위한 리스너 인터페이스
    public interface ThreadStatusListener {
        void onStatusChange(String threadName, String status);
    }
    private ThreadStatusListener threadStatusListener;

    // 엘리베이터 상태 변화를 GUI에 알리기 위한 리스너 인터페이스 추가
    public interface ElevatorStateListener {
        void onElevatorStateUpdated();
    }
    private ElevatorStateListener elevatorStateListener;

    // 여러 스레드가 안전하게 공유하는 요청 큐
    private final BlockingQueue<Integer> passengerRequests = new LinkedBlockingQueue<>();
    
    // 3개의 고정 스레드 풀 (요청, 이동, 상태 감시)
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);
    
    public ElevatorService(int minFloor, int maxFloor, int capacity, LoggerFactory loggerFactory) {
        this.elevator = new Elevator(minFloor, maxFloor, capacity);
        this.loggerFactory = loggerFactory;
        this.passengerService = new PassengerService(loggerFactory);
        this.running = false;
        this.totalSteps = 0;
    }

    public synchronized LoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    // 스레드 상태 리스너를 설정하는 메서드
    public void setThreadStatusListener(ThreadStatusListener listener) {
        this.threadStatusListener = listener;
    }

    // 엘리베이터 상태 리스너를 설정하는 메서드 추가
    public void setElevatorStateListener(ElevatorStateListener listener) {
        this.elevatorStateListener = listener;
    }

    public void startSimulation() {
        if (running) return;
        running = true;
        loggerFactory.log("=== SCAN 엘레베이터 알고리즘 시작 ===");
        loggerFactory.log("초기 상태: " + elevator);

        // ExecutorService에 각 역할을 담당하는 스레드 작업 제출
        executorService.submit(this::requestProcessorThread);
        executorService.submit(this::movementControlThread);
        executorService.submit(this::statusMonitorThread);
    }
    
    public synchronized void stopSimulation() {
        if (!running) return;
        running = false;
        
        // 스레드 상태를 '종료'로 업데이트
        if (threadStatusListener != null) {
            threadStatusListener.onStatusChange("요청 처리", "종료");
            threadStatusListener.onStatusChange("이동 제어", "종료");
            threadStatusListener.onStatusChange("상태 감시", "종료");
        }

        executorService.shutdownNow(); // 모든 스레드 즉시 종료
        try {
            if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                loggerFactory.log("경고: 스레드가 1초 내에 종료되지 않았습니다.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        loggerFactory.log("=== 알고리즘 완료 ===");
        printStatistics();
    }

    /**
     * 외부에서 들어오는 승객 요청을 받아서 큐에 추가
     */
    public synchronized void addPassengerRequest(int startFloor, int destinationFloor) {
        if (startFloor == destinationFloor) {
            loggerFactory.log("경고: 출발층과 도착층이 같습니다. (" + startFloor + ")");
            return;
        }
        if (startFloor < elevator.getMinFloor() || startFloor > elevator.getMaxFloor() ||
            destinationFloor < elevator.getMinFloor() || destinationFloor > elevator.getMaxFloor()) {
            loggerFactory.log("경고: 잘못된 층 번호입니다. (범위: " + elevator.getMinFloor() + "~" + elevator.getMaxFloor() + ")");
            return;
        }
        
        passengerService.addPassengerRequest(startFloor, destinationFloor);
        passengerRequests.add(startFloor);
        loggerFactory.log("승객 요청 추가: " + startFloor + "층 -> " + destinationFloor + "층");
        if (elevatorStateListener != null) { // 승객 요청 시 상태 업데이트 알림
            elevatorStateListener.onElevatorStateUpdated();
        }
    }
    
    /**
     * 스레드1: 요청 처리 스레드
     * BlockingQueue에서 승객 요청을 꺼내 엘리베이터에 전달
     */
    private void requestProcessorThread() {
        if (threadStatusListener != null) threadStatusListener.onStatusChange("요청 처리", "실행 중");
        loggerFactory.log("요청 처리 스레드 시작.");
        while (running) {
            try {
                Integer floor = passengerRequests.poll(100, TimeUnit.MILLISECONDS);
                if (floor != null) {
                    synchronized (this) {
                        elevator.addRequest(floor);
                    }
                    loggerFactory.log("요청 처리됨: " + floor + "층");
                    if (elevatorStateListener != null) { // 엘리베이터 요청 추가 시 상태 업데이트 알림
                        elevatorStateListener.onElevatorStateUpdated();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break; // 인터럽트 발생 시 루프 종료
            }
        }
        if (threadStatusListener != null) threadStatusListener.onStatusChange("요청 처리", "종료");
        loggerFactory.log("요청 처리 스레드 종료.");
    }
    
    /**
     * 스레드2: 이동 제어 스레드
     * 엘리베이터의 움직임을 제어하는 SCAN 알고리즘 실행
     */
    private void movementControlThread() {
        if (threadStatusListener != null) threadStatusListener.onStatusChange("이동 제어", "실행 중");
        loggerFactory.log("이동 제어 스레드 시작.");
        while (running) {
            try {
                synchronized (this) {
                    passengerService.getWaitingPassengerFloors().forEach(elevator::addRequest);
                    Integer nextFloor = getNextFloorSCAN();
                    if (nextFloor != null) {
                        moveOneFloor(nextFloor);
                    } else {
                        handleDirectionChange();
                    }
                }
                if (elevatorStateListener != null) { // 엘리베이터 이동 시 상태 업데이트 알림
                    elevatorStateListener.onElevatorStateUpdated();
                }
                
                Thread.sleep(500); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break; // 인터럽트 발생 시 루프 종료
            }
        }
        if (threadStatusListener != null) threadStatusListener.onStatusChange("이동 제어", "종료");
        loggerFactory.log("이동 제어 스레드 종료.");
    }

    /**
     * 스레드3: 상태 감시 스레드
     * 주기적으로 엘리베이터 상태를 확인하고, 승객 탑승/하차 및 로그 출력
     */
    private void statusMonitorThread() {
        if (threadStatusListener != null) threadStatusListener.onStatusChange("상태 감시", "실행 중");
        loggerFactory.log("상태 감시 스레드 시작.");
        while(running) {
            try {
                synchronized (this) {
                    loggerFactory.log("\n--- Step " + totalSteps++ + " ---");
                    processPassengerExit(); 
                    processPassengerBoarding(); 
                    loggerFactory.log("현재 상태: " + elevator);
                    
                    if (!elevator.hasRequests() && !passengerService.hasWaitingPassengers()) {
                         running = false; 
                    }
                }
                if (elevatorStateListener != null) { // 상태 감시 후 상태 업데이트 알림
                    elevatorStateListener.onElevatorStateUpdated();
                }
                
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break; 
            }
        }
        if (threadStatusListener != null) threadStatusListener.onStatusChange("상태 감시", "종료");
        loggerFactory.log("상태 감시 스레드 종료.");
    }
    
    private synchronized void processPassengerExit() {
        if (elevator.hasRequestAt(elevator.getCurrentFloor())) {
            List<Passenger> exitingPassengers = passengerService.getExitingPassengers(
                elevator.getPassengers(), elevator.getCurrentFloor()
            );
            
            if (!exitingPassengers.isEmpty()) {
                loggerFactory.log(elevator.getCurrentFloor() + "층 도착 - 하차하는 승객:");
                for (Passenger passenger : exitingPassengers) {
                    passengerService.processPassengerExit(passenger);
                }
                
                elevator.removePassengersAt(elevator.getCurrentFloor());
                elevator.removeRequest(elevator.getCurrentFloor());
            }
        }
    }
    
    private synchronized void processPassengerBoarding() {
        List<Passenger> boardingPassengers = passengerService.getBoardingPassengers(
            elevator.getCurrentFloor(), 
            elevator.getDirection(), 
            elevator.getCapacity()
        );
        
        if (!boardingPassengers.isEmpty()) {
            loggerFactory.log(elevator.getCurrentFloor() + "층에서 탑승하는 승객:");
            for (Passenger passenger : boardingPassengers) {
                passengerService.processPassengerBoarding(passenger);
                elevator.addPassenger(passenger);
            }
        }
        elevator.removeRequest(elevator.getCurrentFloor());
    }

    public synchronized void printStatistics() {
        if (running) return; 
        loggerFactory.log("\n=== 실행 통계 ===");
        loggerFactory.log("총 실행 단계: " + totalSteps);
        loggerFactory.log("완료된 승객 수: " + passengerService.getCompletedPassengers().size());
        loggerFactory.log("대기 중인 승객 수: " + passengerService.getWaitingPassengers().size());
        
        List<Passenger> completed = passengerService.getCompletedPassengers();
        if (!completed.isEmpty()) {
            double avgWaitTime = completed.stream()
                .mapToLong(Passenger::getWaitingTime)
                .average()
                .orElse(0.0);
            double avgTotalTime = completed.stream()
                .mapToLong(Passenger::getTotalTime)
                .average()
                .orElse(0.0);
            loggerFactory.log("평균 대기시간: " + String.format("%.1f", avgWaitTime) + "초");
            loggerFactory.log("평균 총 소요시간: " + String.format("%.1f", avgTotalTime) + "초");
        }
        loggerFactory.log("최종 엘레베이터 위치: " + elevator.getCurrentFloor() + "층");
    }

    public synchronized Elevator getElevator() { return elevator; }
    public synchronized List<Passenger> getCompletedPassengers() { return passengerService.getCompletedPassengers(); }
    public synchronized Queue<Passenger> getWaitingPassengers() { return passengerService.getWaitingPassengers(); }
    public synchronized boolean isRunning() { return running; }

    private Integer getNextFloorSCAN() {
        int currentFloor = elevator.getCurrentFloor();
        Direction currentDirection = elevator.getDirection();
        
        if (currentDirection == Direction.IDLE) {
            return setInitialDirection();
        }
        
        if (currentDirection == Direction.UP) {
            return elevator.getRequests().stream()
                .filter(floor -> floor > currentFloor)
                .min(Integer::compareTo)
                .orElse(null);
        } else { 
            return elevator.getRequests().stream()
                .filter(floor -> floor < currentFloor)
                .max(Integer::compareTo)
                .orElse(null);
        }
    }

    private Integer setInitialDirection() {
        if (elevator.getRequests().isEmpty()) {
            return null;
        }
        
        int currentFloor = elevator.getCurrentFloor();
        int closestFloor = elevator.getRequests().stream()
            .min(Comparator.comparing(floor -> Math.abs(floor - currentFloor)))
            .orElse(currentFloor);
        
        if (closestFloor > currentFloor) {
            elevator.setDirection(Direction.UP);
        } else if (closestFloor < currentFloor) {
            elevator.setDirection(Direction.DOWN);
        }
        return closestFloor;
    }
    
    private void moveOneFloor(int targetFloor) {
        int currentFloor = elevator.getCurrentFloor();
        if (currentFloor == targetFloor) return;
        
        if (targetFloor > currentFloor) {
            elevator.setCurrentFloor(currentFloor + 1);
            elevator.setDirection(Direction.UP);
            loggerFactory.log((currentFloor + 1) + "층으로 이동 (UP)");
        } else {
            elevator.setCurrentFloor(currentFloor - 1);
            elevator.setDirection(Direction.DOWN);
            loggerFactory.log((currentFloor - 1) + "층으로 이동 (DOWN)");
        }
    }
    
    private void handleDirectionChange() {
        int currentFloor = elevator.getCurrentFloor();
        Direction currentDirection = elevator.getDirection();
        
        boolean hasRequestsInOppositeDirection = (currentDirection == Direction.UP) ? 
            elevator.getRequests().stream().anyMatch(floor -> floor < currentFloor) :
            elevator.getRequests().stream().anyMatch(floor -> floor > currentFloor);
        
        if (hasRequestsInOppositeDirection) {
            Direction newDirection = (currentDirection == Direction.UP) ? Direction.DOWN : Direction.UP;
            elevator.setDirection(newDirection);
            loggerFactory.log("방향 전환: " + currentDirection + " → " + newDirection);
        } else {
            elevator.setDirection(Direction.IDLE);
            loggerFactory.log("모든 요청 처리 완료 - 정지");
        }
    }
}