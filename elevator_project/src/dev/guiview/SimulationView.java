package dev.guiview;

import dev.controller.ElevatorController;
import dev.model.Elevator;
import dev.model.Passenger;
import dev.service.ElevatorService; // ElevatorService 임포트 추가

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors; // Collectors 임포트 추가

public class SimulationView extends JFrame {
    private final ElevatorController controller;
    
    private JPanel elevatorPanel;
    private JTextArea logArea;
    private JButton runButton;
    private JButton stopButton;
    private JButton addPassengerButton;

    // 스레드 상태를 시각적으로 표시할 패널들
    private JPanel requestThreadPanel;
    private JLabel requestThreadIcon; // 아이콘을 위한 JLabel
    private JLabel requestThreadText; // 텍스트를 위한 JLabel

    private JPanel movementThreadPanel;
    private JLabel movementThreadIcon;
    private JLabel movementThreadText;

    private JPanel monitorThreadPanel;
    private JLabel monitorThreadIcon;
    private JLabel monitorThreadText;


    public SimulationView(ElevatorController controller) {
        this.controller = controller;
        setTitle("엘리베이터 SCAN 알고리즘 시뮬레이터");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        initializeUI();
        setupEventListeners();
        
        // LoggerFactory에 JTextArea 연결
        controller.getLoggerFactory().setLogArea(logArea);
        
        // ElevatorService에 스레드 상태 리스너 설정
        // 컨트롤러를 통해 서비스의 리스너를 설정합니다.
        controller.setThreadStatusListener(this::updateThreadStatusGUI);
        
        // ElevatorService에 엘리베이터 상태 리스너 설정 (추가된 부분)
        controller.setElevatorStateListener(this::updateStatus);
        
        updateStatus();
    }

    private void initializeUI() {
        JPanel controlPanel = new JPanel();
        runButton = new JButton("시작");
        stopButton = new JButton("중지");
        addPassengerButton = new JButton("승객 추가");
        controlPanel.add(runButton);
        controlPanel.add(stopButton);
        controlPanel.add(addPassengerButton);
        add(controlPanel, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        
        // 왼쪽 패널: 엘리베이터 상태와 스레드 상태를 함께 표시
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS)); // 세로 정렬

        elevatorPanel = new JPanel();
        elevatorPanel.setBorder(BorderFactory.createTitledBorder("엘리베이터 상태"));
        elevatorPanel.setLayout(new BoxLayout(elevatorPanel, BoxLayout.Y_AXIS)); // 엘리베이터 층 표시를 위한 레이아웃

        // 스레드 상태를 시각적으로 표시할 패널
        JPanel threadStatusPanel = new JPanel(new GridLayout(3, 1, 5, 5)); // 3행 1열, 간격 5
        threadStatusPanel.setBorder(BorderFactory.createTitledBorder("스레드 상태"));
        
        // 각 스레드의 상태를 표시할 컴포넌트 초기화
        requestThreadPanel = createThreadStatusPanel("요청 처리 스레드", Color.LIGHT_GRAY, requestThreadIcon, requestThreadText);
        movementThreadPanel = createThreadStatusPanel("이동 제어 스레드", Color.LIGHT_GRAY, movementThreadIcon, movementThreadText);
        monitorThreadPanel = createThreadStatusPanel("상태 감시 스레드", Color.LIGHT_GRAY, monitorThreadIcon, monitorThreadText);
        
        threadStatusPanel.add(requestThreadPanel);
        threadStatusPanel.add(movementThreadPanel);
        threadStatusPanel.add(monitorThreadPanel);

