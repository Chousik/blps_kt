INSERT INTO users (id, username, email, first_name, last_name, role, created_at, updated_at)
VALUES (
    '11111111-1111-4111-8111-111111111111',
    'guest-alex',
    'guest.alex@example.com',
    'Alex',
    'Morozov',
    'GUEST',
    '2024-05-01T10:00:00Z'::timestamptz,
    '2024-05-01T10:00:00Z'::timestamptz
) ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, username, email, first_name, last_name, role, created_at, updated_at)
VALUES (
    '22222222-2222-4222-8222-222222222222',
    'guest-vika',
    'guest.vika@example.com',
    'Victoria',
    'Ivanova',
    'GUEST',
    '2024-05-01T11:00:00Z'::timestamptz,
    '2024-05-01T11:00:00Z'::timestamptz
) ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, username, email, first_name, last_name, role, created_at, updated_at)
VALUES (
    '33333333-3333-4333-8333-333333333333',
    'host-dmitry',
    'host.dmitry@example.com',
    'Dmitry',
    'Petrov',
    'HOST',
    '2024-05-01T12:00:00Z'::timestamptz,
    '2024-05-01T12:00:00Z'::timestamptz
) ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, username, email, first_name, last_name, role, created_at, updated_at)
VALUES (
    '44444444-4444-4444-8444-444444444444',
    'host-elena',
    'host.elena@example.com',
    'Elena',
    'Sidorova',
    'HOST',
    '2024-05-01T13:00:00Z'::timestamptz,
    '2024-05-01T13:00:00Z'::timestamptz
) ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, username, email, first_name, last_name, role, created_at, updated_at)
VALUES (
    '55555555-5555-4555-8555-555555555555',
    'ops-olga',
    'ops.olga@example.com',
    'Olga',
    'Support',
    'PLATFORM',
    '2024-05-01T14:00:00Z'::timestamptz,
    '2024-05-01T14:00:00Z'::timestamptz
) ON CONFLICT (id) DO NOTHING;
