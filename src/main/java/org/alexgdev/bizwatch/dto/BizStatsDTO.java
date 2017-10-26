package org.alexgdev.bizwatch.dto;

import java.util.Date;

import org.alexgdev.bizwatch.entities.BizStatsEntry;
import org.alexgdev.bizwatch.entities.CryptoCoin;

import lombok.Data;

@Data
public class BizStatsDTO {
	private Long id;
	private CryptoCoin coin;
	private Integer nrThreads;
	private Integer nrPosts;
	private Integer positiveMentions = 0;
	private Integer negativeMentions = 0;
	private Double averageSentiment = 0.0;
	private String top5words ="";
	private String date;
	private Double price_usd;
	private Double price_btc;
	private Double market_cap_usd;
	private Double available_supply;
	private Double total_supply;
	
	public BizStatsDTO(){};
	
	public BizStatsDTO(BizStatsEntry entry){
		this.id = entry.getId();
		this.coin = entry.getCoin();
		this.nrThreads = entry.getNrThreads();
		this.nrPosts = entry.getNrPosts();
		this.positiveMentions = entry.getPositiveMentions();
		this.negativeMentions = entry.getNegativeMentions();
		this.top5words = entry.getTop5words();
		this.date = entry.getDate().toString();
		this.price_usd = entry.getPrice_usd();
		this.price_btc = entry.getPrice_btc();
		this.market_cap_usd = entry.getMarket_cap_usd();
		this.available_supply = entry.getAvailable_supply();
		this.total_supply = entry.getTotal_supply();
		this.averageSentiment = entry.getAverageSentiment();
	}
	
	

}
