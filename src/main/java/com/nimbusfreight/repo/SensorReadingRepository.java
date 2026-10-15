package com.nimbusfreight.repo;

import com.nimbusfreight.entity.SensorReading;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    boolean existsByTruckIdAndRecordedAt(String truckId, Instant recordedAt);

    Optional<SensorReading> findFirstByTruckIdOrderByRecordedAtDesc(String truckId);

    List<SensorReading> findByTruckIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            String truckId, Instant from, Instant to);

    List<SensorReading> findByTruckIdOrderByRecordedAtAsc(String truckId);

    void deleteByTruckId(String truckId);
}
