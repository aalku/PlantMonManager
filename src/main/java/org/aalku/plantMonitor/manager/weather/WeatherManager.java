package org.aalku.plantMonitor.manager.weather;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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
public class WeatherManager implements InitializingBean {

	private static Logger log = LogManager.getLogger(WeatherManager.class);

	public static class Weather {
		public static class Main {
			private float temp;
			private float pressure;
			private float humidity;
			public float getTemp() {
				return temp;
			}
			public void setTemp(float temp) {
				this.temp = temp;
			}
			public float getPressure() {
				return pressure;
			}
			public void setPressure(float pressure) {
				this.pressure = pressure;
			}
			public float getHumidity() {
				return humidity;
			}
			public void setHumidity(float humidity) {
				this.humidity = humidity;
			}
		}
		private Main main;
		public float getTemp() {
			return main.temp;
		}
		public void setMain(Main main) {
			this.main = main;
		}
		public float getPressure() {
			return main.getPressure();
		}
		public float getHumidity() {
			return main.getHumidity();
		}
	}

	@Value("http://api.openweathermap.org/data/2.5")
	private String baseUrl;

	@Value("${api.openweathermap.org.key}")
	private String appid;

	@Value("${api.openweathermap.org.city}")
	private String city;

	private WebClient client;

	private AsyncCacheLoader<String, Weather> loader = (city, e) -> {
		Mono<Weather> weather = client.get()
				.uri(builder -> {
					URI uri = builder.pathSegment("weather").queryParam("appid", appid).queryParam("units", "metric").queryParam("q", city).build();
					log.debug("Requestting: {}", uri);
					return uri;
				})
				.retrieve().bodyToMono(Weather.class);
		return weather.toFuture();
	};

	private AsyncLoadingCache<String, Weather> weatherCache = Caffeine.newBuilder()
			.expireAfterWrite(2, TimeUnit.MINUTES).buildAsync(loader);

	public Weather getWeather() {
		try {
			Weather res = weatherCache.get(city).get();
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
