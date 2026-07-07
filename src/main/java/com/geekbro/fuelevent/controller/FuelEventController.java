package com.geekbro.fuelevent.controller;

import com.geekbro.fuelevent.service.FuelEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fuel-events")
@AllArgsConstructor
public class FuelEventController {

    private final FuelEventService fuelEventService;

    @Operation(
            summary = "Analyze an Excel file of fuel sensor readings",
            description = "Each sheet must contain a single column of raw, continuous readings for one device/vehicle. " +
                    "Returns, per sheet, an ordered list of detected events: negative = fuel stolen, positive = fuel added."
    )
    @ApiResponse(responseCode = "200", description = "Detected events per sheet",
            content = @Content(schema = @Schema(example = "{ \"FL Steal\": [-6.0, 11.0, 35.0] }")))
    @ApiResponse(responseCode = "400", description = "Invalid or empty file")
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> analyzeFuelFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "threshold", required = false) Double threshold) throws IOException {

            Map<String, List<Double>> events = fuelEventService.processFile(file, threshold);
            return ResponseEntity.ok(events);
    }
}
