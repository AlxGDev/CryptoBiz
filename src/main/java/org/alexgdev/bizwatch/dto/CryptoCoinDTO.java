package org.alexgdev.bizwatch.dto;

import org.alexgdev.bizwatch.entities.CryptoCoin;

import lombok.Data;

@Data
public class CryptoCoinDTO {

	private String id;

	private String name;

	private String symbol;
	
	public CryptoCoinDTO(){};
	public CryptoCoinDTO (CryptoCoin coin){
		this.id = coin.getId();
		this.name = coin.getName().substring(0, 1).toUpperCase() + coin.getName().substring(1);
		this.symbol = coin.getSymbol().toUpperCase();
	}

}
