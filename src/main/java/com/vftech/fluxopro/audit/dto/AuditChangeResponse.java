package com.vftech.fluxopro.audit.dto;

public record AuditChangeResponse(String field, String oldValue, String newValue) {
}
