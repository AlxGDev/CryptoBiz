package org.alexgdev.bizwatch.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.alexgdev.bizwatch.entities.BizStatsEntry;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;

public class JDBCBizStatsService implements IBizStatsService {

	private static final String SQL_CREATE = "CREATE TABLE IF NOT EXISTS bizstats"
											+ "("
											+ "id bigint NOT NULL DEFAULT nextval('bizstats_id_seq'::regclass),"
											+ "available_supply double precision,"
											+ "average_sentiment double precision NOT NULL,"
											+ "date timestamp without time zone NOT NULL,"
											+ "market_cap_usd double precision,"
											+ "negative_mentions integer NOT NULL,"
											+ "nr_posts integer NOT NULL,"
											+ "nr_threads integer NOT NULL,"
											+ "positive_mentions integer NOT NULL,"
											+ "price_btc double precision,"
											+ "price_usd double precision,"
											+ "top5words character varying(255),"
											+ "total_supply double precision,"
											+ "cryptocoin_id character varying(255) NOT NULL,"
											+ "CONSTRAINT bizstats_pkey PRIMARY KEY (id),"
											+ "CONSTRAINT fkqejfjxg82jfrktjycbneiqugi FOREIGN KEY (cryptocoin_id) "
											+ "REFERENCES cryptocoin (id) MATCH SIMPLE "
											+ "ON UPDATE NO ACTION ON DELETE NO ACTION"
											+ ")";
	
	private static final String SQL_QUERY_CERTAIN = "SELECT b.*,c.name,c.symbol FROM bizstats b, cryptocoin c WHERE b.cryptocoin_id = c.id AND b.id = ?";
	private static final String SQL_QUERY_ALL_PAGEABLE = "SELECT b.*,c.name,c.symbol FROM bizstats b, cryptocoin c WHERE b.cryptocoin_id = c.id ORDER BY b.id LIMIT ? OFFSET ?";
	private static final String SQL_QUERY_ALL_PAGEABLE_FOR_COIN = "SELECT b.*,c.name,c.symbol FROM bizstats b, cryptocoin c WHERE b.cryptocoin_id = c.id AND b.cryptocoin_id = ? ORDER BY b.id LIMIT ? OFFSET ?";
	private static final String SQL_QUERY_ALL_PAGEABLE_FOR_COIN_AND_DATE = "SELECT b.*,c.name,c.symbol FROM bizstats b, cryptocoin c WHERE b.cryptocoin_id = c.id AND b.cryptocoin_id = ? AND b.date > ? AND b.date < ? ORDER BY b.id LIMIT ? OFFSET ?";
	
	private static final String SQL_INSERT = "INSERT INTO bizstats(available_supply, average_sentiment, date, market_cap_usd, "
											+ "negative_mentions, nr_posts, nr_threads, positive_mentions, price_btc, "
											+ "price_usd, top5words, total_supply, cryptocoin_id) "
											+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String SQL_GET_TOP10 = "SELECT b.*,c.name,c.symbol FROM bizstats b, cryptocoin c WHERE b.cryptocoin_id = c.id AND b.date = (SELECT max(b2.date) "
												+ "FROM bizstats b2) "
												+ "ORDER BY (b.nr_Threads + b.nr_Posts) DESC "
												+ "LIMIT 10 ";
	
	private static final Integer defaultSize = 1000;
	private static final Integer defaultPage = 0;
	
	private final Vertx vertx;
	private final JsonObject config;
	private final SQLClient client;
	
	public JDBCBizStatsService() {
	    this(Vertx.vertx(), new JsonObject().put("password", "root").put("url", "jdbc:postgresql://localhost:5432/bizwatchdb").put("user", "postgres"));
	}
	
	public JDBCBizStatsService(JsonObject config) {
	    this(Vertx.vertx(), config);
	}

	public JDBCBizStatsService(Vertx vertx, JsonObject config) {
	    this.vertx = vertx;
	    this.config = config;
	    this.client = JDBCClient.createShared(vertx, config);
	}

	private Handler<AsyncResult<SQLConnection>> connHandler(Future future, Handler<SQLConnection> handler) {
	    return conn -> {
	      if (conn.succeeded()) {
	        final SQLConnection connection = conn.result();
	        handler.handle(connection);
	      } else {
	        future.fail(conn.cause());
	      }
	    };
	}
	
