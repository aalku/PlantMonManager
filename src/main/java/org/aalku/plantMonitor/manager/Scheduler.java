package org.aalku.plantMonitor.manager;

import javax.annotation.Resource;

import org.aalku.plantMonitor.manager.repository.weather.WeatherDataRepository;
import org.aalku.plantMonitor.manager.vo.WeatherData;
import org.aalku.plantMonitor.manager.weather.WeatherService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Scheduler {
	
	@Resource
	WeatherDataRepository weatherRepository;
	
	@Resource
	WeatherService weatherService;
	
	@Value("${api.openweathermap.org.place}")
	private String place;

	@Scheduled(fixedRate=1000L*60L)
	public void everyMinute() {
		WeatherData weather = weatherService.getWeather(place);
		weatherRepository.store(weather);
	}
}
