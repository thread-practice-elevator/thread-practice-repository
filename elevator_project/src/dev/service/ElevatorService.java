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
        if (running) {
            loggerFactory.warn("시뮬레이션이 이미 실행 중입니다."); // WARN: 중복 실행 시도
            return;
        }
        running = true;
        loggerFactory.info("=== SCAN 엘레베이터 알고리즘 시작 ==="); // INFO: 시뮬레이션 시작
        loggerFactory.info("초기 상태: " + elevator); // INFO: 초기 상태 로그

        // ExecutorService에 각 역할을 담당하는 스레드 작업 제출
        executorService.submit(this::requestProcessorThread);
        executorService.submit(this::movementControlThread);
        executorService.submit(this::statusMonitorThread);
    }
    
    public synchronized void stopSimulation() {
        if (!running) {
            loggerFactory.warn("시뮬레이션이 이미 종료되었습니다."); // WARN: 중복 종료 시도
            return;
        }
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
                loggerFactory.error("경고: 스레드가 1초 내에 종료되지 않았습니다. 강제 종료를 시도합니다."); // ERROR: 스레드 종료 실패
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            loggerFactory.error("스레드 종료 대기 중 인터럽트 발생: " + e.getMessage()); // ERROR: 인터럽트 발생
        }
        loggerFactory.info("=== 시뮬레이션 완료 ==="); // INFO: 시뮬레이션 완료
        printStatistics();
    }

    /**
     * 외부에서 들어오는 승객 요청을 받아서 큐에 추가
     */
    public synchronized void addPassengerRequest(int startFloor, int destinationFloor) {
        if (startFloor == destinationFloor) {
            loggerFactory.warn("경고: 출발층과 도착층이 같습니다. 요청이 무시됩니다. (" + startFloor + ")"); // WARN: 잘못된 요청
            return;
        }
        if (startFloor < elevator.getMinFloor() || startFloor > elevator.getMaxFloor() ||
            destinationFloor < elevator.getMinFloor() || destinationFloor > elevator.getMaxFloor()) {
            loggerFactory.warn("경고: 잘못된 층 번호입니다. 요청이 무시됩니다. (범위: " + elevator.getMinFloor() + "~" + elevator.getMaxFloor() + ")"); // WARN: 잘못된 층 번호
            return;
        }
        
        Passenger newPassenger = new Passenger(startFloor, destinationFloor);
        passengerService.addPassengerRequest(newPassenger);
        
        // 요청 큐에 출발층을 추가
        passengerRequests.add(startFloor);
        loggerFactory.info("승객 요청 추가: " + startFloor + "층 -> " + destinationFloor + "층"); // INFO: 승객 요청 추가
        
        if (elevatorStateListener != null) { 
            elevatorStateListener.onElevatorStateUpdated();
        }
    }
    
    /**
     * 스레드1: 요청 처리 스레드
     * BlockingQueue에서 승객 요청을 꺼내 엘리베이터에 전달
     */
    private void requestProcessorThread() {
        if (threadStatusListener != null) threadStatusListener.onStatusChange("요청 처리", "실행 중");
        loggerFactory.debug("요청 처리 스레드 시작."); // DEBUG: 스레드 시작 상세 로그
        while (running) {
            try {
                Integer floor = passengerRequests.poll(100, TimeUnit.MILLISECONDS);
                if (floor != null) {
                    synchronized (this) {
                        elevator.addRequest(floor);
                    }
                    loggerFactory.debug("요청 처리됨: " + floor + "층"); // DEBUG: 요청 처리 상세 로그
                    if (elevatorStateListener != null) {
                        elevatorStateListener.onElevatorStateUpdated();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                loggerFactory.warn("요청 처리 스레드 인터럽트 발생, 종료합니다."); // WARN: 스레드 인터럽트
                break;
            }
        }
        if (threadStatusListener != null) threadStatusListener.onStatusChange("요청 처리", "종료");
        loggerFactory.debug("요청 처리 스레드 종료."); // DEBUG: 스레드 종료 상세 로그
    }
    
    /**
     * 스레드2: 이동 제어 스레드
     * 엘리베이터의 움직임을 제어하는 SCAN 알고리즘 실행
     */
    private void movementControlThread() {
        if (threadStatusListener != null) threadStatusListener.onStatusChange("이동 제어", "실행 중");
        loggerFactory.debug("이동 제어 스레드 시작."); // DEBUG: 스레드 시작 상세 로그
        while (running) {
            try {
                synchronized (this) {
                    passengerService.getWaitingPassengerFloors().forEach(elevator::addRequest);
                    Integer nextFloor = getNextFloorSCAN();
                    if (nextFloor != null) {
                        moveOneFloor(nextFloor);
                    } else {
                        // 요청이 없을 경우에만 방향 전환 로직 실행
                        if (elevator.hasRequests() || passengerService.hasWaitingPassengers()) {
                           handleDirectionChange();
                        }
                    }
                }
                if (elevatorStateListener != null) {
                    elevatorStateListener.onElevatorStateUpdated();
                }
                
                Thread.sleep(500);  // 0.5초 대기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                loggerFactory.warn("이동 제어 스레드 인터럽트 발생, 종료합니다."); // WARN: 스레드 인터럽트
                break; 
            }
        }
        if (threadStatusListener != null) threadStatusListener.onStatusChange("이동 제어", "종료");
        loggerFactory.debug("이동 제어 스레드 종료."); // DEBUG: 스레드 종료 상세 로그
    }

    /**
     * 스레드3: 상태 감시 스레드
     * 주기적으로 엘리베이터 상태를 확인하고, 승객 탑승/하차 및 로그 출력
     */
    private void statusMonitorThread() {
        if (threadStatusListener != null) threadStatusListener.onStatusChange("상태 감시", "실행 중");
        loggerFactory.debug("상태 감시 스레드 시작."); // DEBUG: 스레드 시작 상세 로그
        while(running) {
            try {
                synchronized (this) {
                    loggerFactory.info("\n--- Step " + totalSteps++ + " ---"); // INFO: 시뮬레이션 단계 기록
                    processPassengerExit(); 
                    processPassengerBoarding(); 
                    loggerFactory.info("현재 상태: " + elevator); // INFO: 엘리베이터 현재 상태 기록
                    
                    if (!elevator.hasRequests() && !passengerService.hasWaitingPassengers() && passengerRequests.isEmpty()) {
                        loggerFactory.info("모든 요청 및 승객 처리가 완료되었습니다. 시뮬레이션을 종료합니다."); // INFO: 시뮬레이션 자동 종료
                        running = false; 
                    }
                }
                if (elevatorStateListener != null) {
                    elevatorStateListener.onElevatorStateUpdated();
                }
                
                Thread.sleep(500); // 0.5초 대기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                loggerFactory.warn("상태 감시 스레드 인터럽트 발생, 종료합니다."); // WARN: 스레드 인터럽트
                break; 
            }
        }
        if (threadStatusListener != null) threadStatusListener.onStatusChange("상태 감시", "종료");
        loggerFactory.debug("상태 감시 스레드 종료."); // DEBUG: 스레드 종료 상세 로그
    }
    
    private synchronized void processPassengerExit() {
        if (elevator.hasRequestAt(elevator.getCurrentFloor())) {
            List<Passenger> exitingPassengers = passengerService.getExitingPassengers(
                elevator.getPassengers(), elevator.getCurrentFloor()
            );
            
            if (!exitingPassengers.isEmpty()) {
                loggerFactory.info(elevator.getCurrentFloor() + "층 도착 - 하차하는 승객:"); // INFO: 하차 이벤트
                for (Passenger passenger : exitingPassengers) {
                    passengerService.processPassengerExit(passenger);
                    loggerFactory.info("  - " + passenger.toString() + " 하차."); // INFO: 개별 승객 하차
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
            loggerFactory.info(elevator.getCurrentFloor() + "층에서 탑승하는 승객:"); // INFO: 탑승 이벤트
            for (Passenger passenger : boardingPassengers) {
                passengerService.processPassengerBoarding(passenger);
                elevator.addPassenger(passenger);
                loggerFactory.info("  - " + passenger.toString() + " 탑승."); // INFO: 개별 승객 탑승
            }
        }
        // 탑승 요청 처리 후 해당 층 요청 제거
        if (!boardingPassengers.isEmpty()) {
            elevator.removeRequest(elevator.getCurrentFloor());
        }
    }

    public synchronized void printStatistics() {
        if (running) return; 
        loggerFactory.info("\n=== 실행 통계 ==="); // INFO: 통계 정보 시작
        loggerFactory.info("총 실행 단계: " + totalSteps);
        loggerFactory.info("완료된 승객 수: " + passengerService.getCompletedPassengers().size());
        loggerFactory.info("대기 중인 승객 수: " + passengerService.getWaitingPassengers().size());
        
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
            loggerFactory.info("평균 대기시간: " + String.format("%.1f", avgWaitTime) + "초");
            loggerFactory.info("평균 총 소요시간: " + String.format("%.1f", avgTotalTime) + "초");
        }
        loggerFactory.info("최종 엘레베이터 위치: " + elevator.getCurrentFloor() + "층");
        loggerFactory.info("=== 통계 출력 완료 ==="); // INFO: 통계 정보 종료
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
            loggerFactory.debug("초기 방향 설정: IDLE -> UP"); // DEBUG: 방향 설정 상세
        } else if (closestFloor < currentFloor) {
            elevator.setDirection(Direction.DOWN);
            loggerFactory.debug("초기 방향 설정: IDLE -> DOWN"); // DEBUG: 방향 설정 상세
        } else {
            // 같은 층에 요청이 있을 경우
            elevator.setDirection(Direction.IDLE);
            loggerFactory.debug("초기 방향 설정: IDLE -> IDLE (요청 층과 현재 층 동일)"); // DEBUG: 방향 설정 상세
        }
        return closestFloor;
    }
    
    private void moveOneFloor(int targetFloor) {
        int currentFloor = elevator.getCurrentFloor();
        if (currentFloor == targetFloor) return;
        
        if (targetFloor > currentFloor) {
            elevator.setCurrentFloor(currentFloor + 1);
            elevator.setDirection(Direction.UP);
            loggerFactory.debug((currentFloor + 1) + "층으로 이동 (UP)"); // DEBUG: 이동 상세
        } else {
            elevator.setCurrentFloor(currentFloor - 1);
            elevator.setDirection(Direction.DOWN);
            loggerFactory.debug((currentFloor - 1) + "층으로 이동 (DOWN)"); // DEBUG: 이동 상세
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
            loggerFactory.info("방향 전환: " + currentDirection + " → " + newDirection); // INFO: 방향 전환 주요 이벤트
        } else {
            // 이 로직은 불필요한 로그를 방지하기 위해 이동 제어 스레드에서 처리
        }
    }
}