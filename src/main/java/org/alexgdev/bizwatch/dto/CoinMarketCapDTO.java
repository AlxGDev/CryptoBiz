package org.alexgdev.bizwatch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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
	@JsonProperty(value = "24h_volume_usd")
	private Double volume = 0.0;
	private Double market_cap_usd = 0.0;
	private Double available_supply = 0.0;
	private Double total_supply = 0.0;
	private Double percent_change_1h;
	private Double percent_change_24h;
	private Double percent_change_7d;
	private String last_updated;
	private Double volume_marketcap_ratio;
	private String announcementUrl;
	

}
