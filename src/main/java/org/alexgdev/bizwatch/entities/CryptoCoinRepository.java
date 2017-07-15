package org.alexgdev.bizwatch.entities;



import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

public interface CryptoCoinRepository extends CrudRepository<CryptoCoin, String>{
	Page<CryptoCoin> findAll(Pageable pageable);


}
