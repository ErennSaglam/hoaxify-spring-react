package com.hoaxify.ws.entities;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Kullanıcının paylaştığı kısa gönderi (tweet benzeri).
 */
@Entity
@Table(name = "hoax")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Hoax {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "content", nullable = false, length = 1000)
	private String content;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/** @ManyToOne - birçok hoax tek bir kullanıcıya ait. FK: hoax.user_id */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	/**
	 * @ManyToMany + @JoinTable - ilişkinin sahibi Hoax. Ara tablo: hoax_tag(hoax_id, tag_id).
	 * List yerine Set: Hibernate List (bag) üzerinde değişiklikte tüm satırları silip
	 * yeniden ekler, Set'te sadece değişen satırı işler.
	 */
	@ManyToMany
	@JoinTable(name = "hoax_tag",
			joinColumns = @JoinColumn(name = "hoax_id"),
			inverseJoinColumns = @JoinColumn(name = "tag_id"))
	private Set<Tag> tags = new HashSet<>();
}
