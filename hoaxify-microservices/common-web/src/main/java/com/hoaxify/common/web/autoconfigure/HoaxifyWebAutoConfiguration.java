package com.hoaxify.common.web.autoconfigure;

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.hoaxify.common.web.exception.GlobalExceptionHandler;
import com.hoaxify.common.web.i18n.MessageResolver;
import com.hoaxify.common.web.security.CurrentUserIdArgumentResolver;

/**
 * Spring Boot auto-configuration: bu jar classpath'e girdiği anda Spring buradaki bean'leri kendisi oluşturur.
 * Servisin @ComponentScan ile com.hoaxify.common paketini taraması gerekmez.
 * Nasıl bulunuyor? META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports dosyası.
 * spring-boot-starter-web, spring-boot-starter-data-jpa gibi tüm "starter"lar bu mekanizmayla çalışır.
 *
 * @ConditionalOnMissingBean: servis isterse kendi MessageResolver'ını tanımlayıp bunu ezebilir.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
public class HoaxifyWebAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public MessageResolver messageResolver(MessageSource messageSource) {
		return new MessageResolver(messageSource);
	}

	@Bean
	@ConditionalOnMissingBean
	public GlobalExceptionHandler globalExceptionHandler(MessageResolver messageResolver) {
		return new GlobalExceptionHandler(messageResolver);
	}

	@Bean
	public WebMvcConfigurer currentUserIdWebMvcConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
				resolvers.add(new CurrentUserIdArgumentResolver());
			}
		};
	}
}
