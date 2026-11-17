package com.hsbi.hotelverwaltung;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Startet die komplette App einmal testweise. Dafür braucht es die Datenbank aus der
// compose.yaml. Spring Boot startet die Docker-Container in Tests normalerweise nicht,
// deshalb wird das hier ausdrücklich eingeschaltet. Docker Desktop muss laufen.
@SpringBootTest(properties = "spring.docker.compose.skip.in-tests=false")
class HotelverwaltungApplicationTests {

	@Test
	void contextLoads() {
	}

}
