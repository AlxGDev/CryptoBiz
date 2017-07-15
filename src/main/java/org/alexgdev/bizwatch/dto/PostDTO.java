package org.alexgdev.bizwatch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostDTO {
	private Long no;
	private String sub = "";
	private String com = "";
}
