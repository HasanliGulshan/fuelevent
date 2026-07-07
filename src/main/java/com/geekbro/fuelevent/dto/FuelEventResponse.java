package com.geekbro.fuelevent.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Getter
public class FuelEventResponse {
    private final Map<String, List<Double>> events;
}
