################################################################################
###                                                                          ###
### Author: Cornell Reddick                                                  ###
### Company: Llenroc Tech LLC                                                ###
### Project: Enterprise Customer Platform                                    ###
### Module: CustomerConnect API                                              ###
### File: schema.sql                                                         ###
### Version: 1.0                                                             ###
###                                                                          ###
################################################################################

/*
 * --- Database Naming Standards ---
 *
 * 1. Use snake_case for database object names.
 *
 * 2. Use plural table names.
 *    Examples:
 *      users
 *      roles
 *      user_roles
 *
 * 3. Use descriptive primary key names.
 *    Examples:
 *      user_id
 *      role_id
 *      user_role_id
 *
 * 4. Foreign key columns should use the same name as the
 *    primary key column they reference.
 *
 *    Example:
 *      users.user_id
 *      user_roles.user_id
 *
 * 5. Avoid ambiguous column names.
 *
 * 6. Use consistent naming conventions across:
 *      - MySQL schema
 *      - JPA entities
 *      - Spring Data repositories
 *      - REST API models
 *
 * 7. Schema changes should remain aligned with the approved
 *    CustomerConnect physical data model.
 *
 * 8. Database credentials and environment-specific secrets
 *    must never be stored in this file.
 *
 * --- Current Schema Scope ---
 *
 * Current physical model:
 *      - users
 *      - roles
 *      - user_roles
 *
 * Database:
 *      MySQL
 *
 * Database design tool:
 *      MySQL Workbench
 *
 * Schema deployment workflow:
 *      Physical Model
 *          -> Forward Engineering
 *          -> Generated SQL
 *          -> MySQL Schema
 *          -> Spring Boot API Integration
 */

CREATE SCHEMA IF NOT EXISTS customerconnect;

USE customerconnect;

SET NAMES utf8mb4;
SET time_zone = 'US/Eastern';

DROP TABLE IF EXISTS Users;

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
    image_url    VARCHAR(255)    image_url VARCHAR(255) DEFAULT '/assets/images/avatars/default-user-avatar.png',

    CONSTRAINT PK_Users PRIMARY KEY (id),
    CONSTRAINT UQ_Users_Email UNIQUE (email)
);