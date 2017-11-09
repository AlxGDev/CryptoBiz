package org.alexgdev.bizwatch.verticles;



import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import java.util.Queue;
import java.util.function.Consumer;
import java.util.stream.Collectors;


import org.alexgdev.bizwatch.dto.CoinMarketCapDTO;
import org.alexgdev.bizwatch.dto.PageEntryDTO;
import org.alexgdev.bizwatch.dto.PostDTO;

import org.alexgdev.bizwatch.entities.BizStatsEntry;
import org.alexgdev.bizwatch.entities.CryptoCoin;
import org.alexgdev.bizwatch.service.DataProcessingService;
import org.alexgdev.bizwatch.service.IBizStatsService;
import org.alexgdev.bizwatch.service.ICoinService;
import org.alexgdev.bizwatch.service.JDBCBizStatsService;
import org.alexgdev.bizwatch.service.JDBCCoinService;
import org.alexgdev.bizwatch.service.ScraperService;

import org.joda.time.LocalDateTime;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;


@Component
public class DataCollectionVerticle extends AbstractVerticle{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DataCollectionVerticle.class);
	private static final int RETRIES = 3;
	private static final int THREADRETRIES = 3;
	private ScraperService scraper;
	
	
	@Autowired
	private DataProcessingService processingService;
	
	private ICoinService coinService;
	private IBizStatsService statsService;
	
	private Queue<PageEntryDTO> threadsToProcess;
	private Long threadProcessTimer;

	private Long scheduleTimer;
	private int lastHourProcessed = -1;
	//how often retry
	private int bizstatsRetries;
	private int microCapRetries;
	
	private boolean processingThreads;
	
	private List<CoinMarketCapDTO> interestingMicroCaps;
	private Queue<CoinMarketCapDTO> microcapsToProcess;
    
	@Override
    public void start() throws Exception {
        super.start();
       scraper = new ScraperService(vertx);
       final String serviceType = config().getString("service.type", "jdbc");
	    LOGGER.info("Service Type: " + serviceType);
	    switch (serviceType) {
	      case "jdbc": 
	      default:
	    	  coinService = new JDBCCoinService(vertx, config());
	    	  statsService = new JDBCBizStatsService(vertx, config());
	    }
	    
	    threadsToProcess = new ArrayDeque<PageEntryDTO>();
	    processingThreads = false;
	    bizstatsRetries = RETRIES;
	    microCapRetries = RETRIES;
	    interestingMicroCaps = new ArrayList<CoinMarketCapDTO>();
	    microcapsToProcess = new ArrayDeque<CoinMarketCapDTO>();
	    
	    vertx.eventBus()
	    	.<String>consumer(MessageEndpoints.GETMICROCAPS)
	    	.handler(getMicrocaps());
	    
	    //collect data hourly
	    scheduleTimer = vertx.setPeriodic(10000, i -> {
	    	LocalDateTime t = LocalDateTime.now();
	    	if(t.getHourOfDay() != lastHourProcessed && !processingThreads){ //
	    		
	    		lastHourProcessed = LocalDateTime.now().getHourOfDay();
	    		LOGGER.info("Processing Hour: "+lastHourProcessed);
	    		collectMicrocaps();
	    		collectBizData();
	    		
	    	}
	    });
 
    }
    
    private <T> Handler<AsyncResult<T>> resultHandler(Consumer<T> consumer) {
        return res -> {
          if (res.succeeded()) {
            consumer.accept(res.result());
          } else {
        	  LOGGER.error(res.cause());
        	  res.cause().printStackTrace();
            
          }
        };
    }
    /*
     * Passes the result on to consumer, even if there was an error
     */
    private <T> Handler<AsyncResult<T>> resultHandlerPassThrough(Consumer<T> consumer) {
        return res -> {
          if (res.succeeded()) {
            consumer.accept(res.result());
          } else {

        	  LOGGER.error(res.cause());
        	  //res.cause().printStackTrace();
        	  consumer.accept(null);
            
          }
        };
    }
    
    private void collectMicrocaps(){
    	scraper.getCoinMarketCapData(0).setHandler(resultHandlerPassThrough( coins -> {
	 	      if (coins == null && microCapRetries > 0) {
	  	    	 LOGGER.error("Could not retrieve microcap data, retrying");
	  	    	 microCapRetries--;
	  	    	 long timer =   vertx.setTimer(2000, id -> {
		  	    	collectMicrocaps();
		  	     });
	  	      } else if (coins == null) {
	  	    	  microCapRetries = RETRIES;
	  	    	  LOGGER.error("Could not retrieve microcaps!");	    
			  } else {
				  microCapRetries = RETRIES;
				  interestingMicroCaps = coins.stream().filter(dto -> dto.getMarket_cap_usd() != null && dto.getMarket_cap_usd()<=2000000)
										 .filter(dto -> dto.getTotal_supply() != null && dto.getTotal_supply()<= 50000000) 
										 .filter(dto -> dto.getVolume() != null 
													  		&& dto.getMarket_cap_usd() > 0 
													  		&& (dto.getVolume()/dto.getMarket_cap_usd()) >= 0.02) 
										 .filter(dto -> dto.getAvailable_supply() != null 
													  		&& dto.getTotal_supply() > 0 
													  		&& (dto.getAvailable_supply()/dto.getTotal_supply()) >= 0.85) 
										 .map(dto -> {dto.setVolume_marketcap_ratio(Math.round((dto.getVolume()/dto.getMarket_cap_usd()) * 100.0) / 100.0);
													  			   microcapsToProcess.add(dto);
													  			   return dto;})
										 .collect(Collectors.toList());
				  
				  
				  LOGGER.info(interestingMicroCaps.size());
				  LOGGER.info("Finished getting microcaps, starting processing");
				  vertx.setTimer(1000, id -> {
		  	    		 processMicroCap();
				  });

	  	      }
	        }));
    }
    private void processMicroCap(){
    	if(microcapsToProcess.isEmpty()){
    		LOGGER.info("Finished processing microcaps");	
    	} else {
    		CoinMarketCapDTO t = microcapsToProcess.poll();
    		scraper.getAnnouncementPage(t.getId()).setHandler(resultHandlerPassThrough( url -> {
    			  
	    		  if (url == null) {
			  	    	 LOGGER.error("Could not retrieve announcement thread for " +t.getId());
			  	  } else {
		  	    	t.setAnnouncementUrl(url);
		  	    	
		  	      }
	    		  vertx.setTimer(1000, id -> {
	    			  processMicroCap();
		  	      });
		        }));
    	}
    	
    }
    
    private Handler<Message<String>> getMicrocaps(){
		return msg -> {
			
			msg.reply(Json.encodePrettily(interestingMicroCaps));
		};
	}
    
    
    private void collectBizData(){
		LOGGER.info("Starting to get Data");
		
		scraper.getBizThreads().setHandler(resultHandlerPassThrough( pageEntries -> {
			if (pageEntries == null && bizstatsRetries > 0) {
	  	    	 LOGGER.error("Could not retrieve catalog, retrying");
	  	    	 bizstatsRetries--;
	  	    	 long timer =   vertx.setTimer(2000, id -> {
		  	    	collectBizData();
		  	     });
	  	      } else if (pageEntries == null) {
	  	    	  bizstatsRetries = RETRIES;
	  	    	  LOGGER.error("Could not retrieve catalog, stopping");
		  	    	 	    
			  } else {
	  	    	  //LOGGER.info("Retrieved Page Entries: "+pageEntries.size());
				 bizstatsRetries = RETRIES;
				 processingThreads = true;
				 List<String> threads = new ArrayList<String>();
	  	    	 List<String> posts = new ArrayList<String>();
	  	    	 for(PageEntryDTO e : pageEntries){
	  	    		 e.setRetries(THREADRETRIES);
	  	    		 threadsToProcess.add(e);
	  	    		 threads.add((e.getSub()+" "+e.getCom()).toLowerCase());
	  	    	 }

	  	    	 threadProcessTimer =   vertx.setTimer(2000, id -> {
	  	    		 processThread(threads, posts);
	  	    	 });
	  	      }
	        }));
    }
    
    private void processThread(List<String> threads,List<String> posts){
    	if(threadsToProcess.isEmpty()){
    		processingThreads = false;
    		LOGGER.info("Data collection finished, starting processing: "+ posts.size());
    		processData_Step1(threads, posts);
    		
    	} else {
    		PageEntryDTO t = threadsToProcess.poll();
    		scraper.getBizThread(t.getNo()).setHandler(resultHandlerPassThrough( thread -> {
    			  
	    		  if (thread == null && t.getRetries()>0) {
		  	    	 LOGGER.error("Could not retrieve thread, retrying");
		  	    	 t.setRetries(t.getRetries()-1);
		  	    	 threadsToProcess.add(t);
		  	      } else if (thread == null) {
			  	    	 LOGGER.error("Could not retrieve thread");
			  	  } else {
		  	    	//LOGGER.info("Retrieved Posts: "+processedCount);
		  	    	 for(PostDTO post : thread.getPosts()){
		  	    		  posts.add(Jsoup.parse(post.getCom()).text().toLowerCase());	  	    		  
		  	    	 }
		  	    	
		  	      }
	    		  threadProcessTimer =   vertx.setTimer(2000, id -> {
		  	    		 processThread(threads, posts);
		  	      });
		        }));
    	}
    	
    }
		
	
    //get coinmarketcap data and insert new coins into db, then process coin + threads + posts
    private void processData_Step1(List<String> threads, List<String> posts){
    	Instant processingStart = Instant.now();
    	scraper.getCoinMarketCapData(200).setHandler(resultHandlerPassThrough( coins -> {
	 	      if (coins == null && bizstatsRetries > 0) {
	  	    	 LOGGER.error("Could not retrieve coin data, retrying");
	  	    	bizstatsRetries--;
		  	    long timer =   vertx.setTimer(2000, id -> {
		  	    	processData_Step1(threads, posts);
		  	     });
	  	      } else if (coins == null) {
		  	    	LOGGER.error("Could not retrieve coin data!");	
		  	    	bizstatsRetries = RETRIES;
			  } else {
				  bizstatsRetries = RETRIES;
	  	    	  for(CoinMarketCapDTO coinDTO : coins){
	  	    		  coinService.getCertain(coinDTO.getId()).setHandler(resultHandler( optionalCoin -> {
		  	  	 	      if (!optionalCoin.isPresent()) {
		  	  	 	    	CryptoCoin coin = new CryptoCoin(coinDTO);
		  	  	 	    	coinService.insert(coin).setHandler(resultHandler( success -> {
		  	  	  	 	      if (success) {
		  	  		  	    	 //LOGGER.info("Created coin: "+coin.getId());
		  	  		  	    	 processData_Step2(processingStart, threads, posts, coin, coinDTO);
		  	  		  	    	 
		  	  		  	      } else {
		  	  		  	    	  LOGGER.info("Could not create coin");
		  	  		  	    	
		  	  		  	      }
		  	  		        }));
		  		  	    	 
		  		  	      } else {
		  		  	    	processData_Step2(processingStart, threads, posts, optionalCoin.get(), coinDTO);
		  		  	    	 
		  		  	      }
	  	    		  }));
	  	    	  }

	  	      }
	        }));
    }
	private void processData_Step2(Instant processingStart, List<String> threads, List<String> posts, CryptoCoin coin, CoinMarketCapDTO coindto){
		
		BizStatsEntry statEntry = processingService.processData(processingStart, threads, posts, coin, coindto);
		
		statsService.insert(statEntry).setHandler(resultHandler( success -> {
	  	 	      if (success) {
		  	    	 //LOGGER.info("Created statEntry: "+statEntry.getCoin());
		  	    	 
		  	    	 
		  	      } else {
		  	    	  LOGGER.info("Could not create statEntry");
		  	    	
		  	      }
		}));
	}
    
    

}
