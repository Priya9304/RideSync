package com.ridesync.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FareCalculationServiceTest {

    private FareCalculationService fareCalculationService;

    @BeforeEach
    void setUp() {
        fareCalculationService = new FareCalculationService();
    }

    @Test
    void testCalculateFare_noSurge() {
        // distance = 10 km
        // Base: 5.0 + (10 * 1.5) = 20.0
        double fare = fareCalculationService.calculateFare(10.0);
        assertEquals(20.0, fare);
    }

    @Test
    void testCalculateFare_withSurge() {
        fareCalculationService.setSurgeMultiplier(1.5);
        // distance = 10 km
        // (5.0 + 15.0) * 1.5 = 30.0
        double fare = fareCalculationService.calculateFare(10.0);
        assertEquals(30.0, fare);
    }

    @Test
    void testCalculateFare_minimumFareApplied() {
        // distance = 1 km
        // 5.0 + 1.5 = 6.5. Minimum is 7.0
        double fare = fareCalculationService.calculateFare(1.0);
        assertEquals(7.0, fare);
    }
}
