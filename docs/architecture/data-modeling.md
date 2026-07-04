# Data Modeling Architecture

## Purpose

This document captures how the Enterprise Customer Platform moves from business requirements to a logical data model and then to a physical database model.

## Modeling Flow

1. Requirements
2. Conceptual Model
3. Logical Model
4. Physical Model

## Logical Model

The logical model defines the business entities and relationships without focusing on a specific database.

Example starting entities:

- User
- Role
- UserRole
- Permission
- Customer
- CustomerStatus
- Invoice
- InvoiceLineItem
- UserActivity

## Physical Model

The physical model is the database-specific version of the logical model.

For this project, the physical model will be implemented in MySQL.

Physical model details include:

- Table names
- Column names
- Data types
- Primary keys
- Foreign keys
- Indexes
- Constraints

## Current Focus

The first data modeling slice will focus on application security:

- Users
- Roles
- UserRoles
- Permissions

After that, the model will expand into customer and invoice management.

## Notes

The physical model is a refinement of the logical model for a specific database management system.

Only the tables needed by the application should be created.