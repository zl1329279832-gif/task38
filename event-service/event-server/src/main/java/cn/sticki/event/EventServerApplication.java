package cn.sticki.event;

import cn.sticki.common.amqp.autoconfig.EnableAmqpMessageConverterConfig;
import cn.sticki.common.redis.autoconfig.EnableRedisSerialize;
import cn.sticki.common.tool.mybatisconfig.EnableMybatisPlusIPage;
import cn.sticki.common.web.advice.EnableDefaultExceptionAdvice;
import cn.sticki.common.web.advice.EnableDefaultResponseAdvice;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableMybatisPlusIPage
@EnableRedisSerialize
@EnableAmqpMessageConverterConfig
@EnableDefaultExceptionAdvice
@EnableDefaultResponseAdvice
@EnableScheduling
public class EventServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventServerApplication.class, args);
	}
}
