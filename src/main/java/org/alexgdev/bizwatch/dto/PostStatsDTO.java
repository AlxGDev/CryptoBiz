package org.alexgdev.bizwatch.dto;


import lombok.Data;

@Data
public class PostStatsDTO {
	
	private int countMentions;
	private int classifiedPositive;
	private int classifiedNegative;
	private double averageScore;
	private String top5words;
	
	public PostStatsDTO(int countMentions, int classifiedPositive, int classifiedNegative, double averageScore, String top5){
		this.countMentions = countMentions;
		this.classifiedPositive = classifiedPositive;
		this.classifiedNegative = classifiedNegative;
		this.averageScore = averageScore;
		this.top5words = top5;
	}

}
