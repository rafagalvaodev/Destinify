ALTER TABLE tb_payments
    ADD COLUMN provider_checkout_id VARCHAR(60),
    ADD COLUMN checkout_url         VARCHAR(500),
    ADD COLUMN expires_at           TIMESTAMP;

CREATE UNIQUE INDEX uq_payments_provider_checkout_id
    ON tb_payments (provider_checkout_id)
    WHERE provider_checkout_id IS NOT NULL;
