package com.vftech.fluxopro.company.dto;

import java.util.UUID;

public record CompanyThreshold(UUID id, int stalledAfterDays) {
}
