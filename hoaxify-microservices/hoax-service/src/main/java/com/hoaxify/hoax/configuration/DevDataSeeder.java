package com.hoaxify.hoax.configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hoaxify.hoax.entities.Hoax;
import com.hoaxify.hoax.entities.Tag;
import com.hoaxify.hoax.repository.HoaxRepository;
import com.hoaxify.hoax.services.ITagService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Dev verisi: auth-service'in oluşturduğu user1..user3 (id 1..3) için örnek hoax'lar */
@Slf4j
@Component
@ConditionalOnProperty(name = "hoaxify.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

	private final HoaxRepository hoaxRepository;
	private final ITagService tagService;

	@Override
	@Transactional
	public void run(String... args) {
		if (hoaxRepository.count() > 0) {
			return;
		}
		// findOrCreate: etiketler önceden (ör. kullanıcıların hoax'larıyla) oluşmuş olabilir
		Map<String, Tag> tags = tagService.findOrCreate(List.of("java", "spring", "grpc"))
				.stream().collect(Collectors.toMap(Tag::getName, Function.identity()));
		save(1L, "Mikroservis mimarisine hos geldin!", tags.get("spring"));
		save(1L, "hoax-service yazar bilgisini user-service'ten gRPC ile cekiyor", tags.get("grpc"), tags.get("java"));
		save(2L, "Kayit olayi RabbitMQ ile user-service'e ulasti", tags.get("spring"));
		save(3L, "Her servisin kendi veritabani var", tags.get("java"));
		log.info("Seeded {} hoaxes", hoaxRepository.count());
	}

	private void save(Long userId, String content, Tag... tags) {
		Hoax hoax = new Hoax();
		hoax.setUserId(userId);
		hoax.setContent(content);
		hoax.getTags().addAll(List.of(tags));
		hoaxRepository.save(hoax);
	}
}
