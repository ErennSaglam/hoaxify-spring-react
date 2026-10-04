package com.hoaxify.ws.services;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.hoaxify.ws.dto.DtoTag;
import com.hoaxify.ws.entities.Tag;

public interface ITagService {

	List<DtoTag> getTags();

	/** Var olan etiketleri bulur, olmayanları oluşturur. Servisler arası kullanım için entity döner. */
	Set<Tag> findOrCreate(Collection<String> names);

	/** "#Java " -> "java" */
	String normalize(String name);
}
