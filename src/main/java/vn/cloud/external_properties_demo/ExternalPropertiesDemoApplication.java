package vn.cloud.external_properties_demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExternalPropertiesDemoApplication {
	private static final Logger log = LoggerFactory.getLogger(ExternalPropertiesDemoApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(ExternalPropertiesDemoApplication.class, args);
	}

}
