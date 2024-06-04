package vn.cloud.external_properties_demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PeopleService {
    private static final Logger log = LoggerFactory.getLogger(PeopleService.class);

    public PeopleService(PeopleConfiguration peopleConfiguration) {
        log.info("First Name: {}", peopleConfiguration.getFirstname());
        log.info("Last Name: {}", peopleConfiguration.getLastName());
        log.info("Hobbies: {}", peopleConfiguration.getHobbies());
    }
}