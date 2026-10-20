package com.vftech.fluxopro.common.concurrency;

import com.vftech.fluxopro.common.exception.ConflictException;

import java.util.Objects;

public class VersionCheck {

    private VersionCheck() {
    }

    public static void ensure(Long current, Long expected) {
        if (!Objects.equals(current, expected)) {
            throw new ConflictException("The record was changed by someone else. Reload it and try again.");
        }
    }
}
