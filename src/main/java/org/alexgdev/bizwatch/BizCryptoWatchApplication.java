package org.alexgdev.bizwatch;




import org.alexgdev.bizwatch.service.DataCollectionService;
import org.alexgdev.bizwatch.verticles.BizStatsVerticle;
import org.alexgdev.bizwatch.verticles.CoinVerticle;
import org.alexgdev.bizwatch.verticles.ServerVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import io.vertx.core.Vertx;

@SpringBootApplication
@EnableConfigurationProperties
public class BizCryptoWatchApplication implements CommandLineRunner{
	
	
	private static final Logger log = LoggerFactory.getLogger(BizCryptoWatchApplication.class);
	
	@Autowired
	private DataCollectionService dataService;
	
	@Autowired
	private ServerVerticle serverVerticle;
	
	@Autowired
	private CoinVerticle coinVerticle;
	
	@Autowired
	private BizStatsVerticle bizStatsVerticle;
	
	public static void main(String[] args) {
		
		SpringApplication springApplication;
		if(args[0].equals("crawler") || args[0].equals("server")){
			springApplication = new SpringApplicationBuilder()
             .sources(BizCryptoWatchApplication.class)
             .web(false)
             .build();
			springApplication.run(args); 
		} else {
			log.error("Invalid Argument. Start with server or crawler");
			System.exit(1);
		}

	}
	
	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder.build();
	}
	
	@Override
	public void run(String... args) throws Exception {
		if(args[0].equals("crawler")){
			dataService.collectBizCoinData();
		} else {
			final Vertx vertx = Vertx.vertx();
	        vertx.deployVerticle(serverVerticle);
	        vertx.deployVerticle(coinVerticle);
	        vertx.deployVerticle(bizStatsVerticle);
		}
		
	}
}
