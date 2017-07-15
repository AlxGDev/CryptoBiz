package org.alexgdev.bizwatch.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.stemmer.PorterStemmer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;


@Service
public class OpenNLPService {
	
	private DocumentCategorizerME classificationME;
	private SentenceDetectorME sentenceDetectorME;
	private TokenizerME tokenizerME;
	private PorterStemmer stemmer;
	private List<String> stopWords;
	
	@PostConstruct
	public void init(){
		try {
		stemmer = new PorterStemmer();
		InputStream inputStream = OpenNLPService.class.getResourceAsStream("/models/opennlp_twitter.bin");
		
		classificationME = new DocumentCategorizerME(new DoccatModel(inputStream));
		
		inputStream = OpenNLPService.class.getResourceAsStream("/models/en-sent.bin");
		sentenceDetectorME = new SentenceDetectorME(new SentenceModel(inputStream));
		
		inputStream = OpenNLPService.class.getResourceAsStream("/models/en-token.bin");
		tokenizerME = new TokenizerME(new TokenizerModel(inputStream));
		inputStream.close();
			try (Stream<String> lines = new BufferedReader(new InputStreamReader(OpenNLPService.class.getResourceAsStream("/models/stopwords-english.txt"))).lines()) {
			    stopWords = lines.collect(Collectors.toList());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public double[] classifySentiment(String[] text){
		
		return classificationME.categorize(text);
		
		/*
		 * result[0] average
		 * result[1] count positive
		 *result[2] count negative
		 
		double[] result = new double[]{0,0,0};
		
		
		for(String sentence : sentences){
			
			if(prob[0]>prob[1]){
				result[1] += 1;
			} else {
				result[2] += 1;
			}
			result[0] = result[0]+(prob[0]-prob[1]);
		}
		result[0] = result[0]/(double)sentences.length;
		
		return result; */
		
		
	}
	
	public List<String> stem(List<String> toStem) {
		
		for(int i = 0;i < toStem.size(); i++){
			toStem.set(i, stemmer.stem(toStem.get(i)));
		}
		return toStem;
		
	}
	
	public String[] getSentences (String text){
		return sentenceDetectorME.sentDetect(text);
	}
	
	public String[] tokenize(String sentence){
		return tokenizerME.tokenize(sentence);
	}
	
	public List<String> removeStopWords(List<String> tokens){
		
		
		tokens.removeAll(stopWords);
		return tokens;
		
	}
	


}
