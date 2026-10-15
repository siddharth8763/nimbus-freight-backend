package com.nimbusfreight.config;

import com.nimbusfreight.domain.CargoType;
import com.nimbusfreight.entity.Truck;
import com.nimbusfreight.entity.TruckAlertStateEntity;
import com.nimbusfreight.repo.TruckAlertStateRepository;
import com.nimbusfreight.repo.TruckRepository;
import com.nimbusfreight.repo.AlertRepository;
import com.nimbusfreight.repo.SensorReadingRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TruckSeeder implements ApplicationRunner {

    private final TruckRepository trucks;
    private final TruckAlertStateRepository states;
    private final AlertRepository alerts;
    private final SensorReadingRepository readings;

    public TruckSeeder(
            TruckRepository trucks,
            TruckAlertStateRepository states,
            AlertRepository alerts,
            SensorReadingRepository readings
    ) {
        this.trucks = trucks;
        this.states = states;
        this.alerts = alerts;
        this.readings = readings;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        retire("NF-01");
        retire("NF-02");
        retire("NF-03");
        retire("NF-04");
        retire("NF-05");
        seed("T-01", "Sustained", CargoType.PHARMA);
        seed("T-02", "Blip", CargoType.PHARMA);
        seed("T-03", "Steady", CargoType.PHARMA);
        seed("T-04", "Gap", CargoType.PHARMA);
        seed("T-05", "Shuffled", CargoType.PHARMA);
    }

    private void retire(String id) {
        if (!trucks.existsById(id)) {
            return;
        }
        alerts.deleteByTruckId(id);
        readings.deleteByTruckId(id);
        states.deleteById(id);
        trucks.deleteById(id);
    }

    private void seed(String id, String name, CargoType cargoType) {
        if (!trucks.existsById(id)) {
            trucks.save(new Truck(id, name, cargoType));
        }
        if (!states.existsById(id)) {
            states.save(new TruckAlertStateEntity(id));
        }
    }
}
