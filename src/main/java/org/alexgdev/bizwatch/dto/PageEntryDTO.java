package org.alexgdev.bizwatch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageEntryDTO {
	private Long no;
	private Integer sticky;
	private String sub = "";
	private String com = "";
	

}
