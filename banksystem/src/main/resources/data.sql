-- Insert default loan types
INSERT INTO loan_type (name, interest_rate, max_term_months, max_amount)
SELECT * FROM (SELECT 'Consumer', 7.99, 60, 50000.00) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM loan_type WHERE name = 'Consumer');

INSERT INTO loan_type (name, interest_rate, max_term_months, max_amount)
SELECT * FROM (SELECT 'Mortgage', 3.50, 360, 500000.00) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM loan_type WHERE name = 'Mortgage');

INSERT INTO loan_type (name, interest_rate, max_term_months, max_amount)
SELECT * FROM (SELECT 'Business', 5.99, 120, 200000.00) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM loan_type WHERE name = 'Business');

-- Insert default admin user
INSERT INTO employee (ucn, email, encrypted_password, role)
SELECT '1234567890', 'admin@bank.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'ADMIN'
    WHERE NOT EXISTS (SELECT 1 FROM employee WHERE email = 'admin@bank.com');