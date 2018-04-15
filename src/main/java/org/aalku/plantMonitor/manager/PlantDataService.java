package org.aalku.plantMonitor.manager;

import javax.annotation.Resource;

import org.aalku.plantMonitor.manager.vo.PlantData;
import org.influxdb.dto.Point;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.influxdb.InfluxDBTemplate;
import org.springframework.stereotype.Component;

@Component
public class PlantDataService implements InitializingBean {

	@Resource
	private InfluxDBTemplate<Point> influxDBTemplate;

	public void store(String addr, PlantData d) {
//		final Point p = Point.measurement("disk").time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
//				.tag("tag", "value").addField("1field", 0L).addField("field2", 1L).build();
//		influxDBTemplate.write(p);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		influxDBTemplate.createDatabase();
	}

}
