package com.logmein.interview.badreddinesDemo;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class BadreddinesDemoApplication {

	public static void main(String[] args) {
		log.atInfo().setMessage("Hello World!").addKeyValue("name", "logmein").addKeyValue("key", "value").log() ;

		SpringApplication.run(BadreddinesDemoApplication.class, args);
	}
}
