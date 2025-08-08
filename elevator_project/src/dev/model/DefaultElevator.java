package dev.model;

import java.util.List;
import java.util.Set;

/**
 * 엘레베이터의 기본 동작을 정의하는 인터페이스
 * MVC 패턴에서 Model의 추상화를 담당합니다.
 */
public interface DefaultElevator {
    
    // ========== 기본 정보 ==========
    
    /**
     * 엘레베이터 ID를 반환합니다.
     */
    String getId();
    
    /**
     * 최저층을 반환합니다.
     */
    int getMinFloor();
    
    /**
     * 최고층을 반환합니다.
     */
    int getMaxFloor();
    
    /**
     * 최대 수용 인원을 반환합니다.
     */
    int getCapacity();
    
    // ========== 현재 상태 ==========
    
    /**
     * 현재 층을 반환합니다.
     */
    int getCurrentFloor();
    
    /**
     * 현재 이동 방향을 반환합니다.
     */
    Direction getCurrentDirection();
    
    /**
     * 현재 탑승한 승객 수를 반환합니다.
     */
    int getCurrentPassengerCount();
    
    /**
     * 엘레베이터가 꽉 찼는지 확인합니다.
     */
    boolean isFull();
    
    /**
     * 엘레베이터가 비어있는지 확인합니다.
     */
    boolean isEmpty();
    
    /**
     * 엘레베이터가 이동 중인지 확인합니다.
     */
    boolean isMoving();
    
    // ========== 층 설정 ==========
    
    /**
     * 현재 층을 설정합니다.
     * @param floor 설정할 층 번호
     * @return 설정 성공 여부
     */
    boolean setCurrentFloor(int floor);
    
    /**
     * 현재 방향을 설정합니다.
     */
    void setDirection(Direction direction);
    
    // ========== 요청 관리 ==========
    
    /**
     * 층 요청을 추가합니다.
     * @param floor 요청할 층
     */
    void addRequest(int floor);
    
    /**
     * 층 요청을 제거합니다.
     * @param floor 제거할 층
     */
    void removeRequest(int floor);
    
    /**
     * 특정 층에 요청이 있는지 확인합니다.
     */
    boolean hasRequestAt(int floor);
    
    /**
     * 처리할 요청이 있는지 확인합니다.
     */
    boolean hasRequests();
    
    /**
     * 모든 요청 목록을 반환합니다.
     */
    Set<Integer> getRequests();
    
    /**
     * SCAN 알고리즘에 따른 다음 목적지를 반환합니다.
     * @return 다음 층 번호, 요청이 없으면 null
     */
    Integer getNextDestination();
    
    // ========== 승객 관리 ==========
    
    /**
     * 승객을 탑승시킵니다.
     * @param passenger 탑승할 승객
     * @return 탑승 성공 여부
     */
    boolean addPassenger(Passenger passenger);
    
    /**
     * 특정 층에서 하차할 승객들을 제거합니다.
     * @param floor 하차할 층
     * @return 하차한 승객 목록
     */
    List<Passenger> removePassengersAt(int floor);
    
    /**
     * 현재 탑승한 승객 목록을 반환합니다.
     */
    List<Passenger> getCurrentPassengers();
    
    // ========== 이동 및 동작 ==========
    
    /**
     * 지정된 층으로 이동합니다.
     * @param floor 목적지 층
     * @return 이동 가능 여부
     */
    boolean canMoveTo(int floor);
    
    /**
     * 경계층(최상층 또는 최하층)에 있는지 확인합니다.
     */
    boolean isAtBoundary();
    
    /**
     * 유효한 층 번호인지 확인합니다.
     * @param floor 확인할 층 번호
     * @return 유효성 여부
     */
    boolean isValidFloor(int floor);
    
    // ========== 상태 초기화 ==========
    
    /**
     * 엘레베이터를 초기 상태로 리셋합니다.
     */
    void reset();
    
    /**
     * 엘레베이터를 정지 상태로 설정합니다.
     */
    void stop();
    
    // ========== 정보 조회 ==========
    
    /**
     * 엘레베이터의 현재 상태를 문자열로 반환합니다.
     */
    @Override
    String toString();
}