        leftPanel.add(elevatorPanel);
        leftPanel.add(Box.createVerticalStrut(10)); // 간격 추가
        leftPanel.add(threadStatusPanel);
        leftPanel.add(Box.createVerticalGlue()); // 하단에 공간 채우기

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("로그"));
        
        mainPanel.add(leftPanel);
        mainPanel.add(scrollPane);
        add(mainPanel, BorderLayout.CENTER);
    }
    
    // 각 스레드 상태 패널을 생성하는 헬퍼 메서드
    private JPanel createThreadStatusPanel(String title, Color initialColor, JLabel iconLabelRef, JLabel textLabelRef) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBackground(initialColor);
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY)); // 테두리 추가

        iconLabelRef = new JLabel("⚫"); // 초기 아이콘 (정지 상태)
        iconLabelRef.setForeground(Color.DARK_GRAY);
        iconLabelRef.setFont(new Font("맑은 고딕", Font.BOLD, 16)); // 아이콘 크기 키우기
        
        textLabelRef = new JLabel(title);
        textLabelRef.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        
        panel.add(iconLabelRef);
        panel.add(textLabelRef);
        
        // 필드에 JLabel 참조 할당 (람다에서 접근 가능하도록)
        if (title.contains("요청 처리")) {
            this.requestThreadIcon = iconLabelRef;
            this.requestThreadText = textLabelRef;
        } else if (title.contains("이동 제어")) {
            this.movementThreadIcon = iconLabelRef;
            this.movementThreadText = textLabelRef;
        } else if (title.contains("상태 감시")) {
            this.monitorThreadIcon = iconLabelRef;
            this.monitorThreadText = textLabelRef;
        }
        
        return panel;
    }

    // 스레드 상태를 GUI에 업데이트하는 메서드 (ElevatorService에서 호출됨)
    public void updateThreadStatusGUI(String threadName, String status) {
        SwingUtilities.invokeLater(() -> { // EDT에서 UI 업데이트
            JPanel targetPanel = null;
            JLabel targetIcon = null;
            JLabel targetText = null;
            Color color = Color.LIGHT_GRAY;
            String iconChar = "⚫";
            
            if (status.equals("실행 중")) {
                color = new Color(144, 238, 144); // 연한 녹색
                iconChar = "🟢"; // 녹색 원
            } else if (status.equals("종료")) {
                color = new Color(255, 182, 193); // 연한 빨간색
                iconChar = "🔴"; // 빨간색 원
            } else if (status.equals("대기 중")) { // 스레드가 잠시 대기 상태일 경우
                color = new Color(255, 255, 153); // 연한 노란색
                iconChar = "🟡"; // 노란색 원
            }

            if (threadName.equals("요청 처리")) {
                targetPanel = requestThreadPanel;
                targetIcon = requestThreadIcon;
                targetText = requestThreadText;
            } else if (threadName.equals("이동 제어")) {
                targetPanel = movementThreadPanel;
                targetIcon = movementThreadIcon;
                targetText = movementThreadText;
            } else if (threadName.equals("상태 감시")) {
                targetPanel = monitorThreadPanel;
                targetIcon = monitorThreadIcon;
                targetText = monitorThreadText;
            }

            if (targetPanel != null && targetIcon != null && targetText != null) {
                targetPanel.setBackground(color);
                targetIcon.setText(iconChar);
                targetText.setText(threadName + ": " + status); // 텍스트도 업데이트
                targetPanel.revalidate();
                targetPanel.repaint();
            }
        });
    }
    
    private void setupEventListeners() {
        runButton.addActionListener(this::onRunSimulation);
        stopButton.addActionListener(this::onStopSimulation);
        addPassengerButton.addActionListener(this::onAddPassenger);
    }

    private void onRunSimulation(ActionEvent e) {
    	if (!controller.isRunning()) {
            controller.startSimulation();
        }
    }
    
    private void onStopSimulation(ActionEvent e) {
        if (controller.isRunning()) {
            controller.stopSimulation(); // ElevatorService의 stopSimulation 호출
            logArea.setText(""); // 로그 패널 내용 지우기
            updateStatus(); // 최종 상태 업데이트
        }
    }
    
    private void onAddPassenger(ActionEvent e) {
        JPanel panel = new JPanel(new GridLayout(2, 2));
        JTextField startFloorField = new JTextField();
        JTextField destFloorField = new JTextField();
        panel.add(new JLabel("출발 층:"));
        panel.add(startFloorField);
        panel.add(new JLabel("도착 층:"));
        panel.add(destFloorField);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "승객 요청", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                int startFloor = Integer.parseInt(startFloorField.getText());
                int destFloor = Integer.parseInt(destFloorField.getText());
                controller.addPassengerRequest(startFloor, destFloor);
                updateStatus();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "유효한 층 번호를 입력하세요.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void updateStatus() {
        // SwingUtilities.invokeLater를 사용하여 EDT에서 실행되도록 보장
        SwingUtilities.invokeLater(() -> {
            Elevator elevator = controller.getElevator();
            elevatorPanel.removeAll();
            elevatorPanel.setLayout(new BoxLayout(elevatorPanel, BoxLayout.Y_AXIS));
            
            // 현재 대기 중인 모든 승객 목록을 가져옵니다.
            Queue<Passenger> allWaitingPassengers = controller.getWaitingPassengers();

            for (int floor = elevator.getMaxFloor(); floor >= elevator.getMinFloor(); floor--) {
                final int currentFloorInLoop = floor; 
                
                // 해당 층에 대기 중인 승객 수를 계산합니다.
                long passengersWaitingOnThisFloor = allWaitingPassengers.stream()
                                                    .filter(p -> p.getStartFloor() == currentFloorInLoop)
                                                    .count();

                String floorStatus = String.format("층 %d: ", currentFloorInLoop);
                if (currentFloorInLoop == elevator.getCurrentFloor()) {
                    floorStatus += "🛗 [현재 엘리베이터 위치] ";
                }
                
                // 대기 승객 수에 따라 이모티콘을 추가합니다.
                if (passengersWaitingOnThisFloor > 0) {
                    StringBuilder emojiString = new StringBuilder();
                    int displayCount = (int) Math.min(passengersWaitingOnThisFloor, 5); // 최대 5개 이모티콘
                    for (int i = 0; i < displayCount; i++) {
                        emojiString.append("👨");
                    }
                    if (passengersWaitingOnThisFloor > 5) {
                        emojiString.append("+"); // 5명 초과 시 + 표시
                    }
                    floorStatus += " [대기: " + emojiString.toString() + "] ";
                }
                
                JLabel floorLabel = new JLabel(floorStatus);
                elevatorPanel.add(floorLabel);
            }
            
            // 엘리베이터 내부 승객 수를 이모티콘으로 표시
            StringBuilder onboardEmojiString = new StringBuilder();
            int onboardCount = elevator.getPassengers().size();
            int onboardDisplayCount = (int) Math.min(onboardCount, 5); // 최대 5개 이모티콘
            for (int i = 0; i < onboardDisplayCount; i++) {
                onboardEmojiString.append("👤"); // 탑승 승객 이모티콘
            }
            if (onboardCount > 5) {
                onboardEmojiString.append("+"); // 5명 초과 시 + 표시
            }
            JLabel passengerLabel = new JLabel("엘리베이터 내부 승객: " + onboardEmojiString.toString());
            elevatorPanel.add(new JSeparator());
            elevatorPanel.add(passengerLabel);
            
            elevatorPanel.revalidate();
            elevatorPanel.repaint();
        });
    }
}
