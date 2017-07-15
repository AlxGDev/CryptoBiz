package org.alexgdev.bizwatch.service;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.alexgdev.bizwatch.dto.CoinMarketCapDTO;
import org.alexgdev.bizwatch.dto.ThreadDTO;
import org.alexgdev.bizwatch.dto.PageDTO;
import org.alexgdev.bizwatch.dto.PageEntryDTO;
import org.alexgdev.bizwatch.dto.PostDTO;
import org.alexgdev.bizwatch.exception.ServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class ScraperService {
	
	private RestTemplate restTemplate;
	
	@PostConstruct
	public void init(){
		restTemplate = new RestTemplate();
	}
	
	public List<PageDTO> getBizCatalog() throws ServiceException{
		
		String url = "http://a.4cdn.org/biz/catalog.json";
		try{
			
			PageDTO[] catalog = restTemplate.getForObject(url, PageDTO[].class);
			return Arrays.asList(catalog);
		} catch (HttpClientErrorException ex)   {
		    if (ex.getStatusCode() != HttpStatus.NOT_FOUND) {
		        throw new ServiceException(ex.getMessage());
		    }
		    return new ArrayList<PageDTO>();
		}
		
	}
	
	public List<PageEntryDTO> getBizThreads() throws ServiceException{
		List<List<PageEntryDTO>> cs = new ArrayList<List<PageEntryDTO>>();
		List<PageDTO> catalog = this.getBizCatalog();
		for(PageDTO dto: catalog){
			cs.add(dto.getThreads());
		}
		return cs.stream().flatMap(Collection::stream).collect(Collectors.toList());
	}
	
	public List<CoinMarketCapDTO> getCoinData(int limit) throws ServiceException{
		
		String url = "https://api.coinmarketcap.com/v1/ticker/";
		if(limit > 0){
			url+="?limit="+limit;
		}
		
		try{
			CoinMarketCapDTO[] coindata = restTemplate.getForObject(url, CoinMarketCapDTO[].class);
			return Arrays.asList(coindata);
		} catch (HttpClientErrorException ex)   {
		    if (ex.getStatusCode() != HttpStatus.NOT_FOUND) {
		        throw new ServiceException(ex.getMessage());
		    }
		    return new ArrayList<CoinMarketCapDTO>();
		}
		
	}
	
	public ThreadDTO getBizThread(Long no) throws ServiceException{
		
		String url = "http://a.4cdn.org/biz/thread/"+no+".json";
		try{
			ThreadDTO thread = restTemplate.getForObject(url, ThreadDTO.class);
			return thread;
		} catch (HttpClientErrorException ex)   {
		    if (ex.getStatusCode() != HttpStatus.NOT_FOUND) {
		        throw new ServiceException(ex.getMessage());
		    }
		    return null;
		}
		
	}
	
	public List<PostDTO> getBizPosts() throws ServiceException{
		List<List<PostDTO>> cs = new ArrayList<List<PostDTO>>();
		List<PageEntryDTO> catalog = this.getBizThreads();
		for(PageEntryDTO catthread : catalog){
			ThreadDTO thread = this.getBizThread(catthread.getNo());
			if(thread != null){
				cs.add(thread.getPosts());
			}
			try {
				//API limit
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return cs.stream().flatMap(Collection::stream).collect(Collectors.toList());
	}
	

}
