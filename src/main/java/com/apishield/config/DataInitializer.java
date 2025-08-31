package com.apishield.config;

import com.apishield.model.SubscriptionPlan;
import com.apishield.repository.SubscriptionPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"!test"}) // Run in all profiles EXCEPT test
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private SubscriptionPlanRepository planRepository;

    @Override
    public void run(String... args) throws Exception {
        // Only initialize if no plans exist
        if (planRepository.count() == 0) {
            System.out.println("Initializing subscription plans...");

            SubscriptionPlan starterPlan = SubscriptionPlan.builder()
                    .name("Starter")
                    .priceUsd(0.0)
                    .priceInr(0.0)
                    .features("Basic features,5 API services,Email support")
                    .maxApis(5)
                    .maxRequestsPerMonth(10000L)
                    .supportLevel(SubscriptionPlan.SupportLevel.BASIC)
                    .isActive(true)
                    .displayOrder(1)
                    .build();

            SubscriptionPlan proPlan = SubscriptionPlan.builder()
                    .name("Pro")
                    .priceUsd(25.0)
                    .priceInr(2075.0)
                    .features("Advanced features,20 API services,Priority support")
                    .maxApis(20)
                    .maxRequestsPerMonth(100000L)
                    .supportLevel(SubscriptionPlan.SupportLevel.PRIORITY)
                    .isActive(true)
                    .displayOrder(2)
                    .build();

            SubscriptionPlan enterprisePlan = SubscriptionPlan.builder()
                    .name("Enterprise")
                    .priceUsd(99.0)
                    .priceInr(8250.0)
                    .features("All features,Unlimited APIs,24/7 Support,Custom integrations")
                    .maxApis(0) // 0 = unlimited
                    .maxRequestsPerMonth(1000000L)
                    .supportLevel(SubscriptionPlan.SupportLevel.PRIORITY) // Changed from PREMIUM to PRIORITY
                    .isActive(true)
                    .displayOrder(3)
                    .build();

            planRepository.save(starterPlan);
            planRepository.save(proPlan);
            planRepository.save(enterprisePlan);

            System.out.println("Subscription plans initialized successfully!");
            System.out.println("- Starter Plan: Free");
            System.out.println("- Pro Plan: $25/month");
            System.out.println("- Enterprise Plan: $99/month");
        } else {
            System.out.println("Found " + planRepository.count() + " existing subscription plans, skipping initialization.");
        }
    }
}