// services/notification/src/main/java/com/buildabank/notification/NotificationApplication.java
package com.buildabank.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Step 20 · the event-driven Notification service. It consumes {@code transfer.completed} events from Kafka
 * (published by demand-account's Outbox relay) and pushes them to browsers over Server-Sent Events. No
 * database — its state is the live event stream.
 */
@SpringBootApplication
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
