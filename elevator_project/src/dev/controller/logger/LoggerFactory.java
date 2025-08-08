package dev.controller.logger;

import dev.model.logger.LogContext;
import dev.model.logger.strategy.FileLogStrategy;
import dev.model.logger.strategy.LogStrategy;
import dev.model.logger.strategy.Slf4jLogStrategy;
import dev.view.logger.ConsoleLogStrategy;

public class LoggerFactory {

    public static void useConsoleLogger() {
        LogContext.setStrategy(new ConsoleLogStrategy());
    }

    public static void useFileLogger(String filePath) {
        LogContext.setStrategy(new FileLogStrategy(filePath));
    }

    public static void useSlf4jLogger(Class<?> clazz) {
        LogContext.setStrategy(new Slf4jLogStrategy(clazz));
    }

    // 추가적으로 사용자 정의 로그 전략도 주입할 수 있어
    public static void useCustomLogger(LogStrategy custom) {
        LogContext.setStrategy(custom);
    }
}
