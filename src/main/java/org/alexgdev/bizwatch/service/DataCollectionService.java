package org.alexgdev.bizwatch.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alexgdev.bizwatch.dto.CoinMarketCapDTO;
import org.alexgdev.bizwatch.dto.PageEntryDTO;
import org.alexgdev.bizwatch.dto.PostDTO;
import org.alexgdev.bizwatch.dto.PostStatsDTO;
import org.alexgdev.bizwatch.entities.BizStatsEntry;
import org.alexgdev.bizwatch.entities.BizStatsEntryRepository;
import org.alexgdev.bizwatch.entities.CryptoCoin;
import org.alexgdev.bizwatch.entities.CryptoCoinRepository;
import org.alexgdev.bizwatch.exception.ServiceException;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataCollectionService {
	
	private static final Logger log = LoggerFactory.getLogger(DataCollectionService.class);
	
	@Autowired 
	OpenNLPService nlpService;
	
	@Autowired
	private ScraperService scraper;

	@Autowired
	private CryptoCoinRepository coinrepo;
	
	@Autowired
	private BizStatsEntryRepository bizrepo;
	
	
	List<String> commonWords = Arrays.asList(" time ", " moon ", " pay ", " game ");
	
	
	public void collectBizCoinData(){
		log.info("Starting to get Data");
		long start = System.currentTimeMillis();
		Date currentDate = new Date();
		List<PageEntryDTO> threads = new ArrayList<PageEntryDTO>();
		List<PostDTO> posts = new ArrayList<PostDTO>();
		List<CoinMarketCapDTO> coins = new ArrayList<CoinMarketCapDTO>();
		PostStatsDTO postStats;
		try {
			threads = scraper.getBizThreads();
			posts = scraper.getBizPosts();
			coins = scraper.getCoinData(200);
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		int threadCount = 0;
		CryptoCoin coin;
		BizStatsEntry statEntry;
		for(CoinMarketCapDTO coindto : coins){
			coin = coinrepo.findOne(coindto.getId());
			if(coin == null){
				coin = new CryptoCoin(coindto);
				coinrepo.save(coin);
				log.info("Saved coin "+coin.getName()+" to db");
			}
			threadCount = this.getThreadOccurences(threads, coin);
			postStats = this.getPostStats(posts, coin);
			
			statEntry = new BizStatsEntry(coindto);
			statEntry.setDate(currentDate);
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
			bizrepo.save(statEntry);
			
			log.info("Finished with coin: "+coindto.getName());
			coindto = null;
			coin = null;
			statEntry = null;
			postStats = null;
		}
		long end = (System.currentTimeMillis()-start) /1000;
		posts = null;
		coins = null;
		threads = null;
		log.info("Finished getting Data, time taken: "+end);
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
