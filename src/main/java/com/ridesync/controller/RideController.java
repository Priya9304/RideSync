package com.ridesync.controller;

import com.ridesync.dto.RideRequestDto;
import com.ridesync.model.Ride;
import com.ridesync.model.RideStatus;
import com.ridesync.service.RideService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ride")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping("/request")
    public ResponseEntity<Ride> requestRide(@RequestBody RideRequestDto requestDto, Authentication authentication) {
        String riderEmail = authentication.getName();
        Ride ride = rideService.requestRide(riderEmail, requestDto);
        return ResponseEntity.ok(ride);
    }

    @PutMapping("/{rideId}/status")
    public ResponseEntity<Ride> updateRideStatus(@PathVariable Long rideId, @RequestParam RideStatus status, Authentication authentication) {
        String driverEmail = authentication.getName();
        Ride ride = rideService.updateRideStatus(rideId, driverEmail, status);
        return ResponseEntity.ok(ride);
    }

    @GetMapping("/driver")
    public ResponseEntity<java.util.List<Ride>> getDriverRides(Authentication authentication) {
        String driverEmail = authentication.getName();
        return ResponseEntity.ok(rideService.getDriverRides(driverEmail));
    }
}
