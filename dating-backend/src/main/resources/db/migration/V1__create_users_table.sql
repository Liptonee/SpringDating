CREATE TABLE users (
    id                  BIGSERIAL PRIMARY KEY,
    first_name          VARCHAR(30)  NOT NULL,
    password_hash       VARCHAR(100) NOT NULL,
    email               VARCHAR(50)  NOT NULL,
    city                VARCHAR(40),
    full_about          VARCHAR(512),
    short_about         VARCHAR(127),
    age                 INTEGER,
    gender              VARCHAR(10)  NOT NULL,
    preferred_age_min   SMALLINT,
    preferred_age_max   SMALLINT,
    role                VARCHAR(20)  NOT NULL,

    CONSTRAINT uq_users_email UNIQUE (email)
);