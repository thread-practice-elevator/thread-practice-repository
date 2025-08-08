package dev;

import dev.controller.ElevatorController;
import dev.guiview.SimulationView;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // 엘리베이터 시뮬레이션의 컨트롤러를 생성합니다.
        // 1층부터 10층까지, 최대 5명의 승객을 태울 수 있도록 설정했습니다.
        ElevatorController controller = new ElevatorController(1, 10, 5);
        
        SwingUtilities.invokeLater(() -> {
            SimulationView view = new SimulationView(controller);
            view.setVisible(true);
        });
    }
}