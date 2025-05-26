-- Create users table with inheritance
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       user_type VARCHAR(50) NOT NULL,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       first_name VARCHAR(255) NOT NULL,
                       last_name VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Regular user specific columns
                       phone_number VARCHAR(20),

    -- Admin user specific columns
                       department VARCHAR(100),
                       admin_level VARCHAR(50)
);

-- Create movies table
CREATE TABLE movies (
                        id BIGSERIAL PRIMARY KEY,
                        title VARCHAR(255) NOT NULL,
                        description TEXT,
                        duration_minutes INTEGER NOT NULL,
                        genre VARCHAR(50),
                        director VARCHAR(255) NOT NULL,
                        poster_url VARCHAR(500),
                        rating DECIMAL(3,1),
                        release_date TIMESTAMP,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create cinemas table
CREATE TABLE cinemas (
                         id BIGSERIAL PRIMARY KEY,
                         name VARCHAR(255) NOT NULL,
                         address VARCHAR(500) NOT NULL,
                         city VARCHAR(100),
                         phone_number VARCHAR(20)
);

-- Create cinema_halls table
CREATE TABLE cinema_halls (
                              id BIGSERIAL PRIMARY KEY,
                              name VARCHAR(100) NOT NULL,
                              total_seats INTEGER NOT NULL,
                              rows INTEGER NOT NULL,
                              seats_per_row INTEGER NOT NULL,
                              hall_type VARCHAR(50),
                              cinema_id BIGINT NOT NULL,
                              FOREIGN KEY (cinema_id) REFERENCES cinemas(id) ON DELETE CASCADE
);

-- Create screenings table
CREATE TABLE screenings (
                            id BIGSERIAL PRIMARY KEY,
                            movie_id BIGINT NOT NULL,
                            hall_id BIGINT NOT NULL,
                            start_time TIMESTAMP NOT NULL,
                            end_time TIMESTAMP NOT NULL,
                            price DECIMAL(10,2) NOT NULL,
                            available_seats INTEGER,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
                            FOREIGN KEY (hall_id) REFERENCES cinema_halls(id) ON DELETE CASCADE
);

-- Create reservations table
CREATE TABLE reservations (
                              id BIGSERIAL PRIMARY KEY,
                              user_id BIGINT NOT NULL,
                              screening_id BIGINT NOT NULL,
                              total_price DECIMAL(10,2) NOT NULL,
                              status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              confirmation_code VARCHAR(50),
                              FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                              FOREIGN KEY (screening_id) REFERENCES screenings(id) ON DELETE CASCADE
);

-- Create reserved_seats table
CREATE TABLE reserved_seats (
                                id BIGSERIAL PRIMARY KEY,
                                reservation_id BIGINT NOT NULL,
                                screening_id BIGINT NOT NULL,
                                row_number INTEGER NOT NULL,
                                seat_number INTEGER NOT NULL,
                                FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
                                FOREIGN KEY (screening_id) REFERENCES screenings(id) ON DELETE CASCADE,
                                UNIQUE(screening_id, row_number, seat_number)
);

-- Create indexes for better performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_type ON users(user_type);
CREATE INDEX idx_screenings_movie ON screenings(movie_id);
CREATE INDEX idx_screenings_hall ON screenings(hall_id);
CREATE INDEX idx_screenings_start_time ON screenings(start_time);
CREATE INDEX idx_reservations_user ON reservations(user_id);
CREATE INDEX idx_reservations_screening ON reservations(screening_id);
CREATE INDEX idx_reservations_status ON reservations(status);