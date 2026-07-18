################################################################################
###                                                                          ###
### Author: Cornell Reddick                                                  ###
### Company: Llenroc Tech LLC                                                ###
### Project: Enterprise Customer Platform                                    ###
### Module: CustomerConnect API                                              ###
### File: data.sql                                                           ###
### Version: 1.0                                                             ###
################################################################################

/*
 * Seeds reference data required by the CustomerConnect application.
 *
 * Current Data:
 * - Application roles
 * - (Future) Event types
 * - (Future) Default lookup values
 */

/*
 * CustomerConnect Database Schema
 *
 * Purpose:
 *   Defines the initial MySQL database schema for the CustomerConnect API.
 *
 * Current Schema Scope:
 *   - Users
 *   - Roles
 *   - UserRoles
 *
 * Database:
 *   MySQL
 *
 * Database Design Tool:
 *   MySQL Workbench
 *
 * Schema Workflow:
 *
 *   Requirements
 *       -> Logical Data Model
 *       -> Physical Data Model
 *       -> Forward Engineering
 *       -> MySQL Database Schema
 *       -> Spring Boot API Integration
 *
 * Standards:
 *   - Keep database naming consistent across the application architecture.
 *   - Keep schema changes aligned with the CustomerConnect physical data model.
 *   - Keep MySQL tables aligned with JPA entity mappings.
 *   - Do not store database credentials, secrets, or environment-specific
 *     configuration values in this file.
 */


-- =============================================================================
-- DATABASE INITIALIZATION
-- Creates and selects the CustomerConnect database schema.
-- Configures Unicode support and the database session time zone.
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS customerconnect;

USE customerconnect;

SET NAMES utf8mb4;
SET TIME_ZONE = '-4:00';

-- =============================================================================
-- TABLE CLEANUP
-- Drops existing tables before schema recreation.
-- Child tables are dropped before parent tables to respect foreign key
-- dependencies.
-- =============================================================================

DROP TABLE IF EXISTS AccountVerifications;
DROP TABLE IF EXISTS ResetPasswordVerifications;
DROP TABLE IF EXISTS TwoFactorVerifications;
DROP TABLE IF EXISTS UserEvents;
DROP TABLE IF EXISTS UserRoles;

DROP TABLE IF EXISTS Events;
DROP TABLE IF EXISTS Roles;
DROP TABLE IF EXISTS Users;

-- =============================================================================
-- USERS TABLE
-- Stores application user accounts, profile information, authentication
-- configuration, account status, and default profile image information.
-- =============================================================================

CREATE TABLE Users
(
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    first_name   VARCHAR(50)     NOT NULL,
    last_name    VARCHAR(50)     NOT NULL,
    email        VARCHAR(100)    NOT NULL,
    password     VARCHAR(255)    NOT NULL,
    address      VARCHAR(255)    DEFAULT NULL,
    phone        VARCHAR(30)     DEFAULT NULL,
    title        VARCHAR(50)     DEFAULT NULL,
    bio          VARCHAR(255)    DEFAULT NULL,
    enabled      BOOLEAN         DEFAULT FALSE,
    non_locked   BOOLEAN         DEFAULT TRUE,
    using_mfa    BOOLEAN         DEFAULT FALSE,
    created_date DATETIME        DEFAULT CURRENT_TIMESTAMP,
    image_url    VARCHAR(255)    DEFAULT '/assets/images/avatars/default-user-avatar.png',

    CONSTRAINT PK_Users PRIMARY KEY (id),
    CONSTRAINT UQ_Users_Email UNIQUE (email)
);


-- =============================================================================
-- ROLES TABLE
-- Stores application roles and their associated permission definitions.
-- =============================================================================

CREATE TABLE Roles
(
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(50)     NOT NULL,
    permission VARCHAR(255)    NOT NULL,

    CONSTRAINT UQ_Roles_Name UNIQUE (name)
);


-- =============================================================================
-- USER ROLES TABLE
-- Associates application users with their assigned application roles.
-- Foreign key rules maintain referential integrity between Users and Roles.
-- =============================================================================

CREATE TABLE UserRoles
(
    id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    role_id BIGINT UNSIGNED NOT NULL,

    FOREIGN KEY (user_id)
        REFERENCES Users (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    FOREIGN KEY (role_id)
        REFERENCES Roles (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT UQ_UserRoles_User_Id UNIQUE (user_id)
);

-- =============================================================================
-- EVENTS TABLE
-- Defines the supported user activity and security event types that can be
-- recorded by the CustomerConnect application.
-- =============================================================================

CREATE TABLE Events
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    type        VARCHAR(50)     NOT NULL
        CHECK (type IN (
                        'LOGIN_ATTEMPT',
                        'LOGIN_ATTEMPT_FAILURE',
                        'LOGIN_ATTEMPT_SUCCESS',
                        'PROFILE_UPDATE',
                        'PROFILE_PICTURE_UPDATE',
                        'ROLE_UPDATE',
                        'ACCOUNT_SETTINGS_UPDATE',
                        'PASSWORD_UPDATE',
                        'MFA_UPDATE'
            )),
    description VARCHAR(255)    NOT NULL,

    CONSTRAINT UQ_Events_Type UNIQUE (type)
);


-- =============================================================================
-- USER EVENTS TABLE
-- Records application activity and security events associated with users,
-- including device information, IP address, and event timestamp.
-- =============================================================================

CREATE TABLE UserEvents
(
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT UNSIGNED NOT NULL,
    event_id   BIGINT UNSIGNED NOT NULL,
    device     VARCHAR(100)    DEFAULT NULL,
    ip_address VARCHAR(100)    DEFAULT NULL,
    created_at DATETIME        DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id)
        REFERENCES Users (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    FOREIGN KEY (event_id)
        REFERENCES Events (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- =============================================================================
-- ACCOUNT VERIFICATIONS TABLE
-- Stores account verification URLs associated with application users.
-- Each user and verification URL must be unique.
-- =============================================================================

CREATE TABLE AccountVerifications
(
    id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    url     VARCHAR(255)    NOT NULL,
    `date`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id)
        REFERENCES Users (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT UQ_AccountVerifications_User_Id UNIQUE (user_id),
    CONSTRAINT UQ_AccountVerifications_Url UNIQUE (url)
);

-- =============================================================================
-- RESET PASSWORD VERIFICATIONS TABLE
-- Stores password reset verification URLs and expiration dates for users.
-- Each user and password reset URL must be unique.
-- =============================================================================

CREATE TABLE ResetPasswordVerifications
(
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT UNSIGNED NOT NULL,
    url             VARCHAR(255)    NOT NULL,
    expiration_date DATETIME        NOT NULL,

    FOREIGN KEY (user_id)
        REFERENCES Users (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT UQ_ResetPasswordVerifications_User_Id UNIQUE (user_id),
    CONSTRAINT UQ_ResetPasswordVerifications_Url UNIQUE (url)
);

-- =============================================================================
-- TWO-FACTOR VERIFICATIONS TABLE
-- Stores temporary verification codes and expiration dates used during the
-- CustomerConnect two-factor authentication process.
-- Each user and verification code must be unique.
-- =============================================================================

CREATE TABLE TwoFactorVerifications
(
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT UNSIGNED NOT NULL,
    code            VARCHAR(10)     NOT NULL,
    expiration_date DATETIME        NOT NULL,

    FOREIGN KEY (user_id)
        REFERENCES Users (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT UQ_TwoFactorVerifications_User_Id UNIQUE (user_id),
    CONSTRAINT UQ_TwoFactorVerifications_Code UNIQUE (code)
);