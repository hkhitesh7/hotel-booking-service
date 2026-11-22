package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.domain.common.InvalidRequestException;
import com.rupeek.hotelbooking.domain.payment.PaymentMethod;
import com.rupeek.hotelbooking.domain.payment.PaymentProcessor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Every {@link PaymentProcessor} bean is picked up here; adding a method needs no change to this class. */
@Component
public class PaymentProcessorRegistry {

    private final Map<PaymentMethod, PaymentProcessor> processorsByMethod;

    public PaymentProcessorRegistry(List<PaymentProcessor> processors) {
        this.processorsByMethod = processors.stream()
                .collect(Collectors.toUnmodifiableMap(PaymentProcessor::method, Function.identity()));
    }

    public PaymentProcessor forMethod(PaymentMethod method) {
        PaymentProcessor processor = processorsByMethod.get(method);
        if (processor == null) {
            throw new InvalidRequestException("Unsupported payment method '" + method.code() + "', supported: "
                    + supportedMethods());
        }
        return processor;
    }

    public Set<String> supportedMethods() {
        return processorsByMethod.keySet().stream().map(PaymentMethod::code).collect(Collectors.toSet());
    }
}
