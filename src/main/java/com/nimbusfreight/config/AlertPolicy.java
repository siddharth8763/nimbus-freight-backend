package com.nimbusfreight.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "nimbus.alert")
public class AlertPolicy {

    private BigDecimal thresholdHighC = new BigDecimal("8.0");
    private BigDecimal thresholdHighResolveC = new BigDecimal("7.0");
    private BigDecimal thresholdFreezeC = new BigDecimal("2.0");
    private BigDecimal thresholdFreezeResolveC = new BigDecimal("3.0");
    private int sustainDurationMinutes = 15;
    private int resolveDurationMinutes = 10;
    private int gapResetMinutes = 30;
    private BigDecimal minValidTempC = new BigDecimal("-50");
    private BigDecimal maxValidTempC = new BigDecimal("50");

    public BigDecimal getThresholdHighC() {
        return thresholdHighC;
    }

    public void setThresholdHighC(BigDecimal thresholdHighC) {
        this.thresholdHighC = thresholdHighC;
    }

    public BigDecimal getThresholdHighResolveC() {
        return thresholdHighResolveC;
    }

    public void setThresholdHighResolveC(BigDecimal thresholdHighResolveC) {
        this.thresholdHighResolveC = thresholdHighResolveC;
    }

    public BigDecimal getThresholdFreezeC() {
        return thresholdFreezeC;
    }

    public void setThresholdFreezeC(BigDecimal thresholdFreezeC) {
        this.thresholdFreezeC = thresholdFreezeC;
    }

    public BigDecimal getThresholdFreezeResolveC() {
        return thresholdFreezeResolveC;
    }

    public void setThresholdFreezeResolveC(BigDecimal thresholdFreezeResolveC) {
        this.thresholdFreezeResolveC = thresholdFreezeResolveC;
    }

    public int getSustainDurationMinutes() {
        return sustainDurationMinutes;
    }

    public void setSustainDurationMinutes(int sustainDurationMinutes) {
        this.sustainDurationMinutes = sustainDurationMinutes;
    }

    public int getResolveDurationMinutes() {
        return resolveDurationMinutes;
    }

    public void setResolveDurationMinutes(int resolveDurationMinutes) {
        this.resolveDurationMinutes = resolveDurationMinutes;
    }

    public int getGapResetMinutes() {
        return gapResetMinutes;
    }

    public void setGapResetMinutes(int gapResetMinutes) {
        this.gapResetMinutes = gapResetMinutes;
    }

    public BigDecimal getMinValidTempC() {
        return minValidTempC;
    }

    public void setMinValidTempC(BigDecimal minValidTempC) {
        this.minValidTempC = minValidTempC;
    }

    public BigDecimal getMaxValidTempC() {
        return maxValidTempC;
    }

    public void setMaxValidTempC(BigDecimal maxValidTempC) {
        this.maxValidTempC = maxValidTempC;
    }
}
