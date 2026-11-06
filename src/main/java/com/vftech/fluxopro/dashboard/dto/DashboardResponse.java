package com.vftech.fluxopro.dashboard.dto;

import com.vftech.fluxopro.opportunity.entity.OpportunityStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record DashboardResponse(
        int periodDays,
        int openCount,
        BigDecimal openAmount,
        int stalledCount,
        BigDecimal stalledAmount,
        int wonCount,
        BigDecimal wonAmount,
        int lostCount,
        BigDecimal lostAmount,
        Double conversionRate,
        Double averageDaysToWin,
        List<StageStats> stages,
        List<LostReasonStats> lostReasons,
        List<SellerStats> sellers) {

    public record StageStats(OpportunityStatus status, int count, BigDecimal amount) {
    }

    public record LostReasonStats(String reason, int count) {
    }

    public record SellerStats(UUID userId, String name, int openCount, BigDecimal openAmount,
                              int stalledCount, int wonCount, BigDecimal wonAmount, int lostCount) {
    }
}
