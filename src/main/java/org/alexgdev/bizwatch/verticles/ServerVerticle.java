package org.alexgdev.bizwatch.verticles;

import java.util.Map;

import org.alexgdev.bizwatch.dto.MessageDTO;
import org.alexgdev.bizwatch.dto.MessageType;
import org.springframework.stereotype.Component;



import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.ReplyException;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.FaviconHandler;
import io.vertx.ext.web.handler.StaticHandler;

@Component
public class ServerVerticle extends AbstractVerticle {
	

    @Override
    public void start() throws Exception {
        super.start();

        Router router = Router.router(vertx);
        router.get("/api/coins")
            .handler(this::handleCoinGetAll);
        router.get("/api/bizstats")
        .handler(this::handleBizStatsGetAll);
        router.get("/api/bizstats/top10")
        .handler(this::handleBizStatsGetTop10);
        router.route("/*").handler(StaticHandler.create("static").setCachingEnabled(false));
        router.route().handler(FaviconHandler.create("static/favicon.ico"));

        vertx.createHttpServer()
            .requestHandler(router::accept)
            .listen(config().getInteger("http.port", 8080));
    }
    
    public void handleCoinGetAll(RoutingContext context){
    	JsonObject params = new JsonObject();
    	for (Map.Entry<String, String> entry : context.request().params().entries()) {
    	    params.put(entry.getKey(), entry.getValue());
    	}

		vertx.eventBus()
        .<String>send(CoinVerticle.GET_ALL_COINS, params.encodePrettily(), result -> {
            if (result.succeeded()) {
                context.response()
                    .putHeader("content-type", "application/json")
                    .setStatusCode(200)
                    .end(result.result()
                    .body());
            } else {
            	ReplyException cause = (ReplyException) result.cause();
                MessageDTO dto = new MessageDTO(MessageType.ERROR, cause.getMessage(), null);
                
                context.response()
                	.putHeader("content-type", "application/json")
                    .setStatusCode(cause.failureCode())
                    .end(JsonObject.mapFrom(dto).encodePrettily());
            }
        });
	}
    
    public void handleBizStatsGetAll(RoutingContext context){
    	JsonObject params = new JsonObject();
    	for (Map.Entry<String, String> entry : context.request().params().entries()) {
    	    params.put(entry.getKey(), entry.getValue());
    	}

		vertx.eventBus()
        .<String>send(BizStatsVerticle.GET_ALL_BIZSTATS, params.encodePrettily(), result -> {
            if (result.succeeded()) {
                context.response()
                    .putHeader("content-type", "application/json")
                    .setStatusCode(200)
                    .end(result.result()
                    .body());
            } else {
            	ReplyException cause = (ReplyException) result.cause();
                MessageDTO dto = new MessageDTO(MessageType.ERROR, cause.getMessage(), null);
                
                context.response()
                	.putHeader("content-type", "application/json")
                    .setStatusCode(cause.failureCode())
                    .end(JsonObject.mapFrom(dto).encodePrettily());
            }
        });
	}
    
    public void handleBizStatsGetTop10(RoutingContext context){
    	
		vertx.eventBus()
        .<String>send(BizStatsVerticle.GET_TOP10_BIZSTATS, "", result -> {
            if (result.succeeded()) {
                
            	context.response()
                    .putHeader("content-type", "application/json")
                    .setStatusCode(200)
                    .end(result.result()
                    .body());
            } else {
            	ReplyException cause = (ReplyException) result.cause();
                MessageDTO dto = new MessageDTO(MessageType.ERROR, cause.getMessage(), null);
                
                context.response()
                	.putHeader("content-type", "application/json")
                    .setStatusCode(cause.failureCode())
                    .end(JsonObject.mapFrom(dto).encodePrettily());
            }
        });
	}
    
    

}