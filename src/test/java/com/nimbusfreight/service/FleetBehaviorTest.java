package com.nimbusfreight.service;

import com.nimbusfreight.domain.AlertStatus;
import com.nimbusfreight.domain.AlertType;
import com.nimbusfreight.domain.TruckStatus;
import com.nimbusfreight.engine.InvalidReadingException;
import com.nimbusfreight.repo.AlertRepository;
import com.nimbusfreight.repo.SensorReadingRepository;
import com.nimbusfreight.web.dto.AlertResponse;
import com.nimbusfreight.web.dto.TruckSummaryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FleetBehaviorTest {

    @Autowired
    private ReadingIngestService ingest;

    @Autowired
    private SeedReplayService replay;

    @Autowired
    private FleetQueryService fleet;

    @Autowired
    private AlertRepository alerts;

    @Autowired
    private SensorReadingRepository readings;

    @Test
    void replaySeedMatchesScenariosAndIsIdempotent() throws Exception {
        SeedReplayService.ReplayResult first = replay.replay();

        assertEquals(54, first.rowsRead());
        assertEquals(54, first.processed());
        assertEquals(0, first.duplicates());
        assertEquals(0, first.storedOutOfOrder());
        assertEquals(0, first.rejectedInvalid());

        TruckSummaryResponse sustained = fleet.truck("T-01");
        TruckSummaryResponse blip = fleet.truck("T-02");
        TruckSummaryResponse steady = fleet.truck("T-03");
        TruckSummaryResponse gap = fleet.truck("T-04");
        TruckSummaryResponse shuffled = fleet.truck("T-05");

        assertEquals(TruckStatus.ALERT_HIGH, sustained.status());
        assertEquals(TruckStatus.OK, blip.status());
        assertEquals(TruckStatus.OK, steady.status());
        assertEquals(TruckStatus.ALERT_HIGH, gap.status());
        assertEquals(TruckStatus.ALERT_HIGH, shuffled.status());

        List<AlertResponse> sustainedAlerts = fleet.alerts(null, "T-01");
        assertEquals(1, sustainedAlerts.size());
        assertEquals(AlertType.HIGH_TEMP, sustainedAlerts.get(0).type());
        assertEquals(AlertStatus.OPEN, sustainedAlerts.get(0).status());
        assertEquals(Instant.parse("2026-07-20T08:10:00Z"), sustainedAlerts.get(0).startedAt());
        assertDecimal("9.6", sustainedAlerts.get(0).peakTempC());

        assertTrue(fleet.alerts(null, "T-02").isEmpty());
        assertTrue(fleet.alerts(null, "T-03").isEmpty());

        List<AlertResponse> gapAlerts = fleet.alerts(null, "T-04");
        assertEquals(1, gapAlerts.size());
        assertEquals(AlertStatus.OPEN, gapAlerts.get(0).status());
        assertEquals(Instant.parse("2026-07-20T08:25:00Z"), gapAlerts.get(0).startedAt());
        assertDecimal("9.1", gapAlerts.get(0).peakTempC());

        List<AlertResponse> shuffledAlerts = fleet.alerts(null, "T-05");
        assertEquals(1, shuffledAlerts.size());
        assertEquals(AlertType.HIGH_TEMP, shuffled.openAlert().type());
        assertEquals(Instant.parse("2026-07-20T08:10:00Z"), shuffledAlerts.get(0).startedAt());
        assertDecimal("9.6", shuffledAlerts.get(0).peakTempC());

        long alertCount = alerts.count();
        long readingCount = readings.count();

        SeedReplayService.ReplayResult second = replay.replay();
        assertEquals(0, second.processed());
        assertEquals(first.processed() + first.duplicates(), second.duplicates());
        assertEquals(alertCount, alerts.count());
        assertEquals(readingCount, readings.count());
    }

    @Test
    void duplicateTimestampIsIgnored() {
        Instant when = Instant.parse("2026-10-02T00:00:00Z");
        assertEquals(ReadingIngestService.IngestResult.PROCESSED, ingest.ingest("T-01", new BigDecimal("4.0"), when));
        assertEquals(ReadingIngestService.IngestResult.DUPLICATE, ingest.ingest("T-01", new BigDecimal("12.0"), when));
        assertEquals(1, readings.findByTruckIdOrderByRecordedAtAsc("T-01").size());
        assertEquals(0, new BigDecimal("4.0").compareTo(readings.findByTruckIdOrderByRecordedAtAsc("T-01").get(0).getTemperatureC()));
    }

    @Test
    void outOfOrderReadingIsStoredButDoesNotMoveState() {
        Instant later = Instant.parse("2026-10-02T02:00:00Z");
        Instant earlier = Instant.parse("2026-10-02T01:00:00Z");
        ingest.ingest("T-01", new BigDecimal("4.0"), later);
        assertEquals(ReadingIngestService.IngestResult.STORED_OUT_OF_ORDER, ingest.ingest("T-01", new BigDecimal("20.0"), earlier));

        assertEquals(2, readings.findByTruckIdOrderByRecordedAtAsc("T-01").size());
        assertTrue(alerts.findByTruckIdOrderByStartedAtDesc("T-01").isEmpty());
        assertEquals(TruckStatus.OK, fleet.truck("T-01").status());
    }

    @Test
    void invalidReadingIsRejected() {
        assertThrows(InvalidReadingException.class,
                () -> ingest.ingest("T-01", new BigDecimal("999"), Instant.parse("2026-10-02T03:00:00Z")));
        assertTrue(readings.findByTruckIdOrderByRecordedAtAsc("T-01").isEmpty());
    }

    private static void assertDecimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
