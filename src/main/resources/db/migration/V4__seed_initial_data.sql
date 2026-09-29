-- V4: Initial Seed Data for Demo

-- 1. Demo Categories
INSERT INTO categories (id, name, slug, description, created_at, updated_at)
VALUES 
    ('11111111-1111-1111-1111-111111111101', 'Software Engineering', 'software-engineering', 'Architecture, clean code, design patterns, and engineering practices.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('11111111-1111-1111-1111-111111111102', 'Cloud & DevOps', 'cloud-devops', 'Kubernetes, AWS, containerization, CI/CD, and site reliability.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('11111111-1111-1111-1111-111111111103', 'Artificial Intelligence', 'artificial-intelligence', 'Machine learning, LLMs, neural networks, and generative AI.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('11111111-1111-1111-1111-111111111104', 'Databases & Distributed Systems', 'databases-distributed-systems', 'PostgreSQL, NoSQL, data intensive architectures, and distributed computing.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 2. Demo Authors
INSERT INTO authors (id, name, bio, created_at, updated_at)
VALUES 
    ('22222222-2222-2222-2222-222222222201', 'Robert C. Martin', 'Software consultant and author of Clean Code and Clean Architecture.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('22222222-2222-2222-2222-222222222202', 'Martin Fowler', 'Chief Scientist at Thoughtworks, author of Refactoring and Patterns of Enterprise Application Architecture.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('22222222-2222-2222-2222-222222222203', 'Martin Kleppmann', 'Associate Professor at University of Cambridge, author of Designing Data-Intensive Applications.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('22222222-2222-2222-2222-222222222204', 'Eric Evans', 'Domain-Driven Design pioneer and founder of Domain Language.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 3. Demo Publishers (Brands)
INSERT INTO publishers (id, name, website, created_at, updated_at)
VALUES 
    ('33333333-3333-3333-3333-333333333301', 'O''Reilly Media', 'https://www.oreilly.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333333302', 'Pearson Education', 'https://www.pearson.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333333303', 'Addison-Wesley Professional', 'https://www.informit.com/imprint/addison-wesley', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333333304', 'Manning Publications', 'https://www.manning.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 4. Demo Books
INSERT INTO books (
    id, title, isbn, description, price, stock_quantity, cover_image_url, 
    expected_delivery_days, active, version, category_id, author_id, publisher_id, created_at, updated_at
)
VALUES 
    (
        '44444444-4444-4444-4444-444444444401', 
        'Clean Code: A Handbook of Agile Software Craftsmanship', 
        '978-0132350884', 
        'Even bad code can function. But if code isn''t clean, it can bring a development organization to its knees.', 
        44.99, 
        25, 
        'https://images.unsplash.com/photo-1532012164546-f432f2e3edd9?auto=format&fit=crop&w=600&q=80', 
        2, 
        TRUE, 
        0, 
        '11111111-1111-1111-1111-111111111101', 
        '22222222-2222-2222-2222-222222222201', 
        '33333333-3333-3333-3333-333333333302', 
        CURRENT_TIMESTAMP, 
        CURRENT_TIMESTAMP
    ),
    (
        '44444444-4444-4444-4444-444444444402', 
        'Clean Architecture: A Craftsman''s Guide to Software Structure', 
        '978-0134494166', 
        'Practical software architecture solutions for the real world.', 
        39.50, 
        18, 
        'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=600&q=80', 
        3, 
        TRUE, 
        0, 
        '11111111-1111-1111-1111-111111111101', 
        '22222222-2222-2222-2222-222222222201', 
        '33333333-3333-3333-3333-333333333303', 
        CURRENT_TIMESTAMP, 
        CURRENT_TIMESTAMP
    ),
    (
        '44444444-4444-4444-4444-444444444403', 
        'Designing Data-Intensive Applications', 
        '978-1449373320', 
        'The big ideas behind reliable, scalable, and maintainable systems.', 
        49.99, 
        30, 
        'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=600&q=80', 
        2, 
        TRUE, 
        0, 
        '11111111-1111-1111-1111-111111111104', 
        '22222222-2222-2222-2222-222222222203', 
        '33333333-3333-3333-3333-333333333301', 
        CURRENT_TIMESTAMP, 
        CURRENT_TIMESTAMP
    ),
    (
        '44444444-4444-4444-4444-444444444404', 
        'Domain-Driven Design: Tackling Complexity in Software', 
        '978-0321125217', 
        'Leading software designers have recognized domain modeling and design as core practices.', 
        54.00, 
        12, 
        'https://images.unsplash.com/photo-1497633762265-9d179a990aa6?auto=format&fit=crop&w=600&q=80', 
        4, 
        TRUE, 
        0, 
        '11111111-1111-1111-1111-111111111101', 
        '22222222-2222-2222-2222-222222222204', 
        '33333333-3333-3333-3333-333333333303', 
        CURRENT_TIMESTAMP, 
        CURRENT_TIMESTAMP
    ),
    (
        '44444444-4444-4444-4444-444444444405', 
        'Patterns of Enterprise Application Architecture', 
        '978-0321127426', 
        'An indispensable handbook of solutions for enterprise software developers.', 
        52.25, 
        10, 
        'https://images.unsplash.com/photo-1457369804613-52c61a468e7d?auto=format&fit=crop&w=600&q=80', 
        3, 
        TRUE, 
        0, 
        '11111111-1111-1111-1111-111111111101', 
        '22222222-2222-2222-2222-222222222202', 
        '33333333-3333-3333-3333-333333333303', 
        CURRENT_TIMESTAMP, 
        CURRENT_TIMESTAMP
    )
ON CONFLICT (id) DO NOTHING;

-- 5. Demo Customer Account (BCrypt hashed password for 'Password123!')
-- BCrypt: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO users (
    id, username, email, password_hash, full_name, role, reward_points, active, created_at, updated_at
)
VALUES (
    '55555555-5555-5555-5555-555555555501',
    'customer_demo',
    'customer.demo@example.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Jane Customer',
    'ROLE_CUSTOMER',
    350,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- 6. Demo Customer Address
INSERT INTO user_addresses (
    id, user_id, recipient_name, phone, street, city, state, postal_code, country, is_default, created_at, updated_at
)
VALUES (
    '66666666-6666-6666-6666-666666666601',
    '55555555-5555-5555-5555-555555555501',
    'Jane Customer',
    '+1-555-0144',
    '742 Evergreen Terrace',
    'Springfield',
    'OR',
    '97477',
    'USA',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- 7. Initialize Shopping Cart for Demo Customer
INSERT INTO carts (id, user_id, created_at, updated_at)
VALUES (
    '77777777-7777-7777-7777-777777777701',
    '55555555-5555-5555-5555-555555555501',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (user_id) DO NOTHING;
