# 🏢 Elevator Simulation Project

## 📄 개요

이 프로젝트는 **Java**를 사용하여 **멀티스레딩 기반의 엘리베이터 시뮬레이션**을 구현합니다.  
**SCAN(Elevator)** 알고리즘을 적용하여 엘리베이터의 작동 원리를 시각화하고, **동시성 프로그래밍**의 이해를 돕는 데 중점을 두었습니다.

---

## 🚀 주요 기능 및 특징

-   **멀티스레딩**  
    세 가지 핵심 스레드(요청 처리, 이동 제어, 상태 감시)가 독립적으로 작동하여 실시간 시뮬레이션을 가능하게 합니다.
-   **SCAN 알고리즘**  
    엘리베이터가 한 방향으로 요청을 처리하며 이동하고, 끝에 도달하면 방향을 바꾸는 효율적인 알고리즘을 구현했습니다.
-   **MVC 패턴**  
    Model, View, Controller의 역할을 분리하여 코드의 유지보수성과 확장성을 높였습니다.
-   **GUI 시각화**  
    Java Swing을 사용하여 엘리베이터의 현재 상태, 스레드 상태, 실시간 로그를 한눈에 볼 수 있는 사용자 인터페이스를 제공합니다.
-   **로깅 시스템**  
    `LoggerFactory` 클래스를 통해 로깅 기능을 추상화하고, `INFO`, `WARN`, `DEBUG`, `ERROR` 등 로그 레벨별로 메시지를 출력합니다.

---

## 📁 파일 구조

```
src/
├── dev/
│ ├── controller/
│ │ ├── ElevatorController.java (컨트롤러)
│ │ └── logger/
│ │ └── LoggerFactory.java (로깅)
│ ├── guiview/
│ │ └── SimulationView.java (뷰)
│ ├── model/
│ │ ├── Direction.java (모델)
│ │ ├── DefaultElevator.java (인터페이스)
│ │ ├── Elevator.java (모델)
│ │ └── Passenger.java (모델)
│ ├── service/
│ │ ├── ElevatorService.java (서비스)
│ │ └── PassengerService.java (서비스)
└── Main.java (시작점)
```

---

## 🛠️ 실행 방법

### 1. 프로젝트 클론

```
git clone https://github.com/thread-practice-elevator/thread-practice-repository.git
cd thread-practice-repository
```

### 2. 프로젝트 빌드

프로젝트의 소스 코드를 .jar 파일로 빌드해야 합니다.

IDE(IntelliJ IDEA, Eclipse)를 사용하면 빌드 기능을 쉽게 이용할 수 있습니다.

Gradle 또는 Maven 같은 빌드 도구를 사용하거나, IDE의 Export/Build 기능을 통해 JAR 파일을 생성하세요.

### 3. JAR 파일 실행

빌드된 .jar 파일이 있는 디렉터리로 이동합니다.
터미널(Command Prompt 또는 PowerShell)을 열고 다음 명령어를 입력하세요.

```
java -jar [빌드된_jar_파일_이름].jar
```

**예시**

```
java -jar ElevatorSimulation.jar
```
