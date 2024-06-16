package vn.cloud.external_properties_demo;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@ConfigurationPropertiesBinding
public class FullNameConverter implements Converter<String, MyServiceConfiguration.FullName> {
    @Override
    public MyServiceConfiguration.FullName convert(String source) {
        String[] parts = source.split(" ");
        return new MyServiceConfiguration.FullName(parts[0], parts[1]);
    }
}
