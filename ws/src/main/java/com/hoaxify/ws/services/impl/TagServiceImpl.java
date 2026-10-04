package com.hoaxify.ws.services.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoaxify.ws.dto.DtoTag;
import com.hoaxify.ws.entities.Tag;
import com.hoaxify.ws.mapper.TagMapper;
import com.hoaxify.ws.repository.TagRepository;
import com.hoaxify.ws.services.ITagService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements ITagService {

	private final TagRepository tagRepository;
	private final TagMapper tagMapper;

	@Override
	@Transactional(readOnly = true)
	public List<DtoTag> getTags() {
		return tagRepository.findAllOrderByUsage().stream().map(tagMapper::toDto).toList();
	}

	/**
	 * "Find or create": tek sorguyla var olanları bulur, sadece eksik olanları kaydeder.
	 * Çağıran servisin transaction'ına katılır (varsayılan propagation REQUIRED).
	 */
	@Override
	@Transactional
	public Set<Tag> findOrCreate(Collection<String> names) {
		if (names == null || names.isEmpty()) {
			return new HashSet<>();
		}
		Set<String> normalized = names.stream().map(this::normalize).collect(Collectors.toSet());

		Set<Tag> tags = new HashSet<>(tagRepository.findByNameIn(normalized));
		Set<String> existingNames = tags.stream().map(Tag::getName).collect(Collectors.toSet());

		List<Tag> newTags = normalized.stream()
				.filter(name -> !existingNames.contains(name))
				.map(Tag::new)
				.toList();
		tags.addAll(tagRepository.saveAll(newTags));
		return tags;
	}

	@Override
	public String normalize(String name) {
		String trimmed = name.trim();
		if (trimmed.startsWith("#")) {
			trimmed = trimmed.substring(1);
		}
		return trimmed.toLowerCase(Locale.ROOT);
	}
}
