package org.aalku.plantMonitor.manager.repository.weather;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.aalku.plantMonitor.manager.vo.WeatherData;
import org.influxdb.dto.Point;
import org.springframework.data.influxdb.InfluxDBTemplate;
import org.springframework.stereotype.Component;

@Component
public class WeatherDataRepository {

	@Resource
	private InfluxDBTemplate<Point> influxDBTemplate;

	public void store(WeatherData d) {
		final Point dataPoint = Point.measurement("weather").time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
				.tag("place", d.getPlace().getName())
				.tag("country", d.getPlace().getCountry())
				.tag("coords", d.getPlace().getLon() + ":" + d.getPlace().getLat())
				.addField("temp", d.getTemp())
				.addField("humidity", d.getHumidity())
				.addField("pressure", d.getPressure())
				.build();
		influxDBTemplate.write(dataPoint);
	}

}
