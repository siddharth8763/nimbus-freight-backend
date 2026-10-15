package com.nimbusfreight.repo;

import com.nimbusfreight.domain.AlertStatus;
import com.nimbusfreight.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findAllByOrderByStartedAtDesc();

    List<Alert> findByStatusOrderByStartedAtDesc(AlertStatus status);

    List<Alert> findByTruckIdOrderByStartedAtDesc(String truckId);

    void deleteByTruckId(String truckId);
}
