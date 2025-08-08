package dev;

import dev.controller.logger.LoggerFactory;
import dev.model.logger.LogContext;

public class Main {
    public static void main(String[] args) {
        LoggerFactory.useConsoleLogger();
        LogContext.log("INFO", "콘솔 출력!");

<<<<<<< HEAD
	public static void main(String[] args) {
		System.out.println("dd");
	}
=======
        LoggerFactory.useFileLogger("log/elevator.log");
        LogContext.log("WARN", "파일에 기록됨!");
>>>>>>> 06599a1 (Feat : Logger 구현)

        LoggerFactory.useSlf4jLogger(Main.class);
        LogContext.log("ERROR", "SLF4J 로그 기록됨!");
    }
}
