package com.nimbusfreight.engine;

import com.nimbusfreight.config.AlertPolicy;
import com.nimbusfreight.domain.AlertSink;
import com.nimbusfreight.domain.AlertStatus;
import com.nimbusfreight.domain.AlertTrackState;
import com.nimbusfreight.domain.AlertType;
import com.nimbusfreight.domain.ResolutionReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemperatureAlertEngineTest {

    private final TemperatureAlertEngine engine = new TemperatureAlertEngine();
    private final AlertPolicy policy = new AlertPolicy();
    private MemorySink sink;
    private AlertTrackState state;
    private Instant t0;

    @BeforeEach
    void setUp() {
        sink = new MemorySink();
        state = new AlertTrackState();
        t0 = Instant.parse("2026-10-01T00:00:00Z");
    }

    @Test
    void warmBlipDoesNotAlert() {
        feed(0, "9.2");
        feed(10, "9.4");
        feed(15, "6.0");

        assertTrue(sink.alerts.isEmpty());
        assertNull(state.getWarmSince());
        assertNull(state.getOpenHighAlertId());
    }

    @Test
    void sustainedWarmOpensOnceAndTracksPeak() {
        feed(0, "9.0");
        feed(10, "9.4");
        feed(15, "10.2");
        feed(20, "8.5");

        assertEquals(1, sink.alerts.size());
        MemorySink.Row alert = sink.alerts.get(0);
        assertEquals(AlertType.HIGH_TEMP, alert.type);
        assertEquals(AlertStatus.OPEN, alert.status);
        assertEquals(new BigDecimal("10.2"), alert.extreme);
        assertEquals(t0, alert.startedAt);
        assertEquals(alert.id, state.getOpenHighAlertId());
    }

    @Test
    void exactlyFifteenMinutesOpensWarmAlert() {
        feed(0, "8.5");
        feed(15, "8.5");
        assertEquals(1, sink.alerts.size());
    }

    @Test
    void fourteenMinutesDoesNotOpen() {
        feed(0, "8.5");
        feed(14, "8.5");
        assertTrue(sink.alerts.isEmpty());
    }

    @Test
    void boundaryAtEightCelsiusIsNotWarm() {
        feed(0, "8.0");
        feed(20, "8.0");
        assertTrue(sink.alerts.isEmpty());
        assertNull(state.getWarmSince());
    }

    @Test
    void freezeBlipDoesNotAlert() {
        feed(0, "1.5");
        feed(10, "1.0");
        feed(12, "4.0");
        assertTrue(sink.alerts.isEmpty());
        assertNull(state.getFreezeSince());
    }

    @Test
    void sustainedFreezeOpensOnceAndTracksMin() {
        feed(0, "1.0");
        feed(10, "0.4");
        feed(15, "0.8");
        feed(20, "0.2");

        assertEquals(1, sink.alerts.size());
        MemorySink.Row alert = sink.alerts.get(0);
        assertEquals(AlertType.LOW_TEMP, alert.type);
        assertEquals(AlertStatus.OPEN, alert.status);
        assertEquals(new BigDecimal("0.2"), alert.extreme);
    }

    @Test
    void boundaryAtTwoCelsiusIsNotFreeze() {
        feed(0, "2.0");
        feed(20, "2.0");
        assertTrue(sink.alerts.isEmpty());
        assertNull(state.getFreezeSince());
    }

    @Test
    void warmAlertResolvesAfterHysteresis() {
        feed(0, "9.0");
        feed(15, "9.0");
        feed(20, "6.5");
        feed(30, "6.4");

        assertEquals(1, sink.alerts.size());
        assertEquals(AlertStatus.RESOLVED, sink.alerts.get(0).status);
        assertEquals(ResolutionReason.NORMAL, sink.alerts.get(0).reason);
        assertNull(state.getOpenHighAlertId());
    }

    @Test
    void newWarmEpisodeAfterResolveCreatesAnotherAlert() {
        feed(0, "9.0");
        feed(15, "9.0");
        feed(20, "6.5");
        feed(30, "6.4");
        feed(40, "9.1");
        feed(55, "9.4");

        assertEquals(2, sink.alerts.size());
        assertEquals(AlertStatus.RESOLVED, sink.alerts.get(0).status);
        assertEquals(AlertStatus.OPEN, sink.alerts.get(1).status);
        assertEquals(AlertType.HIGH_TEMP, sink.alerts.get(1).type);
    }

    @Test
    void gapLongerThanThirtyMinutesResetsWarmTimer() {
        feed(0, "9.0");
        feed(10, "9.0");
        feed(41, "9.0");
        feed(50, "9.0");

        assertTrue(sink.alerts.isEmpty());
        assertEquals(t0.plusSeconds(41 * 60), state.getWarmSince());
    }

    @Test
    void gapOfExactlyThirtyMinutesDoesNotReset() {
        feed(0, "9.0");
        feed(10, "9.0");
        feed(40, "9.0");

        assertEquals(1, sink.alerts.size());
        assertEquals(t0, sink.alerts.get(0).startedAt);
    }

    @Test
    void freezeSustainedWhileWarmAlertOpenSupersedesIt() {
        feed(0, "9.0");
        feed(15, "9.0");
        feed(20, "1.0");
        feed(35, "0.5");

        assertEquals(2, sink.alerts.size());
        assertEquals(AlertStatus.RESOLVED, sink.alerts.get(0).status);
        assertEquals(ResolutionReason.SUPERSEDED, sink.alerts.get(0).reason);
        assertEquals(AlertType.LOW_TEMP, sink.alerts.get(1).type);
        assertEquals(AlertStatus.OPEN, sink.alerts.get(1).status);
        assertNull(state.getOpenHighAlertId());
        assertEquals(sink.alerts.get(1).id, state.getOpenLowAlertId());
    }

    @Test
    void shortFreezeBlipDoesNotResolveOpenWarmAlert() {
        feed(0, "9.0");
        feed(15, "9.0");
        feed(20, "1.0");
        feed(25, "4.0");

        assertEquals(1, sink.alerts.size());
        assertEquals(AlertStatus.OPEN, sink.alerts.get(0).status);
        assertEquals(AlertType.HIGH_TEMP, sink.alerts.get(0).type);
    }

    private void feed(int plusMinutes, String temp) {
        engine.evaluate(state, t0.plusSeconds(plusMinutes * 60L), new BigDecimal(temp), policy, sink);
    }

    private static final class MemorySink implements AlertSink {
        private final List<Row> alerts = new ArrayList<>();
        private long nextId = 1;

        @Override
        public long open(AlertType type, Instant startedAt, BigDecimal extremeTemp) {
            Row row = new Row();
            row.id = nextId++;
            row.type = type;
            row.status = AlertStatus.OPEN;
            row.startedAt = startedAt;
            row.extreme = extremeTemp;
            alerts.add(row);
            return row.id;
        }

        @Override
        public void resolve(long alertId, Instant resolvedAt, ResolutionReason reason) {
            Row row = find(alertId);
            row.status = AlertStatus.RESOLVED;
            row.resolvedAt = resolvedAt;
            row.reason = reason;
        }

        @Override
        public void updateExtreme(long alertId, BigDecimal temp, AlertType type) {
            Row row = find(alertId);
            if (type == AlertType.HIGH_TEMP) {
                if (temp.compareTo(row.extreme) > 0) {
                    row.extreme = temp;
                }
            } else if (temp.compareTo(row.extreme) < 0) {
                row.extreme = temp;
            }
        }

        private Row find(long alertId) {
            return alerts.stream().filter(row -> row.id == alertId).findFirst().orElseThrow();
        }

        private static final class Row {
            private long id;
            private AlertType type;
            private AlertStatus status;
            private Instant startedAt;
            private Instant resolvedAt;
            private BigDecimal extreme;
            private ResolutionReason reason;
        }
    }
}
