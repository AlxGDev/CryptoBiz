package org.alexgdev.bizwatch.service;

import java.util.List;
import java.util.Optional;

import org.alexgdev.bizwatch.entities.CryptoCoin;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface ICoinService {
	  Future<Boolean> initData();

	  Future<Boolean> insert(CryptoCoin coin);

	  Future<List<CryptoCoin>> getAll(JsonObject params);

	  Future<Optional<CryptoCoin>> getCertain(String coinID);

	  /*Future<CryptoCoin> update(String coinID, CryptoCoin newCoin);

	  Future<Boolean> delete(String coinID);

	  Future<Boolean> deleteAll(); */
}
