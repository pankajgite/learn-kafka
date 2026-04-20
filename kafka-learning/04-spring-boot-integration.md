# 4. Configuring Kafka with Spring Boot

Spring Boot makes integrating Kafka incredibly straightforward. This document covers the essential configurations, with examples drawn from our `user-service`.

---

## 🏗️ application.yml Explanation

Your `application.yml` is the command center for your Kafka setup.

```yaml
spring:
  kafka:
    # 1. Broker Address
    bootstrap-servers: localhost:9092

    # 2. Producer Configuration
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer # Or LongSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

    # 3. Consumer Configuration
    consumer:
      group-id: user-service-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer # Or LongDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*" # Crucial for JSON deserialization

kafka:
  topic:
    user-random-topic:
      name: user_random_topic
    user-created-topic:
      name: user_created_topic
```

*   **`bootstrap-servers`**: The address of your Kafka broker(s).
*   **`group-id`**: Groups consumers together. Essential for load balancing.
*   **`auto-offset-reset`**: If a consumer starts and there's no initial offset (or the offset is deleted), what should it do?
    *   `earliest`: Read from the beginning of the partition. Good for not missing data.
    *   `latest`: Only read new messages that arrive after the consumer starts.
*   **`spring.json.trusted.packages: "*"`**: A security measure. It tells Spring it's safe to deserialize JSON into classes from any package.

---

## 📤 Producer Setup

To send messages, you use `KafkaTemplate`.

### The Generic Way (Strings)

If you only need to send strings, Spring Boot auto-configures a `KafkaTemplate<String, String>` for you. You can simply inject it:

```java
// UserController.java
@RestController
@RequiredArgsConstructor
public class UserController {
    private final KafkaTemplate<String, String> kafkaTemplate;

    @PostMapping("/{message}")
    public void sendMessage(@PathVariable String message) {
        kafkaTemplate.send("my-topic", "key", message);
    }
}
```

### The Custom Way (Objects/JSON)

If you are sending objects (like `UserCreatedEvent`), Spring needs to know how to serialize them. If you define a custom `KafkaTemplate`, you often need to provide a custom `ProducerFactory`.

```java
// KafkaProducerConfig.java
@Configuration
public class KafkaProducerConfig {

    // You define the rules: Key is Long, Value is JSON
    @Bean
    public ProducerFactory<Long, UserCreatedEvent> userCreatedProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    // You create the template using those rules
    @Bean
    public KafkaTemplate<Long, UserCreatedEvent> userCreatedKafkaTemplate() {
        return new KafkaTemplate<>(userCreatedProducerFactory());
    }
}
```

*   *See `KafkaProducerConfig.java` in `user-service` for this exact implementation.*

---

## 📥 Consumer Setup

To receive messages, you use the `@KafkaListener` annotation on a method.

```java
// UserConsumerService.java (Example)
@Service
@Slf4j
public class UserConsumerService {

    @KafkaListener(topics = "${kafka.topic.user-created-topic.name}", groupId = "user-service-group")
    public void consumeUserCreatedEvent(UserCreatedEvent event) {
        log.info("Received event for user: {}", event.getName());
        // Process the event...
    }
}
```

Spring handles the connection, pulling messages, and converting the JSON payload back into a `UserCreatedEvent` object (thanks to the deserializers configured in `application.yml`).

---

## ⚠️ Common Errors and Fixes

1.  **Error: `No qualifying bean of type 'org.springframework.kafka.core.KafkaTemplate<String, String>' available`**
    *   **Cause:** You defined a custom `KafkaTemplate` (e.g., `<Long, Object>`), which overrode Spring's default `<String, String>` template.
    *   **Fix:** Define the `<String, String>` template manually in your config class and annotate it with `@Primary`. (We did this in `KafkaProducerConfig.java`).

2.  **Error: `IllegalArgumentException: The class 'X' is not in the trusted packages:`**
    *   **Cause:** Spring's `JsonDeserializer` refuses to create an object from a package it doesn't trust.
    *   **Fix:** Add `spring.kafka.consumer.properties.spring.json.trusted.packages: "*"` (or the specific package name) to your `application.yml`.

3.  **Error: Cannot deserialize from generic JSON to specific object.**
    *   **Cause:** The producer sent an object, but the consumer doesn't know what class to map it to.
    *   **Fix:** The producer must include type information in headers, OR the consumer must explicitly configure `value-default-type` in the deserializer properties.

---

## 🔁 Revision Corner

### ✅ Quick Summary

*   **`application.yml`** configures serializers, deserializers, broker URLs, and consumer groups.
*   **`KafkaTemplate`** is used to send messages. You can use the default or build custom ones.
*   **`@KafkaListener`** is used to receive messages. Spring handles the boilerplate.
*   **JSON Serialization** requires careful configuration of serializers/deserializers and trusted packages.
*   Customizing a `KafkaTemplate` often disables Spring's default template.

### 🧠 Interview Questions

1.  **How do you consume messages in a Spring Boot application?**
    *   By adding the `@KafkaListener` annotation to a method and specifying the topic and group ID. Spring's message listener container manages the polling.

2.  **What happens if a custom `KafkaTemplate` bean is defined in Spring Boot?**
    *   Spring Boot's auto-configuration backs off. It won't create the default `KafkaTemplate<String, String>`, which can lead to injection errors if other parts of the app rely on it.

3.  **Why do you need `spring.json.trusted.packages` in your consumer config?**
    *   It's a security feature. It prevents attackers from sending malicious JSON payloads that could be deserialized into arbitrary classes on the classpath, potentially executing harmful code.

### 📌 Key Takeaways

*   Spring Boot abstractions (`KafkaTemplate`, `@KafkaListener`) hide the complexity of the native Kafka Java client.
*   Serialization configuration must perfectly match between producer and consumer.
*   Defining custom beans overrides defaults; be prepared to manually configure what you replaced.
*   Security settings (`trusted.packages`) are crucial for JSON handling in Spring Kafka.
