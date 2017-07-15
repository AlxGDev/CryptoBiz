package org.alexgdev.bizwatch.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageDTO {
	private Integer page;
	private List<PageEntryDTO> threads;

}
