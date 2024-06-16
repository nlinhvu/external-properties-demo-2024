# @ConfigurationProperties: PropertiesBinding vs ConstructorBinding, Properties Conversion with Converter

> This repo is used in this Youtube video: https://youtu.be/qWr9WD3uYCE

> **Converting properties below**
```yaml
my-service.common-attributes:
  author: Linh Vu
  system-email: my_system@mail.com
  read-timeout: 30s
  threshold-limit: 1000.0
  currency: EUR

  supported-countries:
    germany:
      iso3-code: DEU
      timezones:
        - Europe/Berlin
    spain:
      iso3-code: ESP
      timezones:
        - Europe/Madrid
        - Atlantic/Canary
```
> **to a configuration class**
```java
@ConfigurationProperties(prefix = "my-service.common-attributes")
public class MyServiceConfiguration {
    private String author;
    private String systemEmail;
    private String readTimeout;
    private String thresholdLimit;
    private String currency;
    
    private Map<String, Country> supportedCountries;
    
    public static class Country {
        private String iso3Code;
        private List<String> timezones ;
    }
}
```
---
To enable this configuration class, it depends on:
1. **Properties Binding**
* `@EnableConfigurationProperties`
* `@ConfigurationPropertiesScan`
* `@Component`
2. **Constructor Binding**
* `@EnableConfigurationProperties`
* `@ConfigurationPropertiesScan`

---

Let's use `@EnableConfigurationProperties` that works for both `Properties Binding` and `Constructor Binding` to enable
`the configuration class` as `a configuration bean` and test by autowiring it to `ApplicationRunner` bean 
```java
@EnableConfigurationProperties(value = {MyServiceConfiguration.class})
```
```java
@Bean
ApplicationRunner applicationRunner(MyServiceConfiguration myServiceConfiguration) {
    return args -> {
        System.out.println(myServiceConfiguration);
    };
}
```

## 1. JavaBean Properties Binding (Properties Binding)
At the moment, we only have **one** `default no-argument constructor` implicitly created by Java. It leads to
the use of `Properties Binding` that requires `setters` in place (for most cases)
```java
@ConfigurationProperties(prefix = "my-service.common-attributes")
public class MyServiceConfiguration {
    private String author;
    private String systemEmail;
    private String readTimeout;
    private String thresholdLimit;
    private String currency;
    
    private Map<String, Country> supportedCountries;
    
    // Setters

    public static class Country {
        private String iso3Code;
        private List<String> timezones ;

        // Setters
    }
}
```

---

Explicitly declare a `no-argument constructor`, we'll see, first, our `constructor` will be called to initialize a bean,
then `JavaBeanBinder` set up the value for each property based on `setters`
```java
this.setter.invoke(instance.get(), value);
```
That's why it is called `JavaBean Properties Binding`. Let's test `@ConfigurationPropertiesScan` and `@Component`.

---

## 2. Constructor Binding
Instead of using `no-argument constructor`, we can use `all-argument constructor` to switch to `Constructor Binding`.

Let's add a `all-argument constructor` and run again (still keep the `no-argument constructor`)
```java
public MyServiceConfiguration() {
}

public MyServiceConfiguration(String author, String systemEmail, String readTimeout, String thresholdLimit, String currency, Map<String, Country> supportedCountries) {
    this.author = author;
    this.systemEmail = systemEmail;
    this.readTimeout = readTimeout;
    this.thresholdLimit = thresholdLimit;
    this.currency = currency;
    this.supportedCountries = supportedCountries;
}
```

> **Noted:** If we have `no-argument constructor`, `no-argument constructor` will be used along with `Properties Binding`.

> **Noted:** If we have more than one constructor, we need to specify which one will be used by `@ConstructorBinding`.
> If we can, only use 1 `all-argument constructor` to trigger `Constructor Binding` and avoid confusing.

The advantage of `Constructor Binding` is we can keep our beans **immutable**, by not exposing `setters` and only returning
`immutable objects` through `getters`.

