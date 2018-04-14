package org.aalku.plantMonitor.manager.repository;

import java.util.List;

import org.aalku.plantMonitor.manager.vo.PersistedConfiguration;
import org.springframework.data.repository.CrudRepository;

public interface ConfigRepository extends CrudRepository<PersistedConfiguration, Long> {

	List<PersistedConfiguration> get(Long id);
	
}