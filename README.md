# External Properties in Spring Boot 3 @ConfigurationProperties, @Value, Environment

> This repo is used in this Youtube video: https://youtu.be/s_BT8o0KbxQ

> **Noted:** By default, Spring Boot will automatically find and load `application.properties` and `application.yaml` files... - [External Application Properties - Spring Boot](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.files).
> We will follow the default: `application.properties` or `application.yaml` under `main/resources` without changing anything like name (`spring.config.name`),
> location (`spring.config.location`),...

## Getting Started: 
Put a property in our application.properties, we'll play with it in this demo
```properties
my-service.person.first-name=Linh
```

## 1. Get External Properties By `Environment` bean:

`Environment` interface is an abstraction that contains _properties_ from a variety of **sources** (`PropertySource`s) 
like properties files (`application.properties`), JVM system properties (`System.getProperty()`), system environment variables(`System.getenv()`), 
JNDI, servlet context parameters, ad-hoc Properties objects, Map objects, and so on - [Spring Framework - Environment](https://docs.spring.io/spring-framework/reference/core/beans/environment.html)

> **Simply put,** `Environment` bean represents loaded properties from runtime environment, and `Environment` bean contains a list of `PropertySource`s.
> Each `PropertySource` represents for 1 type of properties, for example:
> 
> * Config resource 'class path resource [application.properties]' via location 'optional:classpath:/' `PropertySource` represents our properties in `application.properties` file
> * systemEnvironment `PropertySource` represents the set of system environment variables (`System.getenv()`) (OS environment variables)
> * systemProperties `PropertySource` represents the set of JVM system properties (`System.getProperties()`) (Java System properties)

> **Noted:** the order is also considered here, later `PropertySource`s can override the values defined in earlier ones - [Externalized Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html).
> `systemProperties` > `systemEnvironment` > `Config resource 'class path resource [application.properties]'`

Environment Variable:
```properties
MYSERVICE_PERSON_FIRSTNAME=LinhOSEnv
```
JVM properties
```properties
-DmyService.person.firstName=LinhVMProperties
```

### 1.1. Get `Environment` from `ApplicationContext`:

```java
@Service
public class PeopleService {
    private static final Logger log = LoggerFactory.getLogger(PeopleService.class);

    public PeopleService(ApplicationContext context) {
        Environment environment = context.getEnvironment(); // <== autowire ApplicationContext, then get Environment
        String firstName = environment.getProperty("my-service.person.first-name", String.class, "defaultValue");
        log.info("Property Value: {}", firstName);
    }
}
```

### 1.2. Directly autowire `Environment`:

```java
@Service
public class PeopleService {
    private static final Logger log = LoggerFactory.getLogger(PeopleService.class);

    public PeopleService(Environment environment) {
        String firstName = environment.getProperty("my-service.person.first-name", String.class, "defaultValue");
        log.info("Property Value: {}", firstName);
    }
}
```

## 2. @Value

Using `Environment` is quite cumbersome, we can directly map a property to our variable by [`@Value`](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/value-annotations.html)

### 2.1. @Value with $:

With `@Value` and `$`, our property will be resolved by `PropertySourcesPlaceholderConfigurer` which is a `BeanFactoryPostProcessor` which by default
get properties from `application.properties` and `application.yml` files.

By `field injection`:
```java
@Service
public class PeopleService {
    private static final Logger log = LoggerFactory.getLogger(PeopleService.class);

    @Value("${my-service.person.first-name}")
    private String firstName;

    public PeopleService() {
        log.info("Property Value: {}", firstName);
    }
}
```

By `constructor injection`, it's safer and we can set firstName `final`
```java
@Service
public class PeopleService {
    private static final Logger log = LoggerFactory.getLogger(PeopleService.class);

    private final String firstName;

    public PeopleService(@Value("${my-service.person.first-name}") String firstName) {
        log.info("Property Value: {}", firstName);
        this.firstName = firstName;
    }
}
```

Default value with `:`
```java
@Value("${my-service.person.first-name-non-exist:defaultValue}") String firstName
```

### 2.2. @Value with #:

With `#` followed by `SpEL` ([Spring Expression Language](https://docs.spring.io/spring-framework/reference/core/expressions.html)),
the value will be dynamically computed at runtime

We can refer to our properties by `Environment` or **systemEnvironment** `PropertySource` or **systemProperties** `PropertySource` above

```java
@Service
public class PeopleService {
    private static final Logger log = LoggerFactory.getLogger(PeopleService.class);

    private final String firstName;

    public PeopleService(@Value("#{ environment['my-service.person.first-name'] }") String firstName) {
        log.info("Property Value: {}", firstName);
        this.firstName = firstName;
    }
}
```

With `SpEL`, we can play with the expression itself
```java
@Service
public class PeopleService {
    private static final Logger log = LoggerFactory.getLogger(PeopleService.class);

    private final String firstName;

    public PeopleService(@Value("#{ 'Hello ' + environment['my-service.person.first-name'] }") String firstName) {
        log.info("Property Value: {}", firstName);
        this.firstName = firstName;
    }
}
```

Default value with `?:`
```java
@Value("#{ environment['my-service.person.first-name-non-exist'] ?: 'defaultValue' }") String firstName
```

## 3. @ConfigurationProperties

Using the `@Value("${property}")` annotation to inject configuration properties can sometimes be cumbersome, 
especially if you are working with multiple properties or your data is hierarchical in nature.
`Spring Boot` provides a Type-safe Configuration Properties `@ConfigurationProperties`

Let's extend our `application.properties` a little bit
```properties
my-service.person.first-name=Linh
my-service.person.last-name=Vu
my-service.person.hobbies[0]=sleep
my-service.person.hobbies[1]=listen to music
```

The corresponding in `application.yml`
```yaml
my-service:
  person:
    first-name: Linh
    last-name: Vu
    hobbies:
    - sleep
    - listen to music
```

Since we're having many properties, we can map all of them into a `Class/Record` with `@ConfigurationProperties`.

* First, we have a prefix `my-service.person`
* Second, each attribute of the class will be mapped to `first-name`(String), `last-name`(String), `hobbies`(List<String>) respectively

We end up with
```java
@ConfigurationProperties(prefix = "my-service.person")
public class PeopleConfiguration {
    private String firstname;
    private String lastName;
    private List<String> hobbies;
}
```

**For simplest case,** let's use the `Properties Binding` by declaring `an empty constructor` and `getter/setter for each fields`.
(We're going to jump into details in next videos)

**With that being set up,** we have 3 ways to enable `PeopleConfiguration` bean in order to inject into other `components`, but let's update our service first
```java
@Service
public class PeopleService {
    private static final Logger log = LoggerFactory.getLogger(PeopleService.class);

    public PeopleService(PeopleConfiguration peopleConfiguration) {
        log.info("First Name: {}", peopleConfiguration.getFirstname());
        log.info("Last Name: {}", peopleConfiguration.getLastName());
        log.info("Hobbies: {}", peopleConfiguration.getHobbies());
    }
}
```

1. Add `@EnableConfigurationProperties` with `PeopleConfiguration.class` as a value in any `@Configuration` class, for example:
```java
@SpringBootApplication
@EnableConfigurationProperties(value = {PeopleConfiguration.class})
public class ExternalPropertiesDemoApplication {
```

2. Add `@ConfigurationPropertiesScan` in any `@Configuration` class, for example:
```java
@SpringBootApplication
@ConfigurationPropertiesScan
public class ExternalPropertiesDemoApplication {
```

3. Add Stereotype Annotation like `@Component` at `PeopleConfiguration` class (only works for `Properties Binding`)
```java
@Component
@ConfigurationProperties(prefix = "my-service.person")
public class PeopleConfiguration {
```

## References
- `1.` **Environment** in [**Spring Framework 6**](https://docs.spring.io/spring-framework/reference/core/beans/environment.html#beans-property-source-abstraction).
- `2.` **@Value** in [**Spring Framework 6**](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/value-annotations.html).
- `3.` **Spring Expression Language** in [**Spring Framework 6**](https://docs.spring.io/spring-framework/reference/core/expressions/beandef.html#expressions-beandef-annotation-based).
- `4.` **External Properties** in [**Spring Boot 3**](https://docs.spring.io/spring-boot/reference/features/external-config.html).
