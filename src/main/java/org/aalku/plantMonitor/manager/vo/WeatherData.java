package org.aalku.plantMonitor.manager.vo;

import org.aalku.plantMonitor.manager.weather.WeatherService.Place;

public interface WeatherData {

	float getTemp();

	float getPressure();

	float getHumidity();
	
	Place getPlace();

}