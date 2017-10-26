package org.alexgdev.bizwatch.entities;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;

import javax.validation.constraints.NotNull;

import org.alexgdev.bizwatch.dto.CoinMarketCapDTO;

import io.vertx.core.json.JsonObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
public class BizStatsEntry {
	
	private Long id;
	private CryptoCoin coin;
	private Integer nrThreads = 0;
	private Integer nrPosts = 0;
	private Integer positiveMentions = 0;
	private Integer negativeMentions = 0;
	private Double averageSentiment = 0.0;
	private String top5words ="";
	private Instant date;
	private Double price_usd;
	private Double price_btc;
	private Double market_cap_usd;
	private Double available_supply;
	private Double total_supply;
	
	public BizStatsEntry(){};
	public BizStatsEntry(CoinMarketCapDTO dto){
		this.price_usd = dto.getPrice_usd();
		this.price_btc = dto.getPrice_btc();
		this.market_cap_usd = dto.getMarket_cap_usd();
		this.available_supply = dto.getAvailable_supply();
		this.total_supply = dto.getTotal_supply();
	}
	
	public BizStatsEntry(JsonObject json){
		if (json.getValue("id") instanceof Number) {
			id = ((Number)json.getValue("id")).longValue();
		}	    
		if (json.getValue("cryptocoin_id") instanceof String && json.getValue("name") instanceof String && json.getValue("symbol") instanceof String) {
			CryptoCoin coin = new CryptoCoin();
			coin.setId((String) json.getValue("cryptocoin_id"));
			coin.setName((String) json.getValue("name"));
			coin.setSymbol((String) json.getValue("symbol"));
			this.coin = coin;
		}
		if (json.getValue("nr_threads") instanceof Number) {
			nrThreads = ((Number)json.getValue("nr_threads")).intValue();
		}	
		if (json.getValue("nr_posts") instanceof Number) {
			nrPosts = ((Number)json.getValue("nr_posts")).intValue();
		}
		if (json.getValue("positive_mentions") instanceof Number) {
			positiveMentions = ((Number)json.getValue("positive_mentions")).intValue();
		}
		if (json.getValue("negative_mentions") instanceof Number) {
			negativeMentions = ((Number)json.getValue("negative_mentions")).intValue();
		}
		if (json.getValue("average_sentiment") instanceof Number) {
			averageSentiment = ((Number)json.getValue("average_sentiment")).doubleValue();
		}
		if (json.getValue("top5words") instanceof String) {
			top5words = (String) json.getValue("top5words");
		}
		if (json.getValue("date") instanceof String) {
			date = Instant.parse(((String) json.getValue("date")));
			
		}
		if (json.getValue("price_usd") instanceof Number) {
			price_usd = ((Number)json.getValue("price_usd")).doubleValue();
		}
		if (json.getValue("price_btc") instanceof Number) {
			price_btc = ((Number)json.getValue("price_btc")).doubleValue();
		}
		if (json.getValue("market_cap_usd") instanceof Number) {
			market_cap_usd = ((Number)json.getValue("market_cap_usd")).doubleValue();
		}
		if (json.getValue("available_supply") instanceof Number) {
			available_supply = ((Number)json.getValue("available_supply")).doubleValue();
		}
		if (json.getValue("total_supply") instanceof Number) {
			total_supply = ((Number)json.getValue("total_supply")).doubleValue();
		}
	}
	public JsonObject toJson(){
		JsonObject json = new JsonObject();
		json.put("id", id);
		json.put("coin", coin.toJson());
		json.put("nrThreads", nrThreads);
		json.put("nrPosts", nrPosts);
		json.put("positiveMentions", positiveMentions);
		json.put("negativeMentions", negativeMentions);
		json.put("averageSentiment", averageSentiment);
		json.put("top5words", top5words);
		json.put("date", date);
		json.put("price_usd", price_usd);
		json.put("price_btc", price_btc);
		json.put("market_cap_usd", market_cap_usd);
		json.put("available_supply", available_supply);
		json.put("total_supply", total_supply);
		return json;
	}

}
