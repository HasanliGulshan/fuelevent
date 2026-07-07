package com.geekbro.fuelevent.controller;

import com.geekbro.fuelevent.dto.FuelEventResponse;
import com.geekbro.fuelevent.service.FuelEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/fuel-events")
@Tag(name = "Fuel Events", description = "Detects refuel and theft events from raw fuel sensor data")
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
    public ResponseEntity<FuelEventResponse> analyzeFuelFile(
            @Parameter(description = "Excel (.xlsx) file to analyze", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Optional noise threshold override (raw sensor units). Must be positive.")
            @Positive(message = "threshold must be greater than zero")
            @RequestParam(value = "threshold", required = false) Double threshold) {

        FuelEventResponse response = new FuelEventResponse(fuelEventService.processFile(file, threshold));
        return ResponseEntity.ok(response);
    }
}
