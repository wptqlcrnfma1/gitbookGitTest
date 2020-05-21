package com.douzone.gitbookGitTest.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableAutoConfiguration //viewResolver도 떠있음
@ComponentScan("com.douzone.gitbookGitTest.controller")
public class BootApplication {

	public static void main(String[] args) {
		SpringApplication.run(BootApplication.class, args);
		//boot가 com.douzone.springbootex.controller 클래스를 가지고 @SpringBootApplication를 알려주고 @EnableAutoConfiguration보고 자동설정 -> MVC가 보인다
		//run하면 톰캣 포트
	}
	
}
