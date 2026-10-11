package com.hoaxify.hoax.services;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.hoaxify.hoax.dto.DtoTag;
import com.hoaxify.hoax.entities.Tag;

public interface ITagService {

	List<DtoTag> getTags();

	Set<Tag> findOrCreate(Collection<String> names);

	String normalize(String name);
}
