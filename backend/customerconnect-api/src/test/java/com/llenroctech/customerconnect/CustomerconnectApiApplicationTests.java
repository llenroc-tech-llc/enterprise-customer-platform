package com.llenroctech.customerconnect;

import com.llenroctech.customerconnect.service.SmsService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class CustomerconnectApiApplicationTests {

	@MockitoBean
	private SmsService smsService;

	@Test
	void contextLoads() {
	}

}
