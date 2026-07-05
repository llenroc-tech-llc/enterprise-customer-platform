# CustomerConnect Data Modeling Architecture

## 1. Purpose

This document defines the data modeling architecture, database design principles, schema lifecycle, and database engineering workflow for the CustomerConnect application.

CustomerConnect is a module of the Llenroc Tech Enterprise Customer Platform. The database architecture is designed to support secure user management, authorization, account verification, multi-factor authentication, password recovery, and security event auditing.

The data model is maintained as an evolving architectural artifact. Database structures will evolve as additional business capabilities are designed and implemented.

The current database engineering lifecycle is:

**Business Requirements → Logical Design → Physical Schema Definition → SQL Schema → Spring Boot Initialization → MySQL Database → Reverse Engineering → EER Validation**

This workflow establishes traceability between application requirements, database design, executable schema definitions, the deployed database structure, and architecture documentation.

---

## 2. Architecture Objectives

The CustomerConnect data architecture is designed around the following objectives:

- Maintain clear separation between business concepts and physical database implementation.
- Preserve referential integrity through explicit foreign-key relationships.
- Enforce important data integrity rules through database constraints.
- Support secure identity and access-management capabilities.
- Support account verification and password recovery workflows.
- Support multi-factor authentication workflows.
- Maintain auditable user activity and security events.
- Keep the executable SQL schema aligned with the deployed MySQL database.
- Provide visual database documentation through MySQL Workbench EER diagrams.
- Support future migration to version-controlled database migration tooling.
- Keep environment-specific credentials outside source-controlled schema files.
- Allow the database model to evolve incrementally with application capabilities.

---

## 3. Modeling Strategy

CustomerConnect uses multiple levels of data modeling to move from application requirements to a working database implementation.

### 3.1 Requirements Model

The requirements model identifies the business capabilities that require persistent data.

Current database-backed capability areas include:

- User identity
- User profiles
- Authentication state
- Account verification
- Password recovery
- Multi-factor authentication
- Role-based access control
- Security event definitions
- User activity tracking

Future business domains, including customer management and invoice management, will be incorporated into the physical schema as those application capabilities enter implementation.

Planned capabilities are not represented as implemented architecture until corresponding database objects exist.

---

### 3.2 Logical Data Model

The logical model describes business entities and their relationships independently of a specific database implementation.

The currently implemented logical domain includes:

- User
- Role
- UserRole
- Event
- UserEvent
- AccountVerification
- ResetPasswordVerification
- TwoFactorVerification

The primary relationships are:

- A user is associated with role assignments through `UserRoles`.
- A role can be referenced by user-role assignments.
- A user can generate multiple recorded user events.
- An event defines the category of activity recorded by a user event.
- Account verification records are associated with users.
- Password-reset verification records are associated with users.
- Two-factor verification records are associated with users.

The logical model will continue to evolve as additional CustomerConnect capabilities are implemented.

---

### 3.3 Physical Data Model

The physical data model translates the logical design into MySQL-specific database structures.

The physical implementation defines:

- Tables
- Columns
- MySQL data types
- Primary keys
- Foreign keys
- Unique constraints
- Check constraints
- Default values
- Referential actions
- Table relationships

The current physical implementation is represented by the executable `schema.sql` file and validated against the deployed database through MySQL Workbench reverse engineering.

---

## 4. Current Physical Schema

The current CustomerConnect schema contains eight tables:

| Table | Responsibility |
| --- | --- |
| `Users` | Stores application identity, profile, authentication state, and account configuration data. |
| `Roles` | Defines application roles and associated permission information. |
| `UserRoles` | Associates users with application roles. |
| `Events` | Defines supported security and user activity event types. |
| `UserEvents` | Records user activity and contextual audit information. |
| `AccountVerifications` | Stores account verification records associated with users. |
| `ResetPasswordVerifications` | Stores password-reset verification records and expiration timestamps. |
| `TwoFactorVerifications` | Stores temporary multi-factor authentication verification codes and expiration timestamps. |

The current schema is focused on the platform identity, access-control, verification, and security-audit foundation.

Customer and invoice domain tables will be introduced as those modules enter implementation.

---

## 5. Core Domain Model

### 5.1 Users

The `Users` table is the central identity entity within the current CustomerConnect schema.

It stores:

- User identity information
- Contact information
- Profile information
- Authentication state
- Account lock state
- MFA configuration state
- Account creation timestamp
- Profile image location

