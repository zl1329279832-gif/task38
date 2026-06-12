package cn.sticki.event;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableFeignClients(basePackages = "cn.sticki")
@SpringBootApplication
public class EventServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventServerApplication.class, args);
	}

}
