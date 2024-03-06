package com.xxl.job.executor.test;

import com.xxl.job.core.context.XxlJobDelayHelper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

@SpringBootTest
public class XxlJobExecutorExampleBootApplicationTests {

	@Test
	public void test() {
		XxlJobDelayHelper.pushDelayTask("demoTask", "demoDelayHandler", "delayParam", LocalDateTime.now().plusMinutes(2));
	}

	@Test
	public void testBatch(){
		for (int i = 0; i < 2000; i++) {
			XxlJobDelayHelper.pushDelayTask("demoTask" + i, "demoDelayHandler", "delayParam" + i, LocalDateTime.now().plusSeconds(i));
		}
	}

	@Test
	public void testBatch2(){
		for (int i = 0; i < 4000; i++) {
			XxlJobDelayHelper.pushDelayTask("demoTask" + i, "demoDelayHandler", "delayParam" + i, LocalDateTime.now().plusMinutes(5));
		}
	}

}