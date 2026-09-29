package com.ibm.bookstore.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Getter
@Setter
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "bookstore")
public class BookstoreProperties {

    GiftPoints giftPoints = new GiftPoints();
    Shipping shipping = new Shipping();
    Cancellation cancellation = new Cancellation();

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class GiftPoints {
        int rate = 100;
    }

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Shipping {
        BigDecimal flatRate = new BigDecimal("5.00");
        BigDecimal freeThreshold = new BigDecimal("50.00");
    }

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Cancellation {
        int windowHours = 48;
    }
}
