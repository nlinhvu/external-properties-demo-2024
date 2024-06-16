package vn.cloud.external_properties_demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "my-service.common-attributes")
public record MyServiceConfiguration(FullName author,
                                     String systemEmail,
                                     Duration readTimeout,
                                     BigDecimal thresholdLimit,
                                     Currency currency,
                                     Map<String, Country> supportedCountries) {

    public record Country(String iso3Code, List<ZoneId> timezones) {}

    public record FullName(String firstName, String lastName) {}
}

