package org.aalku.plantMonitor.manager.vo;

public class PlantData {
	@Override
	public String toString() {
		return "[vcc=" + vcc + ", vbat=" + vbat + ", sense=" + sense + "]";
	}
	private Float vcc;
	private Float vbat;
	private Integer sense;
	
	public Float getVcc() {
		return vcc;
	}
	public void setVcc(Float vcc) {
		this.vcc = vcc;
	}
	public Float getVbat() {
		return vbat;
	}
	public void setVbat(Float vbat) {
		this.vbat = vbat;
	}
	public Integer getSense() {
		return sense;
	}
	public void setSense(Integer sense) {
		this.sense = sense;
	}
}
