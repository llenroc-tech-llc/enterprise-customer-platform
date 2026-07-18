package com.llenroctech.customerconnect;

import com.llenroctech.customerconnect.provider.TokenProvider;
import com.llenroctech.customerconnect.service.SmsService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties =
        "jwt.secret=context-load-test-only-secret"
)
class CustomerconnectApiApplicationTests {

	@MockitoBean
	private SmsService smsService;

	@MockitoBean
	private TokenProvider tokenProvider;

	@Test
	void contextLoads() {
	}

}
