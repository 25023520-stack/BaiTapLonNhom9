package com.auction.system.client;

public enum ClientRunMode {
    DEMO,
    SEPARATE;

    private static final String PROPERTY_NAME = "auction.client.mode";

    public static ClientRunMode current() {
        String value = System.getProperty(PROPERTY_NAME, DEMO.name());
        try {
            return ClientRunMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return DEMO;
        }
    }

    public boolean shouldStartEmbeddedServer() {
        return this == DEMO;
    }

    public static String propertyName() {
        return PROPERTY_NAME;
    }
}
