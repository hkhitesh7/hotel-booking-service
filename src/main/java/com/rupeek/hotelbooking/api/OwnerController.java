package com.rupeek.hotelbooking.api;

import com.rupeek.hotelbooking.api.dto.OnboardingRequests.OnboardOwnerRequest;
import com.rupeek.hotelbooking.api.dto.OnboardingRequests.PropertyRequest;
import com.rupeek.hotelbooking.api.dto.OwnerResponse;
import com.rupeek.hotelbooking.api.dto.PropertyResponse;
import com.rupeek.hotelbooking.application.PropertyOnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/owners")
@Tag(name = "Onboarding")
public class OwnerController {

    private final PropertyOnboardingService onboarding;

    public OwnerController(PropertyOnboardingService onboarding) {
        this.onboarding = onboarding;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Onboard an owner (individual or chain) with zero or more properties")
    public OwnerResponse onboardOwner(@Valid @RequestBody OnboardOwnerRequest request) {
        return OwnerResponse.from(onboarding.onboardOwner(request.toCommand()));
    }

    @PostMapping("/{ownerId}/properties")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a property to an existing owner's portfolio")
    public PropertyResponse addProperty(@PathVariable String ownerId, @Valid @RequestBody PropertyRequest request) {
        return PropertyResponse.from(onboarding.addProperty(ownerId, request.toCommand()));
    }

    @GetMapping("/{ownerId}")
    public OwnerResponse getOwner(@PathVariable String ownerId) {
        return OwnerResponse.from(onboarding.getOwner(ownerId));
    }
}
