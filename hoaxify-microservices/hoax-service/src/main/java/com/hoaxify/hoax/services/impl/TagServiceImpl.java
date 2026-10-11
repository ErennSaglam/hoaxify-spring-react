package com.hoaxify.hoax.services.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoaxify.hoax.dto.DtoTag;
import com.hoaxify.hoax.entities.Tag;
import com.hoaxify.hoax.mapper.HoaxMapper;
import com.hoaxify.hoax.repository.TagRepository;
import com.hoaxify.hoax.services.ITagService;

import lombok.RequiredArgsConstructor;

/** Monolith'teki TagServiceImpl'in aynısı */
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements ITagService {

	private final TagRepository tagRepository;
	private final HoaxMapper hoaxMapper;

	@Override
	@Transactional(readOnly = true)
	public List<DtoTag> getTags() {
		return tagRepository.findAllOrderByUsage().stream().map(hoaxMapper::toDto).toList();
	}

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
