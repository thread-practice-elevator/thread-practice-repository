package dev.controller.logger;

import javax.swing.JTextArea;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerFactory {
    private JTextArea logArea;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    public void setLogArea(JTextArea logArea) {
        this.logArea = logArea;
    }

    // 기본 log 메서드 (level이 없는 경우)
    public void log(String message) {
        // INFO 레벨로 간주하여 처리
        log("INFO", message);
    }
    
    // 로그 레벨을 받는 오버로드된 log 메서드
    public void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String logEntry = String.format("[%s] [%s] %s", timestamp, level, message);
        
        if (logArea != null) {
            logArea.append(logEntry + "\n");
            // 로그가 많아질 경우 자동으로 스크롤
            logArea.setCaretPosition(logArea.getDocument().getLength());
        } else {
            System.out.println(logEntry);
        }
    }
    
    // 로그 레벨별 편의 메서드 추가
    public void info(String message) {
        log("INFO", message);
    }
    
    public void warn(String message) {
        log("WARN", message);
    }
    
    public void error(String message) {
        log("ERROR", message);
    }
    
    public void debug(String message) {
        log("DEBUG", message);
    }
}