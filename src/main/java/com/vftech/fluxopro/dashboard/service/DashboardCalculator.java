package com.vftech.fluxopro.dashboard.service;

import com.vftech.fluxopro.dashboard.dto.DashboardResponse;
import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.opportunity.entity.OpportunityStatus;
import com.vftech.fluxopro.user.entity.User;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;

public final class DashboardCalculator {

    static final int STALLED_DAYS = 7;
    private static final String NO_REASON = "Sem motivo informado";

    private DashboardCalculator() {
    }

    public static DashboardResponse calculate(List<Opportunity> open, List<Opportunity> closed,
                                       Instant now, int periodDays) {
        Instant stalledBefore = now.minus(STALLED_DAYS, ChronoUnit.DAYS);

        List<Opportunity> stalled = open.stream()
                .filter(o -> o.getLastActivityAt().isBefore(stalledBefore))
                .toList();
        List<Opportunity> won = closed.stream().filter(o -> o.getStatus() == OpportunityStatus.WON).toList();
        List<Opportunity> lost = closed.stream().filter(o -> o.getStatus() == OpportunityStatus.LOST).toList();

        Double conversion = closed.isEmpty() ? null : (double) won.size() / closed.size();
        Double averageDaysToWin = won.isEmpty() ? null : won.stream()
                // getCreatedAt(): troque aqui se o getter da sua entidade tiver outro nome.
                .mapToDouble(o -> Duration.between(o.getCreatedAt(), o.getStatusChangedAt()).toSeconds() / 86_400.0)
                .average()
                .orElse(0);

        List<DashboardResponse.StageStats> stages = OpportunityStatus.openStatuses().stream()
                .map(status -> {
                    List<Opportunity> inStage = open.stream().filter(o -> o.getStatus() == status).toList();
                    return new DashboardResponse.StageStats(status, inStage.size(), sum(inStage));
                })
                .toList();

        return new DashboardResponse(
                periodDays,
                open.size(), sum(open),
                stalled.size(), sum(stalled),
                won.size(), sum(won),
                lost.size(), sum(lost),
                conversion,
                averageDaysToWin == null ? null : Math.round(averageDaysToWin * 10) / 10.0,
                stages,
                lostReasons(lost),
                sellers(open, stalledBefore, won, lost));
    }

    private static List<DashboardResponse.LostReasonStats> lostReasons(List<Opportunity> lost) {
        // Agrupa ignorando maiúsculas e espaços ("Preço" e "preço " viram um motivo só).
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, String> display = new HashMap<>();
        for (Opportunity o : lost) {
            String raw = o.getLostReason() == null ? "" : o.getLostReason().trim();
            String label = raw.isEmpty() ? NO_REASON : raw;
            String key = label.toLowerCase(Locale.ROOT);
            display.putIfAbsent(key, label);
            counts.merge(key, 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> new DashboardResponse.LostReasonStats(display.get(e.getKey()), e.getValue()))
                .toList();
    }

    private static List<DashboardResponse.SellerStats> sellers(List<Opportunity> open, Instant stalledBefore,
                                                               List<Opportunity> won, List<Opportunity> lost) {
        Map<UUID, User> owners = new LinkedHashMap<>();
        Stream.of(open, won, lost).flatMap(List::stream)
                .forEach(o -> owners.putIfAbsent(o.getOwner().getId(), o.getOwner()));

        return owners.values().stream()
                .map(owner -> {
                    List<Opportunity> mineOpen = ownedBy(open, owner);
                    List<Opportunity> mineWon = ownedBy(won, owner);
                    return new DashboardResponse.SellerStats(
                            owner.getId(), owner.getName(),
                            mineOpen.size(), sum(mineOpen),
                            (int) mineOpen.stream().filter(o -> o.getLastActivityAt().isBefore(stalledBefore)).count(),
                            mineWon.size(), sum(mineWon),
                            ownedBy(lost, owner).size());
                })
                .sorted(Comparator.comparing(DashboardResponse.SellerStats::wonAmount).reversed()
                        .thenComparing(DashboardResponse.SellerStats::name))
                .toList();
    }

    private static List<Opportunity> ownedBy(List<Opportunity> list, User owner) {
        return list.stream().filter(o -> o.getOwner().getId().equals(owner.getId())).toList();
    }

    private static BigDecimal sum(List<Opportunity> list) {
        return list.stream()
                .map(Opportunity::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
