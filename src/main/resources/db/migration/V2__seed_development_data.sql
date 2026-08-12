insert into users (public_id, name, email, password_hash, role, created_at, updated_at) values
('00000000-0000-0000-0000-000000000001', 'Admin User', 'admin@example.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiw/Jz0i1eyS5UJGk7r4V9S42zB4WfC', 'ADMIN', now(), now()),
('00000000-0000-0000-0000-000000000002', 'Merchant One', 'merchant1@example.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiw/Jz0i1eyS5UJGk7r4V9S42zB4WfC', 'MERCHANT', now(), now()),
('00000000-0000-0000-0000-000000000003', 'Merchant Two', 'merchant2@example.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiw/Jz0i1eyS5UJGk7r4V9S42zB4WfC', 'MERCHANT', now(), now());

insert into payments (public_id, payment_reference, merchant_id, amount, currency, description, status, payment_method, failure_reason, simulation_token, version, created_at, updated_at, completed_at) values
('10000000-0000-0000-0000-000000000001', 'pay_seed_success_00000001', 2, 1500.00, 'INR', 'Seed successful payment', 'SUCCEEDED', 'CARD', null, '****4242', 0, now(), now(), now()),
('10000000-0000-0000-0000-000000000002', 'pay_seed_failed_00000002', 2, 75.00, 'USD', 'Seed failed payment', 'FAILED', 'CARD', 'Card declined by simulator.', '****4000', 0, now(), now(), now()),
('10000000-0000-0000-0000-000000000003', 'pay_seed_pending_00000003', 3, 230.00, 'EUR', 'Seed pending payment', 'PENDING', 'UPI', null, 'success@upi', 0, now(), now(), null),
('10000000-0000-0000-0000-000000000004', 'pay_seed_refunded_0000004', 2, 500.00, 'INR', 'Seed refunded payment', 'REFUNDED', 'CARD', null, '****4242', 0, now(), now(), now());

insert into refunds (public_id, refund_reference, payment_id, amount, status, reason, created_at, completed_at) values
('20000000-0000-0000-0000-000000000001', 'rf_seed_full_00000000001', 4, 500.00, 'SUCCEEDED', 'Seed full refund', now(), now());
