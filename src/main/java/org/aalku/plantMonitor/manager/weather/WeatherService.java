package org.aalku.plantMonitor.manager.weather;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.aalku.plantMonitor.manager.vo.WeatherData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;

import reactor.core.publisher.Mono;

@Component
public class WeatherService implements InitializingBean {

	public interface Place {
		String getName();
		String getCountry();
		float getLon();
		float getLat();
	}

	private static Logger log = LogManager.getLogger(WeatherService.class);

	public static class WeatherImplOWM implements WeatherData {
		public static class Main {
			float temp;
			float pressure;
			float humidity;
			public void setTemp(float temp) {
				this.temp = temp;
			}
			public void setPressure(float pressure) {
				this.pressure = pressure;
			}
			public void setHumidity(float humidity) {
				this.humidity = humidity;
			}
		}
		public static class Coord {
			float lon;
			float lat;
			public void setLon(float lon) {
				this.lon = lon;
			}
			public void setLat(float lat) {
				this.lat = lat;
			}
		}
		public static class Sys {
			String country;
			public void setCountry(String country) {
				this.country = country;
			}
		}
		private Main main;
		private String name;
		private Coord coord;
		private Sys sys;
		@Override
		public float getTemp() {
			return main.temp;
		}
		public void setMain(Main main) {
			this.main = main;
		}
		public void setName(String name) {
			this.name = name;
		}
		public void setCoord(Coord coord) {
			this.coord = coord;
		}
		public void setSys(Sys sys) {
			this.sys = sys;
		}
		@Override
		public float getPressure() {
			return main.pressure;
		}
		@Override
		public float getHumidity() {
			return main.humidity;
		}
		@Override
		public Place getPlace() {
			return new Place() {
				@Override
				public String getName() {
					return name;
				}
				@Override
				public String getCountry() {
					return sys.country;
				}
				@Override
				public float getLon() {
					return coord.lon;
				}
				@Override
				public float getLat() {
					return coord.lat;
				}
			};
		}
	}

	@Value("http://api.openweathermap.org/data/2.5")
	private String baseUrl;

	@Value("${api.openweathermap.org.key}")
	private String appid;

	private WebClient client;

	private AsyncCacheLoader<String, WeatherImplOWM> loader = (city, e) -> {
		Mono<WeatherImplOWM> weather = client.get()
				.uri(builder -> {
					URI uri = builder.pathSegment("weather").queryParam("appid", appid).queryParam("units", "metric").queryParam("q", city).build();
					log.debug("Requestting: {}", uri);
					return uri;
				})
				.retrieve().bodyToMono(WeatherImplOWM.class);
		return weather.toFuture();
	};

	private AsyncLoadingCache<String, WeatherImplOWM> weatherCache = Caffeine.newBuilder()
			.expireAfterWrite(2, TimeUnit.MINUTES).buildAsync(loader);

	public WeatherData getWeather(String place) {
		try {
			WeatherData res = weatherCache.get(place).get();
			return res;
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		client = WebClient.create(baseUrl);
	}
}
