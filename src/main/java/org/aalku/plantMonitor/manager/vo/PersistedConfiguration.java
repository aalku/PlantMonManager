package org.aalku.plantMonitor.manager.vo;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class PersistedConfiguration {
	@Id
	private Long id;
	private String portName;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPortName() {
		return portName;
	}

	public void setPortName(String portName) {
		this.portName = portName;
	}
}
