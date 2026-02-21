package com.example.shippingservice.shipping.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CarrierCode {
    POST_OFFICE("01", "우체국택배"),
    CJ_LOGISTICS("04", "CJ대한통운"),
    HANJIN("05", "한진택배"),
    LOGEN("06", "로젠택배"),
    LOTTE("08", "롯데택배");

    private final String code;
    private final String name;

    public static CarrierCode fromCode(String code) {
        for (CarrierCode carrier : values()) {
            if (carrier.code.equals(code)) {
                return carrier;
            }
        }
        throw new IllegalArgumentException("Unknown carrier code: " + code);
    }

    public static CarrierCode fromName(String name) {
        for (CarrierCode carrier : values()) {
            if (carrier.name.equals(name)) {
                return carrier;
            }
        }
        throw new IllegalArgumentException("Unknown carrier name: " + name);
    }
}
