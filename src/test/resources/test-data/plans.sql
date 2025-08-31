-- Insert test subscription plans matching your EXACT MySQL database schema
-- Including ALL required columns that exist in your MySQL table

INSERT INTO subscription_plans (
    id,
    name,
    price_usd,
    price_inr,
    features,
    max_api_services,
    max_apis,
    max_requests_per_month,
    support_level,
    is_active,
    display_order
) VALUES
(1, 'Starter', 0.00, 0.00, 'Basic API monitoring,Email alerts,Basic analytics', 5, 5, 10000, 'BASIC', 1, 1),
(2, 'Pro', 25.00, 2075.00, 'Advanced analytics,SMS + Email alerts,Priority support,Custom dashboards', 20, 20, 100000, 'PRIORITY', 1, 2),
(3, 'Enterprise', 100.00, 8300.00, 'Unlimited API services,Custom integrations,Dedicated support,Advanced reporting,SLA guarantee', 999, 999, 1000000, 'DEDICATED', 1, 3);

-- Verify the insertion worked
SELECT * FROM subscription_plans;