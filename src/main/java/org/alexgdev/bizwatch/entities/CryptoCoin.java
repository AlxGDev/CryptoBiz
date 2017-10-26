package org.alexgdev.bizwatch.entities;

import org.alexgdev.bizwatch.dto.CoinMarketCapDTO;

import io.vertx.core.json.JsonObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;


@Data
@EqualsAndHashCode()
@ToString()
public class CryptoCoin {

	private String id;
	
	private String name;

	private String symbol;
	
	public CryptoCoin(){};
	public CryptoCoin(CoinMarketCapDTO dto){
		this.id = dto.getId().toLowerCase();
		this.name = dto.getName().toLowerCase();
		this.symbol = dto.getSymbol().toLowerCase();
	}
	public CryptoCoin(JsonObject json){
		if (json.getValue("id") instanceof String) {
			id = (String) json.getValue("id");
		}	    
		if (json.getValue("name") instanceof String) {
			name = (String) json.getValue("name");
		}
		if (json.getValue("symbol") instanceof String) {
			symbol = (String) json.getValue("symbol");
		}
	}
	public JsonObject toJson(){
		JsonObject json = new JsonObject();
		json.put("id", id);
		json.put("name", name.substring(0, 1).toUpperCase() + name.substring(1));
		json.put("symbol", symbol.toUpperCase());
		return json;
	}
	
	

}
