package com.geekbro.fuelevent.service;

import com.geekbro.fuelevent.config.FuelEventDetectionProperties;
import com.geekbro.fuelevent.detector.FuelEventDetector;
import com.geekbro.fuelevent.exception.FuelFileProcessingException;
import com.geekbro.fuelevent.exception.InvalidFuelFileException;
import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
@AllArgsConstructor
public class FuelEventService {

    private static final Logger log = LoggerFactory.getLogger(FuelEventService.class);

    private final FuelEventDetector detector;

    private final FuelEventDetectionProperties properties;

    public Map<String, List<Double>> processFile(MultipartFile file, Double thresholdOverride) {

        if (file == null || file.isEmpty()) {
            throw new InvalidFuelFileException("Uploaded file is empty.");
        }

        Map<String, List<Double>> result = new LinkedHashMap<>();
        double threshold = thresholdOverride != null ? thresholdOverride : properties.getDefaultThreshold();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            int sheetCount = workbook.getNumberOfSheets();
            if (sheetCount == 0) {
                throw new InvalidFuelFileException("Excel file contains no sheets.");
            }

            boolean anyDataFound = false;

            for (int i = 0; i < sheetCount; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                List<Double> readings = readSingleColumn(sheet, sheet.getSheetName());
                if (!readings.isEmpty()) {
                    anyDataFound = true;
                }
                List<Double> events = detector.detectEvents(readings, threshold);
                result.put(sheet.getSheetName(), events);
            }

            if (!anyDataFound) {
                throw new InvalidFuelFileException(
                        "No numeric data found in any sheet of the uploaded file.");
            }

        } catch (org.apache.poi.EmptyFileException | org.apache.poi.EncryptedDocumentException e) {
            log.warn("Uploaded file '{}' is not a readable Excel file: {}", file.getOriginalFilename(), e.getMessage());
            throw new InvalidFuelFileException("Uploaded file is not a valid, readable Excel file.");
        } catch (IOException e) {
            log.error("I/O error while reading uploaded file '{}'", file.getOriginalFilename(), e);
            throw new FuelFileProcessingException("Failed to read the uploaded file.", e);
        }

        return result;
    }

    private List<Double> readSingleColumn(Sheet sheet, String sheetName) {
        List<Double> values = new ArrayList<>();
        for (Row row : sheet) {
            Cell cell = row.getCell(0);
            if (cell == null) continue;
            if (cell.getCellType() == CellType.NUMERIC) {
                values.add(cell.getNumericCellValue());
            } else if (cell.getCellType() == CellType.STRING) {
                try {
                    values.add(Double.parseDouble(cell.getStringCellValue().trim()));
                } catch (NumberFormatException ignored) {
                    log.debug("Sheet '{}', row {}: skipping non-numeric cell value '{}'",
                            sheetName, row.getRowNum(), cell.getStringCellValue());
                }
            }
        }
        return values;
    }
}
