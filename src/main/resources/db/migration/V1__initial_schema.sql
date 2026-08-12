create table users (
    id bigserial primary key,
    public_id uuid not null unique,
    name varchar(120) not null,
    email varchar(180) not null unique,
    password_hash varchar(255) not null,
    role varchar(30) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table payments (
    id bigserial primary key,
    public_id uuid not null unique,
    payment_reference varchar(40) not null unique,
    merchant_id bigint not null references users(id),
    amount numeric(19,2) not null check (amount > 0),
    currency varchar(3) not null check (currency in ('INR','USD','EUR')),
    description varchar(500),
    status varchar(30) not null,
    payment_method varchar(30) not null,
    failure_reason varchar(300),
    simulation_token varchar(120),
    version bigint not null default 0,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    completed_at timestamptz
);

create table refunds (
    id bigserial primary key,
    public_id uuid not null unique,
    refund_reference varchar(40) not null unique,
    payment_id bigint not null references payments(id),
    amount numeric(19,2) not null check (amount > 0),
    status varchar(30) not null,
    reason varchar(300),
    created_at timestamptz not null,
    completed_at timestamptz
);

create table idempotency_keys (
    id bigserial primary key,
    merchant_id bigint not null references users(id),
    idempotency_key varchar(180) not null,
    request_hash varchar(128) not null,
    response_payload text not null,
    response_status int not null,
    created_at timestamptz not null,
    constraint uk_idempotency_merchant_key unique (merchant_id, idempotency_key)
);

create table webhook_events (
    id bigserial primary key,
    event_id uuid not null unique,
    event_type varchar(80) not null,
    payment_id bigint not null references payments(id),
    payload text not null,
    status varchar(30) not null,
    attempt_count int not null default 0,
    next_retry_at timestamptz,
    created_at timestamptz not null,
    delivered_at timestamptz
);

create table audit_logs (
    id bigserial primary key,
    entity_type varchar(40) not null,
    entity_id varchar(80) not null,
    action varchar(80) not null,
    old_status varchar(30),
    new_status varchar(30),
    performed_by varchar(120),
    metadata text,
    created_at timestamptz not null
);

create index idx_payments_reference on payments(payment_reference);
create index idx_payments_merchant on payments(merchant_id);
create index idx_payments_status on payments(status);
create index idx_payments_created_at on payments(created_at);
create index idx_payments_currency on payments(currency);
create index idx_refunds_payment on refunds(payment_id);
create index idx_webhook_pending on webhook_events(status, next_retry_at);
create index idx_audit_entity on audit_logs(entity_type, entity_id);
