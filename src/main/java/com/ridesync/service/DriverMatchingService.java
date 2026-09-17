package com.ridesync.service;

import com.ridesync.model.Driver;
import com.ridesync.repository.DriverRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DriverMatchingService {

    private final DriverRepository driverRepository;
    private static final double MAX_SEARCH_RADIUS_KM = 10.0; // 10 km radius

    public DriverMatchingService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    public Optional<Driver> findNearestAvailableDriver(double pickupLat, double pickupLong) {
        List<Driver> availableDrivers = driverRepository.findByAvailableTrue();
        
        Driver nearestDriver = null;
        double minDistance = Double.MAX_VALUE;

        for (Driver driver : availableDrivers) {
            if (driver.getLatitude() != null && driver.getLongitude() != null) {
                double distance = calculateDistance(pickupLat, pickupLong, driver.getLatitude(), driver.getLongitude());
                if (distance <= MAX_SEARCH_RADIUS_KM && distance < minDistance) {
                    minDistance = distance;
                    nearestDriver = driver;
                }
            }
        }
        
        return Optional.ofNullable(nearestDriver);
    }

    // Haversine formula to calculate distance between two lat/long points in km
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; 
    }
}
