package org.alexgdev.bizwatch.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.alexgdev.bizwatch.dto.BizStatsDTO;
import org.alexgdev.bizwatch.entities.BizStatsEntry;
import org.alexgdev.bizwatch.entities.BizStatsEntryRepository;
import org.alexgdev.bizwatch.entities.CryptoCoin;
import org.alexgdev.bizwatch.exception.NotFoundException;
import org.alexgdev.bizwatch.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class BizStatsService {
	
	@Autowired
	private BizStatsEntryRepository repo;
	
	@Autowired
	private CoinService coinService;
	
	public Page<BizStatsDTO> getAll(Pageable pageable){
		Page<BizStatsEntry> entries = repo.findAll(pageable);
		Page<BizStatsDTO> dtoPage = entries.map(new Converter<BizStatsEntry, BizStatsDTO>() {
		    @Override
		    public BizStatsDTO convert(BizStatsEntry entity) {
		    	
		    		return new BizStatsDTO(entity);
		        
		    }
		});
		
		return dtoPage;
	}
	
	public Page<BizStatsDTO> getAllByCoin(String coinId, Date date1, Date date2,  Pageable pageable) throws ServiceException, NotFoundException{
		if(coinId == null){
			return this.getAll(pageable);
		}
		
		CryptoCoin coin = coinService.getById(coinId);
		Page<BizStatsEntry> entries;
		if(date1 != null && date2 != null){
			entries = repo.findAllByCoinAndDateBetween(coin, date1, date2, pageable);
		} else {
			entries = repo.findAllByCoin(coin, pageable);
		}
		
		Page<BizStatsDTO> dtoPage = entries.map(new Converter<BizStatsEntry, BizStatsDTO>() {
		    @Override
		    public BizStatsDTO convert(BizStatsEntry entity) {
		    	
		    		return new BizStatsDTO(entity);
		        
		    }
		});
		
		return dtoPage;
	}
	
	public List<BizStatsDTO> getTop10ByScore(){
		List<BizStatsEntry> entryList = repo.getCurrentTopByScore(new PageRequest(0,10));
		List<BizStatsDTO> dtoList = new ArrayList<BizStatsDTO>();
		for(BizStatsEntry entry: entryList){
			dtoList.add(new BizStatsDTO(entry));
		}
		return dtoList;
	}
	

}
