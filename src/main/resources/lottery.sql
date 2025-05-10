CREATE TABLE lotteries (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    prize INT DEFAULT 1000,
    chances INT DEFAULT 10
);

CREATE TABLE lottery_tickets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    lottery_name VARCHAR(255) NOT NULL,
    ticket_number INT NOT NULL,
    prize INT,
    is_winner BOOLEAN DEFAULT FALSE
);
