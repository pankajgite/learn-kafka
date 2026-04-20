# 3. Installing Kafka

This guide provides a minimal, modern setup for running Kafka locally using KRaft mode, which simplifies deployment by removing the need for a separate ZooKeeper cluster.

---

## 🛠️ Kafka Setup (KRaft Mode)

Starting with Kafka 3.x, KRaft mode is the recommended way to run Kafka. It uses an internal quorum of controllers instead of ZooKeeper.

### Step 1: Download and Extract Kafka

1.  Go to the [official Apache Kafka downloads page](https://kafka.apache.org/downloads).
2.  Download the latest binary release (e.g., `kafka_2.13-3.7.0.tgz`).
3.  Extract the archive:
    ```bash
    tar -xzf kafka_2.13-3.7.0.tgz
    cd kafka_2.13-3.7.0
    ```

### Step 2: Generate a Cluster ID

Before starting, Kafka needs a unique ID for the new cluster.

```bash
KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
```

### Step 3: Format the Log Directories

This command initializes the metadata for the log directories using the cluster ID.

```bash
bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/kraft/server.properties
```

### Step 4: Start the Kafka Server

Now, start the Kafka broker. It will run in the background.

```bash
bin/kafka-server-start.sh -daemon config/kraft/server.properties
```

Your Kafka server is now running on `localhost:9092`.

---

## 🎨 Kafbat Setup (UI Tool)

Kafbat (formerly Kafka UI) is an excellent open-source web UI for managing and monitoring Kafka.

### Step 1: Use Docker Compose

The easiest way to run Kafbat is with Docker. Create a `docker-compose.yml` file:

```yaml
version: '3.8'
services:
  kafbat:
    image: provectuslabs/kafbat:latest
    container_name: kafbat
    ports:
      - "8080:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: "host.docker.internal:9092" # For connecting from Docker to host
    extra_hosts:
      - "host.docker.internal:host-gateway"
```

### Step 2: Run Kafbat

Navigate to the directory with your `docker-compose.yml` and run:

```bash
docker-compose up -d
```

Open your browser to `http://localhost:8080`. You should see your Kafka cluster and be able to explore topics, messages, and consumers.

---

## 🏃‍♂️ Running Multiple Brokers (Basic Explanation)

Running multiple brokers locally simulates a real distributed cluster.

1.  **Copy Config:** Create copies of `config/kraft/server.properties` (e.g., `server-1.properties`, `server-2.properties`).
2.  **Edit Configs:** In each new file, you **must** change these properties to be unique:
    *   `node.id`: A unique number for each broker (e.g., 2, 3).
    *   `listeners`: A unique port for each broker (e.g., `PLAINTEXT://localhost:9093`, `PLAINTEXT://localhost:9094`).
    *   `log.dirs`: A unique directory for each broker's logs (e.g., `/tmp/kafka-logs-1`, `/tmp/kafka-logs-2`).
3.  **Format and Start:** Format the new log directories and start each broker using its respective config file.

This setup allows you to test fault tolerance by stopping and starting brokers to see how leaders and followers behave.

---

## 🔁 Revision Corner

### ✅ Quick Summary

*   **KRaft mode** is the modern way to run Kafka without ZooKeeper.
*   The setup process is: **Download -> Generate UUID -> Format -> Start**.
*   **Kafbat** is a UI tool that runs in Docker and helps visualize your cluster.
*   To run multiple brokers, you need to provide a unique `node.id`, `listeners` (port), and `log.dirs` for each one.

### 🧠 Interview Questions

1.  **What is KRaft mode in Kafka?**
    *   It's Kafka's built-in consensus mechanism that replaces the dependency on Apache ZooKeeper. It simplifies the architecture and improves performance.

2.  **What is the first step you need to take before starting a new KRaft cluster?**
    *   You must generate a unique cluster UUID and use it to format the storage directories. This initializes the cluster's metadata.

3.  **Why would you run multiple brokers locally?**
    *   To simulate a production environment, test fault tolerance (what happens when a broker fails), and observe partition replication in action.

### 📌 Key Takeaways

*   KRaft simplifies Kafka's operational overhead significantly.
*   A UI tool like Kafbat is essential for easy management and debugging.
*   Always use `host.docker.internal` to connect from a Docker container to a service running on your local machine.
*   Running multiple brokers is just a matter of copying and editing config files with unique ports and IDs.
*   Don't forget to format the log directories before starting a new broker for the first time.