```java
...
private final String author;
private final String systemEmail;
...
```
```java
public Map<String, Country> getSupportedCountries() {
    return Collections.unmodifiableMap(this.supportedCountries);
}

public List<String> getTimezones() {
    return Collections.unmodifiableList(this.timezones);
}
```
Now, if we look at the trace stack, we're going to see `ConfigurationPropertiesBinder` will be used for `Constructor Binding`.
Let's test `@ConfigurationPropertiesScan` and `@Component`

---

This `class` is now function like a `record`: `immutable`, `1 all-argument constructor`, `no setters`,...
We can easily realize that we can use `Record` as a `ConfigurationProperties bean`
```java
@ConfigurationProperties(prefix = "my-service.common-attributes")
public record MyServiceConfiguration(
        String author,
        String systemEmail,
        String readTimeout,
        String thresholdLimit,
        String currency,
        Map<String, Country> supportedCountries) {

    public record Country(String iso3Code, List<String> timezones) {
    }
}
```

## 3. Properties Conversion with Converter
`Spring Boot` tries to **covert** the `external application properties` to `the right type` when it binds to the `@ConfigurationProperties` beans.
But obviously **NOT** all the type, please check out [the official documentation](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties.conversion)

> **Let's try:**
> 
> **readTimeout:**     `String` --> `Duration`
> 
> **thresholdLimit:**  `String` --> `BigDecimal`
> 
> **currency:** `String` --> `Currency`
> 
> **timezone:**        `String` --> `ZoneId`

```java
@ConfigurationProperties(prefix = "my-service.common-attributes")
public record MyServiceConfiguration(
        String author,
        String systemEmail,
        Duration readTimeout,
        BigDecimal thresholdLimit,
        Currency currency,
        Map<String, Country> supportedCountries) {

    public record Country(String iso3Code, List<ZoneId> timezones) {
    }
}
```

---

Let's create a new Datatype `FullName`, and use it for `author`
```java
@ConfigurationProperties(prefix = "my-service.common-attributes")
public record MyServiceConfiguration(
        FullName author,
        String systemEmail,
        Duration readTimeout,
        BigDecimal thresholdLimit,
        Currency currency,
        Map<String, Country> supportedCountries) {

    public record Country(String iso3Code, List<ZoneId> timezones) {
    }

    public record FullName(String firstName, String lastName) {}
}
```

To convert `String` --> our custom `FullName`, we need to implement `org.springframework.core.convert.converter.Converter`
and register it as a `@ConfigurationPropertiesBinding` and create this bean by adding `@Component`.

```java
@Component
@ConfigurationPropertiesBinding
public class FullNameConverter implements Converter<String, MyServiceConfiguration.FullName> {
    @Override
    public MyServiceConfiguration.FullName convert(String source) {
        String[] parts = source.split(" ");
        return new MyServiceConfiguration.FullName(parts[0], parts[1]);
    }
}
```

**What happened?**

`ConfigurationPropertiesBinder#getBinder()` create its own `binder` with `getConversionServices()`
```java
private Binder getBinder() {
    if (this.binder == null) {
        this.binder = new Binder(getConfigurationPropertySources(), getPropertySourcesPlaceholdersResolver(),
                getConversionServices(), getPropertyEditorInitializer(), null, null);
    }
    return this.binder;
}
```
`getConversionServices()` will find `ConverterBeans` (converters defined as a bean) in `ApplicationContext` and register
them to a `ConversionService` used by the `Binder` above
```java
ConverterBeans converterBeans = new ConverterBeans(applicationContext);

FormattingConversionService beansConverterService = new FormattingConversionService();
converterBeans.addTo(beansConverterService);
```
=> `Binder` --has--> `ConversionService` --has--> our custom `FullNameConverter` converter

## References
- `1.` **Properties Binding** in [**Spring Boot 3**](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties.java-bean-binding).
- `2.` **Constructor Binding** in [**Spring Boot 3**](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties.constructor-binding).
- `3.` **Properties Conversion** in [**Spring Boot 3**](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties.conversion).
