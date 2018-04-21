package org.aalku.plantMonitor.manager.repository.plant;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.aalku.plantMonitor.manager.vo.PlantData;
import org.influxdb.dto.Point;
import org.springframework.data.influxdb.InfluxDBTemplate;
import org.springframework.stereotype.Component;

@Component
public class PlantDataRepository {

	@Resource
	private InfluxDBTemplate<Point> influxDBTemplate;

	public void store(String addr, PlantData d) {
		final Point dataPoint = Point.measurement("moisture").time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
				.tag("addr", addr).addField("sense", d.getSense()).build();
		final Point metaPoint = Point.measurement("meta").time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
				.tag("addr", addr).addField("vcc", d.getVcc()).addField("vbat", d.getVbat()).build();
		influxDBTemplate.write(dataPoint);
		influxDBTemplate.write(metaPoint);
	}

}
