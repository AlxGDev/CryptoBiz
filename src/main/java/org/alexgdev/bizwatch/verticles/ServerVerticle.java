package org.alexgdev.bizwatch.verticles;

import java.util.Map;
import java.util.function.Consumer;

import org.alexgdev.bizwatch.dto.MessageDTO;
import org.alexgdev.bizwatch.dto.MessageType;
import org.alexgdev.bizwatch.service.IBizStatsService;
import org.alexgdev.bizwatch.service.ICoinService;
import org.alexgdev.bizwatch.service.JDBCBizStatsService;
import org.alexgdev.bizwatch.service.JDBCCoinService;
import org.springframework.stereotype.Component;



import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.Logger;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.FaviconHandler;
import io.vertx.ext.web.handler.StaticHandler;

@Component
public class ServerVerticle extends AbstractVerticle {
	
	private static final String API_RETRIEVE_ALL_COINS = "/api/coins";
	private static final String API_RETRIEVE_CERTAIN_COIN = "/api/coins/:id";
	private static final String API_RETRIEVE_ALL_STATS = "/api/bizstats";
	private static final String API_RETRIEVE_TOP10_STATS = "/api/bizstats/top10";

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerVerticle.class);
	
	private ICoinService coinService;
	private IBizStatsService statsService;
	
	private void initData() {
	    final String serviceType = config().getString("service.type", "jdbc");
	    LOGGER.info("Service Type: " + serviceType);
	    switch (serviceType) {
	      case "jdbc": 
	      default:
	    	  coinService = new JDBCCoinService(vertx, config());
	    	  statsService = new JDBCBizStatsService(vertx, config());
	    }
	    coinService.initData().setHandler(res -> {
	        if (res.failed()) {
	          LOGGER.error("Error Initalizing Coin Service!");
	          res.cause().printStackTrace();
	        } else {
	        	statsService.initData().setHandler(res2 -> {
	    	        if (res2.failed()) {
	    	          LOGGER.error("Error Initalizing Stats Service!");
	    	          res2.cause().printStackTrace();
	    	        }
	    	    });
	        }
	    });
	}

    @Override
    public void start() throws Exception {
        super.start();
        initData();
        Router router = Router.router(vertx);
        router.get(API_RETRIEVE_ALL_COINS).handler(this::handleGetAllCoins);
        router.get(API_RETRIEVE_CERTAIN_COIN).handler(this::handleGetCoin);
        router.get(API_RETRIEVE_ALL_STATS).handler(this::handleGetAllStats);
        router.get(API_RETRIEVE_TOP10_STATS).handler(this::handleGetTop10Stats);
        /*router.get("/api/coins")
            .handler(this::handleCoinGetAll);
        router.get("/api/bizstats")
        .handler(this::handleBizStatsGetAll);
        router.get("/api/bizstats/top10")
        .handler(this::handleBizStatsGetTop10); */
        router.route("/*").handler(StaticHandler.create("static").setCachingEnabled(false));
        router.route().handler(FaviconHandler.create("static/favicon.ico"));

        vertx.createHttpServer()
            .requestHandler(router::accept)
            .listen(config().getInteger("http.port", 8080));
    }
    
    private void sendError(int statusCode, HttpServerResponse response) {
        response.setStatusCode(statusCode).end();
    }

    private void notFound(RoutingContext context) {
        context.response().setStatusCode(404).end();
    }

    private void badRequest(RoutingContext context) {
        context.response().setStatusCode(400).end();
    }

    private void serviceUnavailable(RoutingContext context) {
        context.response().setStatusCode(503).end();
    }
    
    private <T> Handler<AsyncResult<T>> resultHandler(RoutingContext context, Consumer<T> consumer) {
        return res -> {
          if (res.succeeded()) {
            consumer.accept(res.result());
          } else {
        	LOGGER.error(res.cause());
        	res.cause().printStackTrace();
            sendError(500, context.response());
          }
        };
    }
    
    public void handleGetAllCoins(RoutingContext context){
    	JsonObject params = new JsonObject();
    	for (Map.Entry<String, String> entry : context.request().params().entries()) {
    	    params.put(entry.getKey(), entry.getValue());
    	}

    	coinService.getAll(params).setHandler(resultHandler(context, res -> {
    	      if (res == null) {
    	    	LOGGER.error("result is null");
    	        serviceUnavailable(context);
    	      } else {
    	    	MessageDTO msg = new MessageDTO();
    	    	msg.setType(MessageType.SUCCESS);
    	    	msg.setData(res);
    	        final String encoded = Json.encodePrettily(msg);
    	        context.response()
    	          .putHeader("content-type", "application/json")
    	          .end(encoded);
    	      }
    	}));
	}
    
    private void handleGetCoin(RoutingContext context) {
        String todoID = context.request().getParam("id");
        if (todoID == null) {
          sendError(400, context.response());
          return;
        }

        coinService.getCertain(todoID).setHandler(resultHandler(context, res -> {
          if (!res.isPresent())
            notFound(context);
          else {
        	MessageDTO msg = new MessageDTO();
  	    	msg.setType(MessageType.SUCCESS);
  	    	msg.setData(res.get());
  	        final String encoded = Json.encodePrettily(msg);
            context.response()
              .putHeader("content-type", "application/json")
              .end(encoded);
          }
        }));
    }
    
    public void handleGetAllStats(RoutingContext context){
    	JsonObject params = new JsonObject();
    	for (Map.Entry<String, String> entry : context.request().params().entries()) {
    	    params.put(entry.getKey(), entry.getValue());
    	}

    	statsService.getAll(params).setHandler(resultHandler(context, res -> {
    	      if (res == null) {
    	    	LOGGER.error("result is null");
    	        serviceUnavailable(context);
    	      } else {
    	    	MessageDTO msg = new MessageDTO();
      	    	msg.setType(MessageType.SUCCESS);
      	    	msg.setData(res);
      	        final String encoded = Json.encodePrettily(msg);
    	        context.response()
    	          .putHeader("content-type", "application/json")
    	          .end(encoded);
    	      }
    	}));
	}
    
    public void handleGetTop10Stats(RoutingContext context){
    	statsService.getTop10().setHandler(resultHandler(context, res -> {
    	      if (res == null) {
    	    	  LOGGER.error("result is null");  
    	        serviceUnavailable(context);
    	      } else {
    	    	MessageDTO msg = new MessageDTO();
      	    	msg.setType(MessageType.SUCCESS);
      	    	msg.setData(res);
      	        final String encoded = Json.encodePrettily(msg);
    	        context.response()
    	          .putHeader("content-type", "application/json")
    	          .end(encoded);
    	      }
    	}));
	}
    
    
    

}