package org.alexgdev.bizwatch;


import org.alexgdev.bizwatch.verticles.DataCollectionVerticle;
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

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@SpringBootApplication
@EnableConfigurationProperties
public class BizCryptoWatchApplication implements CommandLineRunner{
	
	
	private static final Logger log = LoggerFactory.getLogger(BizCryptoWatchApplication.class);
	
	
	@Autowired
	private ServerVerticle serverVerticle;
	
	@Autowired
	private DataCollectionVerticle coinVerticle;
	
	public static void main(String[] args) {
		
		SpringApplication springApplication = new SpringApplicationBuilder()
             .sources(BizCryptoWatchApplication.class)
             .web(false)
             .build();
			springApplication.run(args); 
		

	}
	
	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder.build();
	}
	
	@Override
	public void run(String... args) throws Exception {
			final Vertx vertx = Vertx.vertx();
			DeploymentOptions options = new DeploymentOptions();
			JsonObject config = new JsonObject().put("password", "").put("url", "jdbc:postgresql://localhost:5432/bizwatchdb").put("user", "postgres");
			options.setConfig(config);
	        vertx.deployVerticle(serverVerticle, options);
	        vertx.deployVerticle(coinVerticle, options);
		
		
	}
}
