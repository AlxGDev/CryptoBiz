package org.alexgdev.bizwatch.entities;

import java.util.Date;
import java.util.List;



import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface BizStatsEntryRepository extends CrudRepository<BizStatsEntry, Long>{
	Page<BizStatsEntry> findAll(Pageable pageable);
	Page<BizStatsEntry> findAllByCoin(CryptoCoin coin, Pageable pageable);
	Page<BizStatsEntry> findAllByCoinAndDateBetween(CryptoCoin coin, Date before, Date after, Pageable pageable);
	
	@Transactional
    @Query("SELECT b " +
    	   "FROM BizStatsEntry b "+ 
    	   "WHERE b.date = (SELECT max(b2.date) FROM BizStatsEntry b2)"+
    	   "ORDER BY (b.nrThreads + b.nrPosts) DESC )")
    List<BizStatsEntry> getCurrentTopByScore (Pageable pageable);

}
