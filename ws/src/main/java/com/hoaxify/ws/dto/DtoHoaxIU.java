package com.hoaxify.ws.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoHoaxIU {

	@NotBlank
	@Size(max = 1000)
	private String content;

	/** Etiket adları, ör: ["java", "#spring"]. Başındaki # opsiyonel. */
	@Size(max = 5)
	private List<@NotBlank @Size(max = 30) @Pattern(regexp = "^#?[\\p{L}0-9_]+$", message = "{hoaxify.constraint.tag.pattern}") String> tags = new ArrayList<>();
}
