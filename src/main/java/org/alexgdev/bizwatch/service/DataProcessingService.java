package org.alexgdev.bizwatch.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alexgdev.bizwatch.dto.CoinMarketCapDTO;

import org.alexgdev.bizwatch.dto.PostStatsDTO;
import org.alexgdev.bizwatch.entities.BizStatsEntry;
import org.alexgdev.bizwatch.entities.CryptoCoin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

//takes a cryptocoin, content of threads and content of posts as input, extracts data from them and returns a bizstatsentry
@Service
public class DataProcessingService {
	@Autowired
	private OpenNLPService nlpService;
	
	List<String> commonWords = Arrays.asList(" time ", " moon ", " pay ", " game ", " part ");
	
	
	
	public BizStatsEntry processData(Instant processingStart, List<String> threads, List<String> posts, CryptoCoin coin, CoinMarketCapDTO coindto){
		int threadCount = this.getThreadOccurences(threads, coin);
		PostStatsDTO postStats = this.getPostStats(posts, coin);
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
		return statEntry;
	}
	
	private int getThreadOccurences(List<String> threads, CryptoCoin coin){
		int threadCount = 0;
		String term1 = " "+coin.getName().toLowerCase()+" ";
		String term2 = " "+coin.getSymbol().toLowerCase()+" ";
		for(String s : threads){
			
			//title = thread.getSub().toLowerCase();
			//content = thread.getCom().toLowerCase();
			term1 = " "+coin.getName().toLowerCase()+" ";
			term2 = " "+coin.getSymbol().toLowerCase()+" ";
			if(s.contains(term1)){
				threadCount++;
			} else if(s.contains(term2) && !commonWords.contains(term2)){
				threadCount++;
			}
			
		}
		
		return threadCount;
		
	}
	
	private int getPostOccurences(List<String> posts, CryptoCoin coin){
		int postCount = 0;
		String term1 = " "+coin.getName().toLowerCase()+" ";
		String term2 = " "+coin.getSymbol().toLowerCase()+" ";
		for(String s : posts){
			
			
			//content = post.getCom().toLowerCase();
			if((s.contains(term1) || s.contains(term2)) && !commonWords.contains(term2)){
				postCount++;
			} 
			
		}
		
		return postCount;
		
	}
	
	private PostStatsDTO getPostStats(List<String> posts, CryptoCoin coin){
		int postCount = 0;
		int positive = 0;
		int negative = 0;
		double average = 0;
		String term1 = " "+coin.getName().toLowerCase()+" ";
		String term2 = " "+coin.getSymbol().toLowerCase()+" ";
		double postAvg;
		double[] classificationResult;
		
		HashMap<String, Integer> wordCount = new HashMap<String, Integer>();
		String[] sentences;
		String[] tokens;
		List<String> wordStems;
		for(String s : posts){
						
			//content = Jsoup.parse(post.getCom()).text().toLowerCase();
			
			if((s.contains(term1) || s.contains(term2)) && !commonWords.contains(term2)){
				postCount++;
				
				sentences = nlpService.getSentences(s);
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
