package com.hoaxify.hoax.entities;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tag", uniqueConstraints = @UniqueConstraint(name = "uk_tag_name", columnNames = "name"))
@Getter
@Setter
@NoArgsConstructor
public class Tag {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@ManyToMany(mappedBy = "tags")
	private Set<Hoax> hoaxes = new HashSet<>();

	public Tag(String name) {
		this.name = name;
	}
}
