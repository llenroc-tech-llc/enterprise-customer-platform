# Contributing Guidelines

## Branch Naming Convention

Use the following branch naming format:

<type>/<short-description>

### Allowed Types

- feature/ - New functionality
- fix/ - Bug fixes
- ci/ - CI/CD pipeline changes
- docs/ - Documentation changes
- refactor/ - Code restructuring without behavior changes
- test/ - Test additions or updates
- chore/ - Maintenance and repository tasks

### Examples

feature/customer-management
feature/angular-dashboard
fix/customer-validation
ci/backend-pipeline
docs/platform-architecture
refactor/customer-service
test/customer-controller
chore/dependency-updates

## Commit Message Convention

Use Conventional Commit style:

<type>: <short description>

### Allowed Types

- feat: New functionality
- fix: Bug fix
- ci: CI/CD changes
- docs: Documentation changes
- refactor: Code restructuring
- test: Test changes
- chore: Maintenance tasks
- build: Build system or dependency changes

### Examples

feat: add customer registration endpoint
fix: resolve database connection configuration
ci: add CustomerConnect backend build workflow
docs: add platform architecture documentation
refactor: simplify customer service validation
test: add customer controller integration tests
chore: update project configuration
build: update Maven dependencies

## Pull Request Titles

Pull request titles must follow the same Conventional Commit format.

Example:

feat: add customer management API