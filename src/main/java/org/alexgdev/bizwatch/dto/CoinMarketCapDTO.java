package org.alexgdev.bizwatch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoinMarketCapDTO {
	
	private String id;
	private String name;
	private String symbol;
	private Long rank;	
	private Double price_usd;
	private Double price_btc;
	private Double market_cap_usd;
	private Double available_supply;
	private Double total_supply;
	private Double percent_change_1h;
	private Double percent_change_24h;
	private Double percent_change_7d;
	private String last_updated;
	

}
