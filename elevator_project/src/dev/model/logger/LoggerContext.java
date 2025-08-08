package dev.model.logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 로깅 컨텍스트 클래스
 * 통합된 로깅 기능을 제공합니다.
 */
public class LoggerContext {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    private static boolean loggingEnabled = true;
    
    /**
     * 로그 메시지 출력
     * @param level 로그 레벨 (INFO, WARN, ERROR 등)
     * @param message 로그 메시지
     */
    public static void log(String level, String message) {
        if (loggingEnabled) {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            String logEntry = String.format("[%s] [%s] %s", timestamp, level, message);
            System.out.println(logEntry);
        }
    }
    
    /**
     * INFO 레벨 로그
     */
    public static void info(String message) {
        log("INFO", message);
    }
    
    /**
     * WARN 레벨 로그
     */
    public static void warn(String message) {
        log("WARN", message);
    }
    
    /**
     * ERROR 레벨 로그
     */
    public static void error(String message) {
        log("ERROR", message);
    }
    
    /**
     * DEBUG 레벨 로그
     */
    public static void debug(String message) {
        log("DEBUG", message);
    }
    
    /**
     * 로깅 활성화/비활성화
     */
    public static void setLoggingEnabled(boolean enabled) {
        loggingEnabled = enabled;
    }
    
    /**
     * 로깅 활성화 상태 확인
     */
    public static boolean isLoggingEnabled() {
        return loggingEnabled;
    }
}