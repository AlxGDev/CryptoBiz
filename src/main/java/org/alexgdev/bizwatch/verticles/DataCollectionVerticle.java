package org.alexgdev.bizwatch.verticles;



import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alexgdev.bizwatch.dto.CoinMarketCapDTO;
import org.alexgdev.bizwatch.dto.PageEntryDTO;
import org.alexgdev.bizwatch.dto.PostDTO;
import org.alexgdev.bizwatch.dto.PostStatsDTO;
import org.alexgdev.bizwatch.dto.ThreadDTO;
import org.alexgdev.bizwatch.entities.BizStatsEntry;
import org.alexgdev.bizwatch.entities.CryptoCoin;
import org.alexgdev.bizwatch.service.IBizStatsService;
import org.alexgdev.bizwatch.service.ICoinService;
import org.alexgdev.bizwatch.service.JDBCBizStatsService;
import org.alexgdev.bizwatch.service.JDBCCoinService;
import org.alexgdev.bizwatch.service.OpenNLPService;
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
import io.vertx.ext.web.RoutingContext;

@Component
public class DataCollectionVerticle extends AbstractVerticle{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DataCollectionVerticle.class);
	private static final int RETRIES = 3;
	private static final int THREADRETRIES = 3;
	private ScraperService scraper;
	List<String> commonWords = Arrays.asList(" time ", " moon ", " pay ", " game ", " part ");
	
	@Autowired
	private OpenNLPService nlpService;
	
	private ICoinService coinService;
	private IBizStatsService statsService;
	
	private Queue<PageEntryDTO> threadsToProcess;
	private Long threadProcessTimer;
	private Integer processedCount = 0;
	private Queue<CryptoCoin> coinsToProcess;
	
	private Instant processingStart;
	private Long scheduleTimer;
	private int lastHourProcessed = 0;
	//how often 
	private int getDataRetries = 3;
    
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
	    coinsToProcess = new ArrayDeque<CryptoCoin>();
	    
	    //collect data hourly
	    scheduleTimer = vertx.setPeriodic(10000, i -> {
	    	LocalDateTime t = LocalDateTime.now();
	    	if(t.getHourOfDay() != lastHourProcessed){ //
	    		
	    		lastHourProcessed = LocalDateTime.now().getHourOfDay();
	    		LOGGER.info("Processing Hour: "+lastHourProcessed);
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
    
    private void collectBizData(){
		LOGGER.info("Starting to get Data");
		
		scraper.getBizThreads().setHandler(resultHandlerPassThrough( pageEntries -> {
			if (pageEntries == null && getDataRetries > 0) {
	  	    	 LOGGER.error("Could not retrieve catalog, retrying");
	  	    	getDataRetries--;
		  	    long timer =   vertx.setTimer(2000, id -> {
		  	    	collectBizData();
		  	     });
	  	      } else if (pageEntries == null) {
		  	    	LOGGER.error("Could not retrieve catalog, stopping");
		  	    	getDataRetries = RETRIES; 	    
			  } else {
	  	    	  //LOGGER.info("Retrieved Page Entries: "+pageEntries.size());
				 getDataRetries = RETRIES;
	  	    	 List<PostDTO> posts = new ArrayList<PostDTO>();
	  	    	 for(PageEntryDTO e : pageEntries){
	  	    		 e.setRetries(THREADRETRIES);
	  	    		 threadsToProcess.add(e);
	  	    		 
	  	    	 }

	  	    	 threadProcessTimer =   vertx.setTimer(1000, id -> {
	  	    		 processThread(posts, pageEntries);
	  	    	 });
	  	      }
	        }));
    }
    
    private void processThread(List<PostDTO> posts, List<PageEntryDTO> pageEntries){
    	if(threadsToProcess.isEmpty()){
    		processedCount = 0;
    		processingStart = Instant.now();
    		LOGGER.info("Processing finished: "+ posts.size());
    		processData_Step1(pageEntries, posts);
    		
    	} else {
    		PageEntryDTO t = threadsToProcess.poll();
    		scraper.getBizThread(t.getNo()).setHandler(resultHandlerPassThrough( thread -> {
    			  
	    		  if (thread == null && t.getRetries()>0) {
		  	    	 LOGGER.error("Could not retrieve thread, retrying");
		  	    	 t.setRetries(t.getRetries()-1);
		  	    	 threadsToProcess.add(t);
		  	      } else if (thread == null) {
		  	    	processedCount++;
			  	    	 LOGGER.error("Could not retrieve thread");
			  	  } else {
			  		processedCount++;
		  	    	//LOGGER.info("Retrieved Posts: "+processedCount);
		  	    	 for(PostDTO post : thread.getPosts()){
		  	    		  posts.add(post);	  	    		  
		  	    	 }
		  	    	
		  	      }
	    		  threadProcessTimer =   vertx.setTimer(1000, id -> {
		  	    		 processThread(posts, pageEntries);
		  	      });
		        }));
    	}
    	
    }
		
	
    //get coinmarketcap data and insert new coins into db, then process coin + threads + posts
    private void processData_Step1(List<PageEntryDTO> threads, List<PostDTO> posts){
    	
    	scraper.getCoinData(200).setHandler(resultHandlerPassThrough( coins -> {
	 	      if (coins == null && getDataRetries > 0) {
	  	    	 LOGGER.error("Could not retrieve coin data, retrying");
	  	    	 getDataRetries--;
		  	    long timer =   vertx.setTimer(2000, id -> {
		  	    	processData_Step1(threads, posts);
		  	     });
	  	      } else if (coins == null) {
		  	    	LOGGER.error("Could not retrieve coin data!");
		  	    	 getDataRetries = RETRIES; 	    
			  } else {
				  getDataRetries = RETRIES;
	  	    	  for(CoinMarketCapDTO coinDTO : coins){
	  	    		  coinService.getCertain(coinDTO.getId()).setHandler(resultHandler( optionalCoin -> {
		  	  	 	      if (!optionalCoin.isPresent()) {
		  	  	 	    	CryptoCoin coin = new CryptoCoin(coinDTO);
		  	  	 	    	coinService.insert(coin).setHandler(resultHandler( success -> {
		  	  	  	 	      if (success) {
		  	  		  	    	 LOGGER.info("Created coin: "+coin.getId());
		  	  		  	    	 processData_Step2(threads, posts, coin, coinDTO);
		  	  		  	    	 
		  	  		  	      } else {
		  	  		  	    	  LOGGER.info("Could not create coin");
		  	  		  	    	
		  	  		  	      }
		  	  		        }));
		  		  	    	 
		  		  	      } else {
		  		  	    	processData_Step2(threads, posts, optionalCoin.get(), coinDTO);
		  		  	    	 
		  		  	      }
	  	    		  }));
	  	    	  }

	  	      }
	        }));
    }
	private void processData_Step2(List<PageEntryDTO> threads, List<PostDTO> posts, CryptoCoin coin, CoinMarketCapDTO coindto){
		int threadCount = this.getThreadOccurences(threads, coin);
		PostStatsDTO postStats;postStats = this.getPostStats(posts, coin);
		BizStatsEntry statEntry;
		statEntry = new BizStatsEntry(coindto);
		statEntry.setDate(processingStart);
		statEntry.setCoin(coin);
		if(threadCount > 0 || postStats.getCountMentions() > 0){
			
			statEntry.setNrThreads(threadCount);
			statEntry.setNrPosts(postStats.getCountMentions());
			statEntry.setAverageSentiment(postStats.getAverageScore());
			statEntry.setNegativeMentions(postStats.getClassifiedNegative());
			statEntry.setPositiveMentions(postStats.getClassifiedPositive());
			if(postStats.getTop5words().length()>254){
				statEntry.setTop5words(postStats.getTop5words().substring(0, 254));
			} else {
				statEntry.setTop5words(postStats.getTop5words());
			}

		}
		statsService.insert(statEntry).setHandler(resultHandler( success -> {
	  	 	      if (success) {
		  	    	 LOGGER.info("Created statEntry: "+statEntry.getCoin());
		  	    	 
		  	    	 
		  	      } else {
		  	    	  LOGGER.info("Could not create statEntry");
		  	    	
		  	      }
		}));
	}
    
    private int getThreadOccurences(List<PageEntryDTO> threads, CryptoCoin coin){
		int threadCount = 0;
		String title = "";
		String content = "";
		String term1 = " "+coin.getName().toLowerCase()+" ";
		String term2 = " "+coin.getSymbol().toLowerCase()+" ";
		for(PageEntryDTO thread : threads){
			
			title = thread.getSub().toLowerCase();
			content = thread.getCom().toLowerCase();
			term1 = " "+coin.getName().toLowerCase()+" ";
			term2 = " "+coin.getSymbol().toLowerCase()+" ";
			if(title.contains(term1)){
				threadCount++;
			} else if(title.contains(term2) && !commonWords.contains(term2)){
				threadCount++;
			} else if(content.contains(term1)){
				threadCount++;
			} else if(content.contains(term2) && !commonWords.contains(term2)){
				threadCount++;
			}
			
		}
		
		return threadCount;
		
	}
	
	private int getPostOccurences(List<PostDTO> posts, CryptoCoin coin){
		int postCount = 0;
		String content = "";
		String term1 = " "+coin.getName().toLowerCase()+" ";
		String term2 = " "+coin.getSymbol().toLowerCase()+" ";
		for(PostDTO post : posts){
			
			
			content = post.getCom().toLowerCase();
			if((content.contains(term1) || content.contains(term2)) && !commonWords.contains(term2)){
				postCount++;
			} 
			
		}
		
		return postCount;
		
	}
	
	private PostStatsDTO getPostStats(List<PostDTO> posts, CryptoCoin coin){
		int postCount = 0;
		int positive = 0;
		int negative = 0;
		double average = 0;
		String content = "";
		String term1 = " "+coin.getName().toLowerCase()+" ";
		String term2 = " "+coin.getSymbol().toLowerCase()+" ";
		double postAvg;
		double[] classificationResult;
		
		HashMap<String, Integer> wordCount = new HashMap<String, Integer>();
		String[] sentences;
		String[] tokens;
		List<String> wordStems;
		for(PostDTO post : posts){
						
			content = Jsoup.parse(post.getCom()).text().toLowerCase();
			
			if((content.contains(term1) || content.contains(term2)) && !commonWords.contains(term2)){
				postCount++;
				
				sentences = nlpService.getSentences(content);
				postAvg = 0;
				wordStems = new ArrayList<String>();
				for(String sentence : sentences){
					tokens = nlpService.tokenize(sentence);
					//Sentiment Analysis
					classificationResult = nlpService.classifySentiment(tokens);
					if(classificationResult[0]>classificationResult[1]){
						positive += 1;
					} else {
						negative += 1;
					}
					postAvg = postAvg+(classificationResult[0]-classificationResult[1]);
					
					//get Word stems and add to list
					//wordStems = Stream.concat(wordStems.stream(), nlpService.stem(nlpService.removeStopWords(new ArrayList<String>(Arrays.asList(tokens)))).stream()).collect(Collectors.toList());
					wordStems = Stream.concat(wordStems.stream(), nlpService.removeStopWords(new ArrayList<String>(Arrays.asList(tokens))).stream()).collect(Collectors.toList());
					
					
					
				}
				postAvg = postAvg/(double)sentences.length;
				average+=postAvg;
				//distinct word stems
				wordStems = wordStems.stream().distinct().collect(Collectors.toList());
				for(String wordStem : wordStems){
					if(!wordStem.equals(term1.trim()) && !wordStem.equals(term2.trim())){
						if(wordCount.containsKey(wordStem)){
							wordCount.put(wordStem, wordCount.get(wordStem)+1);
						} else {
							wordCount.put(wordStem, 1);
						}
					}
					
				}
				
				
				
			} 
			
		}
		List<String> orderedWordStems = wordCount.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
		if(orderedWordStems.size()>5){
			orderedWordStems = orderedWordStems.subList(0, 5);
		}
		String top5 = String.join(", ", orderedWordStems);
               
		average = average /(double) postCount;
		if(commonWords.contains(term2)){
			postCount = (int) (postCount * 0.1);
		}
		return new PostStatsDTO(postCount, positive, negative, average, top5);
	}

}
