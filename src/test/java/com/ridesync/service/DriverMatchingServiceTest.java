package com.ridesync.service;

import com.ridesync.model.Driver;
import com.ridesync.repository.DriverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverMatchingServiceTest {

    @Mock
    private DriverRepository driverRepository;

    @InjectMocks
    private DriverMatchingService driverMatchingService;

    private Driver driver1;
    private Driver driver2;

    @BeforeEach
    void setUp() {
        driver1 = new Driver();
        driver1.setId(1L);
        driver1.setLatitude(40.7128); // NY
        driver1.setLongitude(-74.0060);

        driver2 = new Driver();
        driver2.setId(2L);
        driver2.setLatitude(40.7306); // Also NY, slightly further
        driver2.setLongitude(-73.9866);
    }

    @Test
    void testFindNearestAvailableDriver() {
        when(driverRepository.findByAvailableTrue()).thenReturn(Arrays.asList(driver2, driver1));

        // Pickup point exactly at driver1's location
        Optional<Driver> nearestDriverOpt = driverMatchingService.findNearestAvailableDriver(40.7128, -74.0060);

        assertTrue(nearestDriverOpt.isPresent());
        assertEquals(1L, nearestDriverOpt.get().getId());
    }

    @Test
    void testFindNearestAvailableDriver_NoDriversWithinRadius() {
        driver1.setLatitude(34.0522); // LA, far away
        driver1.setLongitude(-118.2437);
        
        when(driverRepository.findByAvailableTrue()).thenReturn(List.of(driver1));

        // Pickup point in NY
        Optional<Driver> nearestDriverOpt = driverMatchingService.findNearestAvailableDriver(40.7128, -74.0060);

        assertTrue(nearestDriverOpt.isEmpty());
    }
    
    @Test
    void testCalculateDistance() {
        // Distance from NY to LA should be roughly 3935 km
        double distance = driverMatchingService.calculateDistance(40.7128, -74.0060, 34.0522, -118.2437);
        assertTrue(distance > 3900 && distance < 4000);
    }
}