The user email address is protected by a unique constraint to prevent duplicate account identities.

The user entity acts as the parent record for multiple security and account-management relationships.

---

### 5.2 Roles and User Role Assignments

Authorization is currently modeled using:

- `Roles`
- `UserRoles`

The `Roles` table defines named application roles and associated permission information.

The `UserRoles` table associates user records with role records and maintains referential integrity through foreign-key relationships.

The current physical schema constrains `user_id` as unique in `UserRoles`. This means the current implementation allows one role-assignment record per user.

If the platform later requires multiple simultaneous roles per user, the relationship can evolve to use a composite uniqueness rule across:

`(user_id, role_id)`

That change should be implemented as an explicit schema change and reflected in the architecture documentation.

---

### 5.3 Events and User Events

Security and application activity tracking are separated into two database concerns:

- Event definitions
- User event occurrences

The `Events` table defines recognized application and security event types.

Current event categories include activities related to:

- Login attempts
- Login failures
- Successful authentication
- Profile updates
- Profile picture updates
- Role updates
- Account settings updates
- Password updates
- MFA updates

The `UserEvents` table records the occurrence of those events for individual users.

Contextual audit information currently includes:

- User reference
- Event reference
- Device information
- IP address
- Event timestamp

Separating event definitions from event occurrences provides a controlled event taxonomy and avoids repeating event descriptions throughout activity records.

---

### 5.4 Account Verification

The `AccountVerifications` table supports account verification workflows.

The table associates a verification record with a user and stores:

- User reference
- Verification URL or token value
- Verification date information

Unique constraints protect the modeled user association and verification URL.

---

### 5.5 Password Reset Verification

The `ResetPasswordVerifications` table supports password recovery workflows.

The table stores:

- User reference
- Password-reset URL or token value
- Expiration timestamp

The expiration timestamp allows the application service layer to reject expired password-reset requests.

Unique constraints protect the modeled user association and reset URL.

---

### 5.6 Two-Factor Verification

The `TwoFactorVerifications` table supports temporary verification codes used by the MFA workflow.

The table stores:

- User reference
- Verification code
- Expiration timestamp

The application service layer is responsible for validating the verification code and expiration time as part of the authentication workflow.

---

## 6. Referential Integrity Strategy

CustomerConnect uses foreign-key constraints to maintain consistency between related tables.

Dependent records that do not have an independent lifecycle may use `ON DELETE CASCADE`. This ensures that dependent records are removed when their owning user record is deleted.

Current examples include:

- User event records
- Account verification records
- Password-reset verification records
- Two-factor verification records

Reference data may use `ON DELETE RESTRICT` when removing the referenced record would leave an invalid relationship.

Examples include role and event references where restrictive deletion behavior is defined by the schema.

Referential actions are defined explicitly in `schema.sql` and should be reviewed whenever relationship behavior changes.

---

## 7. Database Naming Standards

CustomerConnect uses consistent naming conventions across the database schema.

Current conventions include:

- Table names represent domain entities or entity collections.
- Column names use `snake_case`.
- Foreign-key columns use descriptive names such as `user_id`, `role_id`, and `event_id`.
- Constraint names identify the constraint type, table, and protected column where appropriate.
- Database column mappings must remain aligned with JPA entity mappings.
- Schema changes must be reflected in both the executable schema and architecture documentation.
- Database object naming should remain consistent as new application domains are introduced.

Current examples include:

- `Users`
- `Roles`
- `UserRoles`
- `UserEvents`
- `AccountVerifications`
- `user_id`
- `role_id`
- `event_id`
- `expiration_date`
- `created_date`

Naming conventions should remain consistent across:

- MySQL schema objects
- JPA entities
- JPA column mappings
- Spring Data repositories
- Service-layer models
- API data contracts where applicable

---

## 8. Schema Source and Initialization

The current CustomerConnect schema definition is maintained at:

`backend/customerconnect-api/src/main/resources/schema.sql`

The schema file defines the database objects required by the current application implementation.

Spring Boot database initialization behavior is configured through:

`backend/customerconnect-api/src/main/resources/application.yml`

During the current development phase, SQL initialization is enabled to support local database creation and repeatable development setup.

The current development workflow uses `schema.sql` as the executable schema definition.

As the platform matures and requires incremental, environment-safe database changes, the project may adopt a versioned migration framework such as Flyway or Liquibase.

