package org.alexgdev.bizwatch.entities;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.alexgdev.bizwatch.dto.CoinMarketCapDTO;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name="cryptocoin")
@Data
@EqualsAndHashCode(exclude={"statEntries"})
@ToString(exclude={"statEntries"})
public class CryptoCoin {
	@Id
	private String id;
	
	@NotNull
	private String name;
	
	@NotNull
	private String symbol;
	
	@OneToMany(mappedBy = "coin", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BizStatsEntry> statEntries;
	
	public CryptoCoin(){};
	public CryptoCoin(CoinMarketCapDTO dto){
		this.id = dto.getId().toLowerCase();
		this.name = dto.getName().toLowerCase();
		this.symbol = dto.getSymbol().toLowerCase();
	}
	
	

}
