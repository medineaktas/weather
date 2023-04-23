package com.example.weather.controller;

import com.example.weather.constants.validated.CityNameConstraint;
import com.example.weather.dto.WeatherDto;
import com.example.weather.service.WeatherService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/open-weather")
@Validated
@Tag(name = "Open Weather Service API v1", description = "Open Weather Service API to search the current weather report of the city")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @RateLimiter(name = "basic")
    @GetMapping("/{city}")
    public ResponseEntity<WeatherDto> getWeather(@PathVariable("city") @CityNameConstraint @NotBlank String city) {
        return ResponseEntity.ok(weatherService.getWeather(city));
    }
}
