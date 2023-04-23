package com.example.weather.service;

import com.example.weather.dto.WeatherDto;
import com.example.weather.dto.WeatherResponse;
import com.example.weather.exception.WeatherStackApiException;
import com.example.weather.model.WeatherEntity;
import com.example.weather.repository.WeatherRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.ErrorResponse;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static com.example.weather.constants.Constants.*;

@Service
public class WeatherService {
    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);

    private final WeatherRepository weatherRepository;
    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Clock clock;

    public WeatherService(WeatherRepository weatherRepository,
                          RestTemplate restTemplate,
                          Clock clock) {
        this.weatherRepository = weatherRepository;
        this.restTemplate = restTemplate;
        this.clock = clock;
    }

    public WeatherDto getWeather(String city) {

        Optional<WeatherEntity> weatherEntityOptional = weatherRepository.findFirstByRequestedCityNameOrderByUpdatedTimeDesc(city);

        return weatherEntityOptional.map(weather -> {
            if (weather.getUpdatedTime().isBefore(getLocalDateTimeNow().minusMinutes(API_CALL_LIMIT))) {
                logger.info(String.format("Creating a new city weather from weather stack api for %s due to the current one is not up-to-date", city));
                return createCityWeather(city);
            }
            logger.info(String.format("Getting weather from database for %s due to it is already up-to-date", city));
            return WeatherDto.convert(weather);
        }).orElseGet(() -> createCityWeather(city));
    }
    public WeatherDto createCityWeather(String city) {
        logger.info("Requesting weather stack api for city: " + city);
        String url = getWeatherStackUrl(city);
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);

        try {
            WeatherResponse weatherResponse = objectMapper.readValue(responseEntity.getBody(), WeatherResponse.class);
            return WeatherDto.convert(saveWeatherEntity(city, weatherResponse));
        } catch (JsonProcessingException e) {
            try {
                ErrorResponse errorResponse = objectMapper.readValue(responseEntity.getBody(), ErrorResponse.class);
                throw new WeatherStackApiException(errorResponse);
            } catch (JsonProcessingException | WeatherStackApiException ex) {
                throw new RuntimeException(ex.getMessage());
            }
        }
    }
    private WeatherEntity saveWeatherEntity(String city, WeatherResponse response) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        WeatherEntity weatherEntity = new WeatherEntity(city,
                response.location().name(),
                response.location().country(),
                response.current().temperature(),
                getLocalDateTimeNow(),
                LocalDateTime.parse(response.location().localtime(), formatter));

        return weatherRepository.save(weatherEntity);
    }

    private String getWeatherStackUrl(String city) {
        return WEATHER_STACK_API_BASE_URL + WEATHER_STACK_API_ACCESS_KEY_PARAM + API_KEY + WEATHER_STACK_API_QUERY_PARAM + city;
    }

    private LocalDateTime getLocalDateTimeNow() {
        Instant instant = clock.instant();
        return LocalDateTime.ofInstant(
                instant,
                Clock.systemDefaultZone().getZone());
    }

}