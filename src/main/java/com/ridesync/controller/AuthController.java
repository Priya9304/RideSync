package com.ridesync.controller;

import com.ridesync.dto.AuthRequest;
import com.ridesync.dto.AuthResponse;
import com.ridesync.dto.RegisterRequest;
import com.ridesync.model.Driver;
import com.ridesync.model.Rider;
import com.ridesync.model.Role;
import com.ridesync.model.User;
import com.ridesync.repository.DriverRepository;
import com.ridesync.repository.RiderRepository;
import com.ridesync.repository.UserRepository;
import com.ridesync.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RiderRepository riderRepository;
    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager, UserRepository userRepository,
                          RiderRepository riderRepository, DriverRepository driverRepository,
                          PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.riderRepository = riderRepository;
        this.driverRepository = driverRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwt = jwtUtil.generateToken(userDetails);
        
        User user = userRepository.findByEmail(authRequest.getEmail()).orElseThrow();

        return ResponseEntity.ok(new AuthResponse(jwt, user.getEmail(), user.getRole().name()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Error: Email is already taken!");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());

        userRepository.save(user);

        if (request.getRole() == Role.ROLE_RIDER) {
            Rider rider = new Rider();
            rider.setUser(user);
            rider.setName(request.getName());
            rider.setPhone(request.getPhone());
            riderRepository.save(rider);
        } else if (request.getRole() == Role.ROLE_DRIVER) {
            Driver driver = new Driver();
            driver.setUser(user);
            driver.setName(request.getName());
            driver.setPhone(request.getPhone());
            driver.setLicenseNumber(request.getLicenseNumber());
            driverRepository.save(driver);
        }

        return ResponseEntity.ok("User registered successfully!");
    }
}
