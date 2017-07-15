package org.alexgdev.bizwatch.service;


import org.alexgdev.bizwatch.dto.CryptoCoinDTO;
import org.alexgdev.bizwatch.entities.CryptoCoin;
import org.alexgdev.bizwatch.entities.CryptoCoinRepository;
import org.alexgdev.bizwatch.exception.NotFoundException;
import org.alexgdev.bizwatch.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class CoinService {
	
	@Autowired
	private CryptoCoinRepository repo;
	
	public Page<CryptoCoinDTO> getAll(Pageable pageable){
		Page<CryptoCoin> entries = repo.findAll(pageable);
		Page<CryptoCoinDTO> dtoPage = entries.map(new Converter<CryptoCoin, CryptoCoinDTO>() {
		    @Override
		    public CryptoCoinDTO convert(CryptoCoin entity) {
		    	
		    		return new CryptoCoinDTO(entity);
		        
		    }
		});
		
		return dtoPage;
	}
	
	
	public CryptoCoin getById(String coinId) throws ServiceException, NotFoundException{
		if(coinId == null){
			throw new ServiceException("Invalid Coin ID");
			
		}
		CryptoCoin coin = repo.findOne(coinId);
		if(coin == null){
			throw new NotFoundException("No coin with ID "+coinId+ " found!");
			
		}
		return coin;
	}

}
