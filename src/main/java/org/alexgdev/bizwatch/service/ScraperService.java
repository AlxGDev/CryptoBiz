package org.alexgdev.bizwatch.service;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;



import org.alexgdev.bizwatch.dto.CoinMarketCapDTO;
import org.alexgdev.bizwatch.dto.ThreadDTO;

import org.alexgdev.bizwatch.dto.PageDTO;
import org.alexgdev.bizwatch.dto.PageEntryDTO;
import org.alexgdev.bizwatch.dto.PostDTO;


import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class ScraperService {
	
	private MultiMap headers = MultiMap.caseInsensitiveMultiMap();
	private WebClient restclient;
	
	public ScraperService(Vertx vertx){
		String userAgent = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36";
		WebClientOptions options = new WebClientOptions().setUserAgent(userAgent);
		headers.add("User-Agent", userAgent);
	    
		options.setKeepAlive(false);
		restclient = WebClient.create(vertx, options);
	}
	
	public Future<List<PageDTO>> getBizCatalog(){
		HttpRequest<Buffer> request =restclient
		.get(80, "a.4cdn.org", "/biz/catalog.json");
		Future<List<PageDTO>> result = Future.future();
		request.send(ar -> {
					  if (ar.succeeded()) {
						  HttpResponse<Buffer> response = ar.result();
						  if(response.statusCode() == 200){
							  List<PageDTO> pages = response.bodyAsJsonArray().stream()
									  	.filter(o -> o instanceof JsonObject)
									  	.map(o -> (JsonObject)o)
									  	.map(o -> o.mapTo(PageDTO.class))
							            .collect(Collectors.toList());;
							  
							  result.complete(pages);  
							  
						  } else {
							  result.fail("Status Code: "+response.statusCode());
						  }
						  	  
				      
					  } else {
						  result.fail(ar.cause());
					  }
		});
		return result;
		
	}
	
	public Future<List<PageEntryDTO>> getBizThreads(){
		List<List<PageEntryDTO>> cs = new ArrayList<List<PageEntryDTO>>();
		Future<List<PageEntryDTO>> result = Future.future();
		this.getBizCatalog().setHandler(res -> {
	        if (res.failed()) {
		          result.fail(res.cause());
		          //res.cause().printStackTrace();
		        } else {
		        	List<PageDTO> catalog = res.result();
		        	for(PageDTO dto: catalog){
		    			cs.add(dto.getThreads());
		    		}
		        	result.complete(cs.stream().flatMap(Collection::stream).collect(Collectors.toList()));
		        }
		});
		return result;
		
	
	}
	
	public Future<List<CoinMarketCapDTO>> getCoinData(int limit){
		
		String endpoint = "/v1/ticker/";
		if(limit > 0){
			endpoint+="?limit="+limit;
		}
		HttpRequest<Buffer> request =restclient.get(80, "api.coinmarketcap.com", endpoint);
		Future<List<CoinMarketCapDTO>> result = Future.future();
		request.send(ar -> {
			if (ar.succeeded()) {
				HttpResponse<Buffer> response = ar.result();
				if(response.statusCode() == 200){
					List<CoinMarketCapDTO> pages = response.bodyAsJsonArray().stream()
											  	.filter(o -> o instanceof JsonObject)
											  	.map(o -> (JsonObject)o)
											  	.map(o -> o.mapTo(CoinMarketCapDTO.class))
									            .collect(Collectors.toList());
									  
					result.complete(pages);  
					} else {
						result.fail("Status Code: "+response.statusCode());
					}
				} else {
					result.fail(ar.cause());
				}
		});
		return result;	
	}
	
	public Future<ThreadDTO> getBizThread(Long no){
		HttpRequest<Buffer> request =restclient.get(80, "a.4cdn.org", "/biz/thread/"+no+".json");
		Future<ThreadDTO> result = Future.future();
		request.send(ar -> {
			if (ar.succeeded()) {
				HttpResponse<Buffer> response = ar.result();
				if(response.statusCode() == 200){
					ThreadDTO threadDTO = response.bodyAsJsonObject().mapTo(ThreadDTO.class);
									  
					result.complete(threadDTO);  
					} else {
						result.fail("Status Code: "+response.statusCode());
					}
				} else {
					result.fail(ar.cause());
				}
		});
		return result;
		
	}
	
	
	/*public <List<PostDTO> getBizPosts() throws ServiceException{
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
	} */
	

}