	@Override
	public Future<Boolean> initData() {
		Future<Boolean> result = Future.future();
	    client.getConnection(connHandler(result, connection ->
	      connection.execute(SQL_CREATE, create -> {
	        if (create.succeeded()) {
	          result.complete(true);
	        } else {
	          result.fail(create.cause());
	        }
	        connection.close();
	      })));
	    return result;
	}


	@Override
	public Future<Boolean> insert(BizStatsEntry entry) {
		Future<Boolean> result = Future.future();
	    client.getConnection(connHandler(result, connection -> {
	      connection.updateWithParams(SQL_INSERT, new JsonArray()
	    		  .add(entry.getAvailable_supply())
	        .add(entry.getAverageSentiment())
	        .add(entry.getDate())
	        .add(entry.getMarket_cap_usd())
	        .add(entry.getNegativeMentions())
	        .add(entry.getNrPosts())
	        .add(entry.getNrThreads())
	        .add(entry.getPositiveMentions())
	        .add(entry.getPrice_btc())
	        .add(entry.getPrice_usd())
	        .add(entry.getTop5words())
	        .add(entry.getTotal_supply())
	        .add(entry.getCoin().getId()), r -> {
	        if (r.failed()) {
	          result.fail(r.cause());
	        } else {
	          result.complete(true);
	        }
	        connection.close();
	      });
	    }));
	    return result;
	}

	@Override
	public Future<List<BizStatsEntry>> getAll(JsonObject params) {
		JsonArray queryParams = new JsonArray();
		String query;
		if(params.containsKey("coinId") && params.getValue("coinId") instanceof String){
			queryParams.add(params.getString("coinId"));
			if(params.containsKey("from") && params.containsKey("to") && params.getValue("from") instanceof String && params.getValue("to") instanceof String){
				query = SQL_QUERY_ALL_PAGEABLE_FOR_COIN_AND_DATE;
				queryParams.add(params.getString("from"));
				queryParams.add(params.getString("to"));
			} else {
				query = SQL_QUERY_ALL_PAGEABLE_FOR_COIN;
			}
		} else {
			query = SQL_QUERY_ALL_PAGEABLE;
		}
		if(params.containsKey("size") && params.getValue("size") instanceof Integer){
			queryParams.add(params.getInteger("size"));
		} else {
			queryParams.add(defaultSize);
		}
		if(params.containsKey("page") && params.getValue("page") instanceof Integer){
			queryParams.add(params.getInteger("page") * queryParams.getInteger(0));
		} else {
			queryParams.add(defaultPage);
		}
		
		Future<List<BizStatsEntry>> result = Future.future();
	    client.getConnection(connHandler(result, connection ->
	    	connection.queryWithParams(query,queryParams, r -> {
		        if (r.failed()) {
		          result.fail(r.cause());
		        } else {
		          List<BizStatsEntry> todos = r.result().getRows().stream()
		            .map(BizStatsEntry::new)
		            .collect(Collectors.toList());
		          result.complete(todos);
		        }
		        connection.close();
	      })));
	    return result;
	}

	@Override
	public Future<Optional<BizStatsEntry>> getCertain(Long id) {
		Future<Optional<BizStatsEntry>> result = Future.future();
	    client.getConnection(connHandler(result, connection -> {
	      connection.queryWithParams(SQL_QUERY_CERTAIN, new JsonArray().add(id), r -> {
	        if (r.failed()) {
	          result.fail(r.cause());
	        } else {
	          List<JsonObject> list = r.result().getRows();
	          if (list == null || list.isEmpty()) {
	            result.complete(Optional.empty());
	          } else {
	            result.complete(Optional.of(new BizStatsEntry(list.get(0))));
	          }
	        }
	        connection.close();
	      });
	    }));
	    return result;
	}

	@Override
	public Future<List<BizStatsEntry>> getTop10() {
		Future<List<BizStatsEntry>> result = Future.future();
	    client.getConnection(connHandler(result, connection ->
	    	connection.query(SQL_GET_TOP10, r -> {
		        if (r.failed()) {
		          result.fail(r.cause());
		        } else {
		          List<BizStatsEntry> stats = r.result().getRows().stream()
		            .map(BizStatsEntry::new)
		            .collect(Collectors.toList());
		          result.complete(stats);
		        }
		        connection.close();
	      })));
	    return result;
	}
	

}