The expected future migration lifecycle is:

**Versioned Migration → Deployment Pipeline → Database Migration → Application Startup**

Migration tooling is a future architectural direction and is not part of the current database implementation.

---

## 9. Initial Physical Modeling and Forward Engineering

The initial CustomerConnect database foundation was modeled visually in MySQL Workbench.

The initial workflow was:

1. Review application requirements.
2. Identify entities and relationships.
3. Define the logical data model.
4. Create the physical database model in MySQL Workbench.
5. Define tables and columns.
6. Assign MySQL data types.
7. Configure primary keys.
8. Configure foreign keys.
9. Review relationships in the EER diagram.
10. Select **Database → Forward Engineer**.
11. Select the target CustomerConnect database connection.
12. Connect to the MySQL DBMS.
13. Review the database objects selected for generation.
14. Review the generated SQL.
15. Execute the generated SQL against the local MySQL database.
16. Verify the resulting database schema.
17. Validate database connectivity from the Spring Boot API.

This process established the initial physical database foundation.

As development progressed, `schema.sql` became the executable schema definition used by the current application development environment.

The deployed database can then be reverse engineered to validate the actual database structure against the expected architecture.

---

## 10. Reverse Engineering Workflow

Reverse engineering is used to validate and document the structure of the existing CustomerConnect MySQL database.

The current workflow is:

1. Start the CustomerConnect MySQL database.
2. Verify that the current schema has been applied successfully.
3. Open MySQL Workbench.
4. Select **Database → Reverse Engineer**.
5. Select the CustomerConnect database connection.
6. Connect to the MySQL DBMS.
7. Select the `customerconnect` schema.
8. Retrieve database objects.
9. Select the required tables and database objects.
10. Enable placement of imported objects on the diagram.
11. Execute the reverse-engineering process.
12. Review the generated EER diagram.
13. Arrange the model for architectural readability.
14. Compare the generated structure with `schema.sql`.
15. Save the resulting model or architecture image as project documentation.

Reverse engineering provides a validation point between the source-controlled schema and the actual database structure.

The current validation flow is:

**schema.sql → Spring Boot Initialization → MySQL Database → Reverse Engineering → EER Diagram → Architecture Validation**

The current reverse-engineering workflow is documented visually at:

`docs/architecture/reverse-engineering-v1.png`

---

## 11. Schema Validation Strategy

CustomerConnect currently validates database architecture at multiple levels.

### 11.1 SQL Definition Validation

The `schema.sql` file defines the expected database structure for the current development environment.

The SQL schema is reviewed for:

- Valid table definitions
- Correct data types
- Primary keys
- Foreign keys
- Unique constraints
- Check constraints
- Default values
- Referential actions

---

### 11.2 Database Execution Validation

The schema must execute successfully against the configured MySQL database.

A successful schema execution verifies that:

- SQL syntax is valid for the configured MySQL environment.
- Required database objects can be created.
- Constraints can be applied.
- Foreign-key relationships reference valid tables and columns.

---

### 11.3 Application Startup Validation

The Spring Boot application must successfully:

- Connect to MySQL
- Execute configured SQL initialization
- Initialize Hibernate
- Create the JPA `EntityManagerFactory`
- Complete application context startup

Application startup provides an integration validation point between Spring Boot configuration, the MySQL datasource, SQL initialization, and the JPA persistence layer.

---

### 11.4 Physical Model Validation

The deployed MySQL schema is reverse engineered through MySQL Workbench to generate an EER diagram.

The resulting model is reviewed to verify:

- Expected tables exist.
- Primary keys are present.
- Foreign keys are correctly connected.
- Constraints are represented.
- Relationships match the intended architecture.
- The deployed database structure matches the expected schema.

This provides visual confirmation of the actual database structure.

---

### 11.5 Hibernate Schema Management

During the current development phase, Hibernate uses `ddl-auto: update` to support active entity development.

As the entity model stabilizes, the project should move toward:

`ddl-auto: validate`

With validation enabled, Hibernate verifies that the JPA entity mappings are compatible with the database schema without modifying the schema.

The intended progression is:

**Current Development**

- `schema.sql` initializes the development schema.
- Spring Boot manages SQL initialization.
- Hibernate supports active entity development.

**Application Stabilization**

