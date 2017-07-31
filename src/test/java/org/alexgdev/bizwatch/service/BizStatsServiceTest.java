package org.alexgdev.bizwatch.service;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.alexgdev.bizwatch.entities.BizStatsEntry;
import org.alexgdev.bizwatch.entities.BizStatsEntryRepository;
import org.alexgdev.bizwatch.entities.CryptoCoin;
import org.alexgdev.bizwatch.entities.CryptoCoinRepository;
import org.alexgdev.bizwatch.exception.NotFoundException;
import org.alexgdev.bizwatch.exception.ServiceException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class BizStatsServiceTest {
	
	@Configuration
	static class BizStatsServiceTestContextConfiguration {
		@Bean
		public BizStatsService bizStatsService() {
			return new BizStatsService();
		}
		@Bean
		public CoinService coinService() {
			return Mockito.mock(CoinService.class);
		}
		@Bean
		public BizStatsEntryRepository bizStatsEntryRepository() {
			return Mockito.mock(BizStatsEntryRepository.class);
		}
		
		@Bean
		public CryptoCoinRepository cryptoCoinRepository() {
			return Mockito.mock(CryptoCoinRepository.class);
		}
	}
	
	@Autowired
	private BizStatsService service;
	
	@Autowired
	private CoinService coinService;
	
	@Autowired
	private BizStatsEntryRepository repo;
	
	@Autowired
	private CryptoCoinRepository coinRepo;
	
	private CryptoCoin coin;
	private Pageable pageable;
	private Date date1;
	private Date date2;
	private Date date3;

	@Before
	public void setUp() throws Exception {
		date1 = new Date(1400000000);
	
		date2 = new Date(1500000000);
		date3 = new Date(1600000000);

		pageable = new PageRequest(1, 1);
		coin = new CryptoCoin();
		coin.setId("BTC");
		coin.setName("bitcoin");
		coin.setSymbol("btc");
		BizStatsEntry entry = new BizStatsEntry();
		entry.setId(1L);
		entry.setAvailable_supply(1000.0);
		entry.setAverageSentiment(0.4);
		entry.setCoin(coin);
		entry.setDate(date2);
		entry.setNrThreads(0);
		entry.setNegativeMentions(5);
		entry.setNrPosts(50);
		entry.setPositiveMentions(50);
		List<BizStatsEntry> list = new ArrayList<BizStatsEntry>();
		list.add(entry);
		Page<BizStatsEntry> page = new PageImpl<BizStatsEntry>(list);
		Mockito.when(repo.findAll(pageable)).thenReturn(page);
		Mockito.when(repo.findAllByCoin(coin, pageable)).thenReturn(page);
		Mockito.when(repo.findAllByCoinAndDateBetween(coin, date3, date1, pageable)).thenReturn(page);
		Mockito.when(repo.findAllByCoinAndDateBetween(coin, date2, date1, pageable)).thenReturn(new PageImpl<BizStatsEntry>(new ArrayList<BizStatsEntry>()));
		Mockito.when(repo.getCurrentTopByScore(pageable)).thenReturn(list);
		Mockito.when(coinService.getById("BTC")).thenReturn(coin);
		Mockito.when(coinRepo.findOne("BTC")).thenReturn(coin);
		
		
	}

	@Test
	public void testBizStatService() throws ServiceException, NotFoundException {
		assertTrue(1L == service.getAll(pageable).getContent().get(0).getId());
		assertTrue(1L == service.getAllByCoin(coin.getId(), null, null, pageable).getContent().get(0).getId());
	}

}
