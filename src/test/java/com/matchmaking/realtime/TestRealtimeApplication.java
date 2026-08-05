package com.matchmaking.realtime;

import org.springframework.boot.SpringApplication;

public class TestRealtimeApplication {

	public static void main(String[] args) {
		SpringApplication.from(RealtimeApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
