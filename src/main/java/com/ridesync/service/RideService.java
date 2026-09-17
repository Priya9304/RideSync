package com.ridesync.service;

import com.ridesync.dto.RideRequestDto;
import com.ridesync.model.*;
import com.ridesync.repository.DriverRepository;
import com.ridesync.repository.RideRepository;
import com.ridesync.repository.RiderRepository;
import com.ridesync.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class RideService {

    private final RideRepository rideRepository;
    private final RiderRepository riderRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final DriverMatchingService driverMatchingService;
    private final FareCalculationService fareCalculationService;
    private final SimpMessagingTemplate messagingTemplate;

    public RideService(RideRepository rideRepository, RiderRepository riderRepository,
                       DriverRepository driverRepository, UserRepository userRepository,
                       DriverMatchingService driverMatchingService,
                       FareCalculationService fareCalculationService,
                       SimpMessagingTemplate messagingTemplate) {
        this.rideRepository = rideRepository;
        this.riderRepository = riderRepository;
        this.driverRepository = driverRepository;
        this.userRepository = userRepository;
        this.driverMatchingService = driverMatchingService;
        this.fareCalculationService = fareCalculationService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public Ride requestRide(String riderEmail, RideRequestDto requestDto) {
        User user = userRepository.findByEmail(riderEmail).orElseThrow();
        Rider rider = riderRepository.findByUser(user).orElseThrow();

        // Simulate distance calculation since we are using strings instead of coordinates
        double distance = 2.0 + (Math.random() * 13.0); // Random distance between 2 and 15 km
        
        double fare = fareCalculationService.calculateFare(distance);

        Ride ride = new Ride();
        ride.setRider(rider);
        ride.setPickupLocation(requestDto.getPickupLocation());
        ride.setDropoffLocation(requestDto.getDropoffLocation());
        ride.setServiceType(requestDto.getServiceType());
        ride.setDistance(distance);
        ride.setFare(fare);
        ride.setStatus(RideStatus.REQUESTED);

        // Find any available driver since we aren't doing geocoordinate math
        java.util.List<Driver> availableDrivers = driverRepository.findByAvailableTrue();
        if (!availableDrivers.isEmpty()) {
            Driver driver = availableDrivers.get(0);
            ride.setDriver(driver);
        }

        ride = rideRepository.save(ride);
        
        // Notify via WebSocket
        messagingTemplate.convertAndSend("/topic/rides/" + ride.getId(), ride);

        return ride;
    }

    @Transactional
    public Ride updateRideStatus(Long rideId, String driverEmail, RideStatus newStatus) {
        Ride ride = rideRepository.findById(rideId).orElseThrow(() -> new RuntimeException("Ride not found"));
        
        User user = userRepository.findByEmail(driverEmail).orElseThrow();
        Driver driver = driverRepository.findByUser(user).orElseThrow();

        if (ride.getDriver() != null && !ride.getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("Unauthorized: Not your ride");
        }

        ride.setStatus(newStatus);
        
        if (newStatus == RideStatus.ACCEPTED || newStatus == RideStatus.ONGOING) {
            driver.setAvailable(false);
            driverRepository.save(driver);
        } else if (newStatus == RideStatus.COMPLETED || newStatus == RideStatus.CANCELLED) {
            driver.setAvailable(true);
            driverRepository.save(driver);
        }

        ride = rideRepository.save(ride);
        messagingTemplate.convertAndSend("/topic/rides/" + ride.getId(), ride);

        return ride;
    }

    @Transactional(readOnly = true)
    public java.util.List<Ride> getDriverRides(String driverEmail) {
        User user = userRepository.findByEmail(driverEmail).orElseThrow();
        Driver driver = driverRepository.findByUser(user).orElseThrow();
        return rideRepository.findByDriver(driver);
    }
}
