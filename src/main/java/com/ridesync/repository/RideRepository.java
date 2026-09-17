package com.ridesync.repository;

import com.ridesync.model.Ride;
import com.ridesync.model.Rider;
import com.ridesync.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findByRider(Rider rider);
    List<Ride> findByDriver(Driver driver);
}
