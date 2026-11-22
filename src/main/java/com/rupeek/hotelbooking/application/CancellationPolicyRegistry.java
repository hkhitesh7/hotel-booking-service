package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.domain.cancellation.CancellationPolicy;
import com.rupeek.hotelbooking.domain.common.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Looks up cancellation policies by code; every {@link CancellationPolicy} bean is registered automatically. */
@Component
public class CancellationPolicyRegistry {

    private final Map<String, CancellationPolicy> policiesByCode;

    public CancellationPolicyRegistry(List<CancellationPolicy> policies) {
        this.policiesByCode = policies.stream()
                .collect(Collectors.toUnmodifiableMap(p -> p.code().toUpperCase(Locale.ROOT), Function.identity()));
    }

    public CancellationPolicy resolve(String code) {
        CancellationPolicy policy = code == null ? null : policiesByCode.get(code.trim().toUpperCase(Locale.ROOT));
        if (policy == null) {
            throw new InvalidRequestException("Unknown cancellation policy '" + code + "', supported: "
                    + policiesByCode.keySet());
        }
        return policy;
    }
}
