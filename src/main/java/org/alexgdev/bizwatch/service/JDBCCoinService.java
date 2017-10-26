package org.alexgdev.bizwatch.service;


import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


import org.alexgdev.bizwatch.entities.CryptoCoin;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;

public class JDBCCoinService implements ICoinService{
	
	private static final String SQL_CREATE = "CREATE TABLE IF NOT EXISTS cryptocoin"
											+ "("
											+ "id character varying(255) NOT NULL,"
											+ "name character varying(255) NOT NULL,"
											+ "symbol character varying(255) NOT NULL,"
											+ "CONSTRAINT cryptocoin_pkey PRIMARY KEY (id)"
											+ ")";
	private static final String SQL_QUERY_ALL = "SELECT * FROM cryptocoin ORDER BY id";
	private static final String SQL_QUERY_CERTAIN = "SELECT * FROM cryptocoin WHERE id = ?";
	private static final String SQL_QUERY_ALL_PAGEABLE = "SELECT * FROM cryptocoin ORDER BY id LIMIT ? OFFSET ?";
	private static final String SQL_INSERT = "INSERT INTO cryptocoin(id, name, symbol) VALUES (?, ?, ?)";
	
	private static final Integer defaultSize = 1000;
	private static final Integer defaultPage = 0;
	
	private final Vertx vertx;
	private final JsonObject config;
	private final SQLClient client;
	
	public JDBCCoinService() {
	    this(Vertx.vertx(), new JsonObject().put("password", "root").put("url", "jdbc:postgresql://localhost:5432/bizwatchdb").put("user", "postgres"));
	}
	
	public JDBCCoinService(JsonObject config) {
	    this(Vertx.vertx(), config);
	}

	public JDBCCoinService(Vertx vertx, JsonObject config) {
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
	public Future<Boolean> insert(CryptoCoin coin) {
		Future<Boolean> result = Future.future();
	    client.getConnection(connHandler(result, connection -> {
	      connection.updateWithParams(SQL_INSERT, new JsonArray().add(coin.getId())
	        .add(coin.getName())
	        .add(coin.getSymbol()), r -> {
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
	public Future<List<CryptoCoin>> getAll(JsonObject params) {
		JsonArray queryParams = new JsonArray();
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
		
		Future<List<CryptoCoin>> result = Future.future();
	    client.getConnection(connHandler(result, connection ->
	    	connection.queryWithParams(SQL_QUERY_ALL_PAGEABLE,queryParams, r -> {
		        if (r.failed()) {
		          result.fail(r.cause());
		        } else {
		          List<CryptoCoin> todos = r.result().getRows().stream()
		            .map(CryptoCoin::new)
		            .collect(Collectors.toList());
		          result.complete(todos);
		        }
		        connection.close();
	      })));
	    return result;
	}

	@Override
	public Future<Optional<CryptoCoin>> getCertain(String coinID) {
		Future<Optional<CryptoCoin>> result = Future.future();
	    client.getConnection(connHandler(result, connection -> {
	      connection.queryWithParams(SQL_QUERY_CERTAIN, new JsonArray().add(coinID), r -> {
	        if (r.failed()) {
	          result.fail(r.cause());
	        } else {
	          List<JsonObject> list = r.result().getRows();
	          if (list == null || list.isEmpty()) {
	            result.complete(Optional.empty());
	          } else {
	            result.complete(Optional.of(new CryptoCoin(list.get(0))));
	          }
	        }
	        connection.close();
	      });
	    }));
	    return result;
	}

}
