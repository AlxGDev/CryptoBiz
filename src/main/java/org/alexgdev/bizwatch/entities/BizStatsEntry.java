package org.alexgdev.bizwatch.entities;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.alexgdev.bizwatch.dto.CoinMarketCapDTO;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
@Entity
@Table(name="bizstats")
@Data
public class BizStatsEntry {
	
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
	private Long id;

	@NotNull
    @ManyToOne
    @JoinColumn(name = "cryptocoin_id")
	private CryptoCoin coin;
	
	@NotNull
	private Integer nrThreads = 0;
	
	@NotNull
	private Integer nrPosts = 0;
	
	@NotNull
	private Integer positiveMentions = 0;
	
	@NotNull
	private Integer negativeMentions = 0;
	
	@NotNull
	private Double averageSentiment = 0.0;
	
	private String top5words ="";
	
	
	@NotNull
	private Date date;
	
	
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

}
