# CustomerConnect Data Modeling Architecture

## Overview

The CustomerConnect application uses a structured data modeling process to transform application requirements into a working MySQL database schema.

The project follows this flow:

**Application Requirements → Logical Data Model → Physical Data Model → Forward Engineering → MySQL Database Schema**

The database model will evolve incrementally as CustomerConnect features are designed and implemented.

---

## 1. Application Requirements

Data modeling begins with application requirements.

The current CustomerConnect requirements are organized into three primary areas:

- Application users and security
- Customer management
- Invoice management

### Application User Requirements

The application will support:

- New user account creation
- Unique email addresses
- Email account verification
- User profile information
- User profile updates
- Password reset without requiring an active login session
- Password reset links that expire within 24 hours
- Login using email and password
- JWT-based authentication
- Refresh token support
- Account lockout after 6 failed login attempts
- Role and permission-based application access
- Two-factor authentication using the user's phone number
- User activity tracking
- Suspicious activity reporting

User activity tracking may include:

- IP address
- Device
- Browser
- Date and time
- Activity type

---

## 2. Customer Requirements

The application will support customer management.

Customer capabilities include:

- Create customer records
- Update customer information
- Store customer name and address information
- Support customers that are either a person or an institution
- Maintain customer status
- Associate invoices with customers
- Search customers by name
- Support pagination for customer search results
- Export customer information to a spreadsheet

---

## 3. Invoice Requirements

The application will support invoice management.

Invoice capabilities include:

- Create invoices
- Associate invoices with customers
- Print invoices
- Export invoice information to a spreadsheet
- Download invoices as PDF documents

Additional invoice requirements will be added as the invoice feature is designed and implemented.

---

## 4. Logical Data Model

The logical data model represents the application's business entities, attributes, and relationships.

The logical model focuses on the structure of the business domain rather than database-specific implementation details.

At this stage, the model describes concepts such as:

- User
- Role
- UserRole
- Customer
- CustomerStatus
- Invoice

The initial security model contains:

**User → UserRole ← Role**

The `UserRole` entity resolves the many-to-many relationship between users and roles.

A user may have multiple roles, and a role may be assigned to multiple users.

The logical model will expand as additional requirements are implemented.

Possible future entities include:

- Permission
- RolePermission
- UserActivity
- VerificationToken
- PasswordResetToken
- Address
- InvoiceLineItem

These entities represent planned modeling areas and should not be considered implemented until they are added to the actual application and database model.

---

## 5. Physical Data Model

The physical data model is created in MySQL Workbench.

The physical model is the database-specific refinement of the logical model.

It defines implementation details including:

- Tables
- Columns
- MySQL data types
- Primary keys
- Foreign keys
- Indexes
- Constraints
- Relationships

The current physical model contains three tables:

### Users

Stores application user information.

Current columns:

- `id BIGINT`
- `first_name VARCHAR(50)`
- `last_name VARCHAR(50)`
- `email VARCHAR(100)`
- `phone VARCHAR(20)`

### Roles

Stores application role information.

Current columns:

- `id BIGINT`
- `name VARCHAR(50)`
- `permissions VARCHAR(500)`

### UserRoles

Join table connecting users and roles.

Current columns:

- `id BIGINT`
- `user_id BIGINT`
- `role_id BIGINT`

Current foreign-key relationships:

- `UserRoles.user_id` references `Users.id`
- `UserRoles.role_id` references `Roles.id`

This structure implements the many-to-many relationship between users and roles.

---

## 6. MySQL Workbench Modeling

The physical model is designed and maintained in MySQL Workbench before being applied to the database.

The current workflow is:

1. Review application requirements.
2. Identify business entities and relationships.
3. Define the logical data model.
4. Create the physical model in MySQL Workbench.
5. Define tables and columns.
6. Assign MySQL data types.
7. Configure primary keys.
8. Configure foreign keys.
9. Validate relationships.
10. Forward engineer the model into the MySQL database.

This model-first approach allows the database structure to be reviewed visually before SQL is executed.

---

## 7. Forward Engineering

MySQL Workbench Forward Engineering is used to convert the physical data model into an actual MySQL database schema.

The process is:

1. Open the physical model in MySQL Workbench.
2. Review tables, columns, keys, and relationships.
3. Select **Database → Forward Engineer**.
4. Select the CustomerConnect local database connection.
5. Connect to the MySQL DBMS.
6. Select the database objects to generate.
7. Review the generated SQL script.
8. Execute the SQL script.
9. Verify the generated schema and tables.
10. Verify application connectivity from the Spring Boot API.

The local database connection uses the developer's locally configured CustomerConnect connection.

Database passwords and other private credentials must not be committed to the repository.

---

## 8. Current Data Model Status

The current CustomerConnect physical data model includes:

- Users table
- Roles table
- UserRoles join table
- User-to-UserRoles relationship
- Role-to-UserRoles relationship
- Primary keys
- Foreign keys
- MySQL data types
- Physical modeling in MySQL Workbench
- Forward Engineering workflow to the local MySQL database

The model will grow incrementally as CustomerConnect features are implemented.

---

## 9. Architecture Principle

The CustomerConnect database architecture must reflect the actual implemented state of the application.

Future-state entities may be documented as planned architecture, but they should not be represented as implemented database tables until they exist in the physical model and database.

The project follows this principle:

> Model what the application needs, implement what has been designed, and document what actually exists.

This approach keeps the application requirements, architecture documentation, physical database model, and application implementation aligned.