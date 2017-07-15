package org.alexgdev.bizwatch.verticles;

import org.alexgdev.bizwatch.exception.NotFoundException;
import org.alexgdev.bizwatch.exception.ServiceException;
import org.alexgdev.bizwatch.service.CoinService;
import org.alexgdev.bizwatch.util.RestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

@Component
public class CoinVerticle extends AbstractVerticle{
	public static final String GET_ALL_COINS = "get.coins.all";

    private final ObjectMapper mapper = Json.mapper;

    @Autowired
    private CoinService coinService;

    @Override
    public void start() throws Exception {
        super.start();
        vertx.eventBus()
                .<String>consumer(GET_ALL_COINS)
                .handler(getAllCoins(coinService));
    }

    private Handler<Message<String>> getAllCoins(CoinService service) {
        return msg -> vertx.<String>executeBlocking(future -> {
            try {
            	JsonObject params = new JsonObject(msg.body());
            	Pageable page = RestUtil.getPageable(params, RestUtil.getSort(params));
                future.complete(mapper.writeValueAsString(service.getAll(page)));
            } catch (JsonProcessingException e) {
                System.out.println("Failed to serialize result");
                future.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                msg.reply(result.result());
            } else {
            	if(result.cause() instanceof NotFoundException){
            		msg.fail(404, result.cause().getMessage());
            	} else if(result.cause() instanceof ServiceException){
            		msg.fail(400, result.cause().getMessage());
            	} else {
            		msg.fail(500, result.cause().getMessage());
            	}
            }
        });
    }
}
