package dev.controller;

import dev.service.ElevatorService;
import dev.model.Elevator;
import dev.model.Passenger;
import dev.controller.logger.LoggerFactory;
import dev.service.ElevatorService.ThreadStatusListener; 
import dev.service.ElevatorService.ElevatorStateListener; 
import java.util.List;
import java.util.Queue;

public class ElevatorController {
    private final ElevatorService elevatorService;

    public ElevatorController(int minFloor, int maxFloor, int capacity) {
        LoggerFactory loggerFactory = new LoggerFactory();
        this.elevatorService = new ElevatorService(minFloor, maxFloor, capacity, loggerFactory);
    }
    
    public void addPassengerRequest(int startFloor, int destinationFloor) {
        elevatorService.addPassengerRequest(startFloor, destinationFloor);
    }
    
    // 시뮬레이션 시작 메서드 호출 (ElevatorService의 스레드 시작)
    public void startSimulation() {
        elevatorService.startSimulation();
    }
    
    // 시뮬레이션 중지 메서드 호출 (ElevatorService의 스레드 종료)
    public void stopSimulation() {
        elevatorService.stopSimulation();
    }
    
    public void printStatistics() {
        elevatorService.printStatistics();
    }

    public Elevator getElevator() {
        return elevatorService.getElevator();
    }
    
    public List<Passenger> getCompletedPassengers() {
        return elevatorService.getCompletedPassengers();
    }
    
    public Queue<Passenger> getWaitingPassengers() {
        return elevatorService.getWaitingPassengers();
    }
    
    public boolean isRunning() {
        return elevatorService.isRunning();
    }
    
    public LoggerFactory getLoggerFactory() {
        return elevatorService.getLoggerFactory();
    }

    /**
     * ElevatorService의 스레드 상태 리스너를 설정합니다.
     * GUI (SimulationView)에서 스레드 상태 변화를 감지할 수 있도록 합니다.
     * @param listener 스레드 상태 변화를 수신할 리스너
     */
    public void setThreadStatusListener(ThreadStatusListener listener) {
        elevatorService.setThreadStatusListener(listener);
    }

    /**
     * ElevatorService의 엘리베이터 상태 리스너를 설정합니다.
     * GUI (SimulationView)에서 엘리베이터 상태 변화를 감지하여 화면을 갱신할 수 있도록 합니다.
     * @param listener 엘리베이터 상태 변화를 수신할 리스너
     */
    public void setElevatorStateListener(ElevatorStateListener listener) {
        elevatorService.setElevatorStateListener(listener);
    }

    /**
     * ElevatorService 객체 자체를 반환합니다.
     * (필요에 따라 사용하지만, 일반적으로 컨트롤러는 서비스의 세부 구현을 숨기는 것이 좋습니다.)
     * @return ElevatorService 인스턴스
     */
    public ElevatorService getElevatorService() {
        return elevatorService;
    }
}