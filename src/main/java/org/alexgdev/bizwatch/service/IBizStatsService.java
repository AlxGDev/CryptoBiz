package org.alexgdev.bizwatch.service;

import java.util.List;
import java.util.Optional;

import org.alexgdev.bizwatch.entities.BizStatsEntry;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface IBizStatsService {
	 Future<Boolean> initData();

	  Future<Boolean> insert(BizStatsEntry entry);

	  Future<List<BizStatsEntry>> getAll(JsonObject params);
	  Future<List<BizStatsEntry>> getTop10();

	  Future<Optional<BizStatsEntry>> getCertain(Long id);
}
