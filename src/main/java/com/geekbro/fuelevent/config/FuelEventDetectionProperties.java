package com.geekbro.fuelevent.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fuel-event.detection")
@Getter
@Setter
public class FuelEventDetectionProperties {
    private double defaultThreshold = 5.0;
    private int holdSamples = 3;
    private int groupGap = 3;
    private int maxRampExtension = 10;
}
