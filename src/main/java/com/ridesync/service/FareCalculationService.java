package com.ridesync.service;

import org.springframework.stereotype.Service;

@Service
public class FareCalculationService {

    private static final double BASE_FARE = 5.0; // $5 base fare
    private static final double PER_KM_RATE = 1.5; // $1.5 per km
    private static final double MINIMUM_FARE = 7.0;

    // In a real app, this would query active requests to determine demand
    private double currentSurgeMultiplier = 1.0;

    public double calculateFare(double distanceKm) {
        double calculatedFare = BASE_FARE + (distanceKm * PER_KM_RATE);
        double finalFare = calculatedFare * currentSurgeMultiplier;
        
        return Math.max(finalFare, MINIMUM_FARE);
    }
    
    // Simulate changing surge (for testing/admin)
    public void setSurgeMultiplier(double multiplier) {
        this.currentSurgeMultiplier = multiplier;
    }
    
    public double getCurrentSurgeMultiplier() {
        return currentSurgeMultiplier;
    }
}
