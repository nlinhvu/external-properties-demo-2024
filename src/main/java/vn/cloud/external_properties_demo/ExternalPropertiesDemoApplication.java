package vn.cloud.external_properties_demo;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(value = {MyServiceConfiguration.class})
public class ExternalPropertiesDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExternalPropertiesDemoApplication.class, args);
	}

	@Bean
	ApplicationRunner applicationRunner(MyServiceConfiguration myServiceConfiguration) {
		return args -> {
			System.out.println(myServiceConfiguration);
		};
	}
}
