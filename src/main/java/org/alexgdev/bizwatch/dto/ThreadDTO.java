package org.alexgdev.bizwatch.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThreadDTO {
	private List<PostDTO> posts;

}
