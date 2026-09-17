package com.ridesync.controller;

import com.ridesync.model.Ride;
import com.ridesync.model.RideStatus;
import com.ridesync.repository.RideRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final RideRepository rideRepository;

    public AdminController(RideRepository rideRepository) {
        this.rideRepository = rideRepository;
    }

    @GetMapping("/rides")
    public ResponseEntity<List<Ride>> getAllRides() {
        return ResponseEntity.ok(rideRepository.findAll());
    }

    @PutMapping("/rides/{rideId}/cancel")
    public ResponseEntity<Ride> cancelRide(@PathVariable Long rideId) {
        Ride ride = rideRepository.findById(rideId).orElseThrow(() -> new RuntimeException("Ride not found"));
        ride.setStatus(RideStatus.CANCELLED);
        ride = rideRepository.save(ride);
        return ResponseEntity.ok(ride);
    }
}
