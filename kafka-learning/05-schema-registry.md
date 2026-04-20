# 5. Kafka Schema Registry

This document introduces Schema Registry, a crucial component for maintaining data consistency in production Kafka environments.

---

## 🧐 What is Schema Registry?

Schema Registry is a centralized service (outside of the Kafka brokers) that stores and serves schemas (like Avro, Protobuf, or JSON Schema) for your Kafka messages.

Think of it as a **database of contracts** between your producers and consumers.

---

## 🚦 Why is it Needed?

Without Schema Registry, Kafka is just a dumb pipe. It takes bytes from a producer and gives bytes to a consumer. It doesn't know or care what those bytes represent.

**The Problem:**
1.  **Producer V1** sends JSON: `{"userId": 123, "name": "Alice"}`.
2.  **Consumer V1** expects: `{"userId": 123, "name": "Alice"}`. Everything works fine.
3.  **Producer V2** updates its code and sends: `{"user_id": 123, "fullName": "Alice"}`.
4.  **Consumer V1** breaks because it's still looking for `userId` and `name`.

**The Solution (Schema Registry):**
Schema Registry forces the producer and consumer to agree on a contract (the schema) *before* data is sent or read.

*   **Producer:** "Hey Registry, I want to send data using this schema. Is it allowed?"
    *   *Registry: "Yes, here's schema ID 42."*
    *   Producer sends the data with the ID `42` attached, not the full schema. This saves massive amounts of bandwidth.
*   **Consumer:** "I received data with schema ID 42. Hey Registry, what does schema 42 look like?"
    *   *Registry: "Here is the schema."*
    *   Consumer successfully deserializes the data.

Crucially, the Schema Registry enforces **compatibility rules**. It will reject Producer V2's new schema if it would break Consumer V1.

---

## 🏃‍♂️ Steps to Run Locally

You can run Schema Registry alongside Kafka using Docker Compose. (Confluent provides the most common Schema Registry).

1.  **Add to `docker-compose.yml`:**

```yaml
version: '3.8'
services:
  # ... (your kafka service here)

  schema-registry:
    image: confluentinc/cp-schema-registry:7.4.0
    hostname: schema-registry
    container_name: schema-registry
    depends_on:
      - kafka
    ports:
      - "8081:8081"
    environment:
      SCHEMA_REGISTRY_HOST_NAME: schema-registry
      SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS: 'kafka:9092' # Point to your kafka broker
```

2.  **Start it:** `docker-compose up -d`

Schema Registry will now be available at `http://localhost:8081`.

---

## 🛠️ Basic Usage Concept (Avro in Spring Boot)

When using Schema Registry, **Avro** is the most common data format.

1.  **Define Schema (.avsc):** You write a JSON-like file defining your object.
    ```json
    {
      "namespace": "com.example.avro",
      "type": "record",
      "name": "UserEvent",
      "fields": [
        {"name": "id", "type": "long"},
        {"name": "name", "type": "string"}
      ]
    }
    ```
2.  **Generate Classes:** You use the `avro-maven-plugin` (which is already in your `user-service/pom.xml`) to generate Java classes from this `.avsc` file during the build process.
3.  **Configure Spring:** You tell your producer and consumer to use the `KafkaAvroSerializer` and point them to the Schema Registry URL (`http://localhost:8081`).

---

## 🔁 Revision Corner

### ✅ Quick Summary

*   **Schema Registry** is a centralized store for message schemas (contracts).
*   It prevents producers from breaking consumers by enforcing compatibility rules.
*   It saves bandwidth because producers only send a schema ID, not the full schema, with every message.
*   It's typically used with **Avro**, Protobuf, or JSON Schema formats.
*   It runs as a separate service, often alongside Kafka brokers.

### 🧠 Interview Questions

1.  **What problem does Schema Registry solve?**
    *   It solves the "broken contract" problem in data pipelines. It ensures that changes to data structures by producers don't crash downstream consumers by enforcing schema evolution rules.

2.  **How does Schema Registry reduce network traffic?**
    *   Instead of embedding the entire schema in every Kafka message, the producer only includes a small integer (the Schema ID). The consumer fetches the full schema from the registry using that ID and caches it.

3.  **What is Avro, and how does it relate to Schema Registry?**
    *   Avro is a data serialization system. It defines schemas using JSON and serializes data into a compact binary format. It's the most widely supported format for Confluent's Schema Registry.

### 📌 Key Takeaways

*   Kafka doesn't care about your data's structure; Schema Registry does.
*   Always use a Schema Registry in production to avoid late-night data parsing errors.
*   Use the `avro-maven-plugin` to generate Java code from schemas to ensure type safety in Spring Boot.
*   Schema Registry provides a safety net for evolving data models over time.
