package org.aalku.plantMonitor.manager.repository;

import java.util.Optional;

import org.aalku.plantMonitor.manager.vo.PersistedConfiguration;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface ConfigRepository extends CrudRepository<PersistedConfiguration, Long> {
	
	@Query("SELECT x FROM PersistedConfiguration x order by x.id desc")
	Optional<PersistedConfiguration> load();
}