- Move toward `ddl-auto: validate`.
- Hibernate validates JPA mappings against the database.
- Schema changes remain explicit and controlled.

**Future Production Architecture**

- Introduce version-controlled database migrations.
- Apply migrations through a controlled deployment process.
- Prevent Hibernate from silently changing production database structures.

The database should have one clearly defined schema authority at each stage of the application lifecycle.

---

## 12. Security and Configuration Standards

Database credentials and environment-specific secrets must not be hard-coded in:

- `schema.sql`
- `application.yml`
- Java source files
- Repository documentation
- GitHub Actions workflow definitions

Environment-specific values should be supplied through appropriate configuration mechanisms such as:

- Environment variables
- CI/CD platform secrets
- Secret-management systems
- Cloud secret stores

Database connection configuration should separate application configuration from sensitive credentials.

Development defaults should be limited to non-sensitive values where appropriate.

Schema documentation must not contain:

- Database passwords
- Production connection strings
- Private network addresses
- Access tokens
- API keys
- Cloud credentials

---

## 13. Current Architecture Status

The current CustomerConnect database foundation includes:

- Eight physical tables
- User identity and profile persistence
- Authentication state persistence
- Role definitions
- User-role relationships
- Security event definitions
- User event auditing
- Account verification persistence
- Password-reset verification persistence
- MFA verification persistence
- Primary-key constraints
- Foreign-key relationships
- Unique constraints
- Referential actions
- Spring Boot datasource integration
- SQL schema initialization
- MySQL Workbench physical modeling
- Initial forward-engineering workflow
- Reverse-engineering validation
- EER diagram documentation

This represents the current platform security, identity, verification, and auditing foundation of CustomerConnect.

Customer and invoice business domains remain future implementation areas.

---

## 14. Architecture Evolution

The CustomerConnect data model will evolve incrementally as new application capabilities are implemented.

Planned domain areas include:

- Customer management
- Individual and institutional customer types
- Customer addresses
- Customer classification and status
- Invoice management
- Invoice line items
- Payment records
- Expanded authorization and permission modeling
- Session management
- Refresh-token persistence
- Extended security auditing

Future entities are considered architectural direction only.

They are not part of the implemented schema until corresponding database objects, application services, persistence mappings, and API capabilities are introduced.

Architecture documentation should clearly distinguish between:

- Current implemented architecture
- Approved near-term changes
- Future architectural direction

---

## 15. Architecture Principles

The CustomerConnect database architecture follows these principles.

### Requirements Drive the Model

Database structures must support identified application capabilities.

Tables should not be added solely because they may be useful in the future.

---

### The Deployed Database Must Match the Documented Architecture

Architecture diagrams should be generated from or validated against the actual database structure.

Reverse engineering is used as a validation mechanism for the current MySQL schema.

---

### Schema Changes Must Be Traceable

Database changes should be committed to source control with clear commit messages describing the purpose of the change.

Changes to:

- Tables
- Columns
- Relationships
- Constraints
- Initialization behavior

should be reflected in the appropriate architecture documentation.

---

### Application and Database Models Must Remain Aligned

JPA entities, repositories, SQL definitions, and database constraints must represent the same domain structure.

Changes to the persistence model should be evaluated across:

- Database schema
- JPA entity mappings
- Repository interfaces
- Service-layer logic
- API behavior
- Architecture documentation

---

### Security Data Requires Explicit Lifecycle Management

Verification codes, password-reset records, account-verification records, and security events require defined behavior for:

- Creation
- Validation
- Expiration
- Retention
- Deletion

These lifecycle rules should be implemented deliberately in the application and database architecture.

---

### Production Schema Changes Must Be Controlled

The current project uses a development-focused schema initialization process.

As the platform matures, production database evolution should move toward controlled, versioned database migrations rather than implicit ORM schema mutation.

---

### Documentation Must Reflect Implemented State

Architecture documentation should describe what actually exists.

Future-state architecture may be documented, but it must remain clearly separated from implemented database structures.

---

## 16. Architecture Record

The current CustomerConnect database engineering lifecycle is:

```text
Business Requirements
        │
        ▼
Logical Design
        │
        ▼
Physical Schema Definition
        │
        ▼
schema.sql
        │
        ▼
Spring Boot SQL Initialization
        │
        ▼
MySQL Database
        │
        ▼
Reverse Engineering
        │
        ▼
EER Diagram
        │
        ▼
Architecture Validation