package com.freight.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class UploadControllerSecurityTest {

    @Test
    void maintainerCanUploadQuoteAndPortChargeFiles() throws Exception {
        assertThat(preAuthorizeValue(QuoteController.class, "upload"))
                .isEqualTo("hasAnyRole('ADMIN','MAINTAINER')");
        assertThat(preAuthorizeValue(DestPortChargeController.class, "upload"))
                .isEqualTo("hasAnyRole('ADMIN','MAINTAINER')");
        assertThat(preAuthorizeValue(VesselScheduleController.class, "upload"))
                .isEqualTo("hasAnyRole('ADMIN','MAINTAINER')");
    }

    private String preAuthorizeValue(Class<?> controllerType, String methodName) throws Exception {
        Method uploadMethod = java.util.Arrays.stream(controllerType.getDeclaredMethods())
                .filter(method -> method.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        return uploadMethod.getAnnotation(PreAuthorize.class).value();
    }
}
