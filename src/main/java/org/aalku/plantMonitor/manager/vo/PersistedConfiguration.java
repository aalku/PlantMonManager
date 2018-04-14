package org.aalku.plantMonitor.manager.vo;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class PersistedConfiguration {
	@Id
    @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
}
