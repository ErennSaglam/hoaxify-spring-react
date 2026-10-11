package com.hoaxify.hoax.entities;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Monolith'ten en önemli fark: "@ManyToOne User user" YOK, sadece "Long userId" var.
 * users tablosu başka bir veritabanında (userdb); veritabanları arası foreign key ve JOIN olamaz.
 * Bu yüzden userId'ye index koyuyoruz (bir kullanıcının hoax'ları sık sorgulanıyor).
 */
@Entity
@Table(name = "hoax", indexes = @Index(name = "idx_hoax_user_id", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
public class Hoax {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "content", nullable = false, length = 1000)
	private String content;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/** Etiketler aynı veritabanında olduğu için @ManyToMany burada devam ediyor */
	@ManyToMany
	@JoinTable(name = "hoax_tag",
			joinColumns = @JoinColumn(name = "hoax_id"),
			inverseJoinColumns = @JoinColumn(name = "tag_id"))
	private Set<Tag> tags = new HashSet<>();
}
