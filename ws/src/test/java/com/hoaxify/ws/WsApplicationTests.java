package com.hoaxify.ws;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import com.hoaxify.ws.services.ITokenService;
import com.hoaxify.ws.services.impl.OpaqueTokenServiceImpl;
import com.hoaxify.ws.support.PostgresTestContainer;

/** Tüm bean'ler birbirine bağlanabiliyor mu? Konfigürasyon hatalarını en erken burada yakalarız. */
@SpringBootTest
@ActiveProfiles("test")
class WsApplicationTests implements PostgresTestContainer {

	@Autowired
	private ApplicationContext context;

	@Test
	void contextLoads_withOpaqueTokenStrategyByDefault() {
		assertThat(context.getBean(ITokenService.class)).isInstanceOf(OpaqueTokenServiceImpl.class);
	}
}
