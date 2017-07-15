package org.alexgdev.bizwatch.verticles;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.alexgdev.bizwatch.dto.BizStatsDTO;
import org.alexgdev.bizwatch.exception.NotFoundException;
import org.alexgdev.bizwatch.exception.ServiceException;
import org.alexgdev.bizwatch.service.BizStatsService;

import org.alexgdev.bizwatch.util.RestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
public class BizStatsVerticle extends AbstractVerticle{
	public static final String GET_TOP10_BIZSTATS = "get.bizstats.top10";
	public static final String GET_ALL_BIZSTATS = "get.bizstats.all";

    private final ObjectMapper mapper = Json.mapper;
    private final DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
    private static final Logger log = LoggerFactory.getLogger(BizStatsVerticle.class);

    @Autowired
    private BizStatsService statsService;

    @Override
    public void start() throws Exception {
        super.start();
        vertx.eventBus()
                .<String>consumer(GET_TOP10_BIZSTATS)
                .handler(getTop10BizStats(statsService));
        vertx.eventBus()
        .<String>consumer(GET_ALL_BIZSTATS)
        .handler(getAllBizStats(statsService));
    }

    private Handler<Message<String>> getTop10BizStats(BizStatsService service) {
        return msg -> vertx.<String>executeBlocking(future -> {
            try {
                future.complete(mapper.writeValueAsString(service.getTop10ByScore()));
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
    
    private Handler<Message<String>> getAllBizStats(BizStatsService service) {
        return msg -> vertx.<String>executeBlocking(future -> {
            try {
            	JsonObject params = new JsonObject(msg.body());
            	
            	Pageable page = RestUtil.getPageable(params, RestUtil.getSort(params));
            	String coinId = params.getString("coinId");
            	Date from = null;
            	if(params.getString("from") != null){
            		from = format.parse(params.getString("from"));
            	}
            	Date to = null;
            	if(params.getString("to") != null){
            		to = format.parse(params.getString("to"));
            	}
            	Page<BizStatsDTO> res = service.getAllByCoin(coinId, from, to, page);
                future.complete(mapper.writeValueAsString(res));
                
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize result");
                future.fail(e);
            } catch(ServiceException | NotFoundException e){
            	log.error(e.getMessage());
            	future.fail(e);
            } catch (ParseException e) {
            	log.error(e.getMessage());
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

