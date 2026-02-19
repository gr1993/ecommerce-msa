package com.example.mockdelivery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mock-delivery")
public class ApiKeyProperties {

    private Courier courier = new Courier();
    private SmartDelivery smartDelivery = new SmartDelivery();

    public Courier getCourier() {
        return courier;
    }

    public void setCourier(Courier courier) {
        this.courier = courier;
    }

    public SmartDelivery getSmartDelivery() {
        return smartDelivery;
    }

    public void setSmartDelivery(SmartDelivery smartDelivery) {
        this.smartDelivery = smartDelivery;
    }

    public static class Courier {
        private String accountKey;

        public String getAccountKey() {
            return accountKey;
        }

        public void setAccountKey(String accountKey) {
            this.accountKey = accountKey;
        }
    }

    public static class SmartDelivery {
        private String apiKey;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }
}
