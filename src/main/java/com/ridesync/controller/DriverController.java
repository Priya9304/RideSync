package com.ridesync.controller;

import com.ridesync.dto.LocationUpdateRequest;
import com.ridesync.model.Driver;
import com.ridesync.model.User;
import com.ridesync.repository.DriverRepository;
import com.ridesync.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/driver")
public class DriverController {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;

    public DriverController(DriverRepository driverRepository, UserRepository userRepository) {
        this.driverRepository = driverRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/location")
    public ResponseEntity<?> updateLocation(@RequestBody LocationUpdateRequest request, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        Driver driver = driverRepository.findByUser(user).orElseThrow();

        driver.setLatitude(request.getLatitude());
        driver.setLongitude(request.getLongitude());
        driver.setAvailable(request.isAvailable());

        driverRepository.save(driver);

        return ResponseEntity.ok("Location updated successfully");
    }
}
