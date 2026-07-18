# Llenroc Tech Enterprise Platform - AI Engineering Guidelines

## Project Overview

This repository contains the Llenroc Tech Enterprise Customer Platform.

The current application is CustomerConnect, an enterprise full-stack application built with:

- Angular frontend
- Java 21
- Spring Boot 4
- Maven
- Spring Security
- Spring Data JPA
- MySQL
- Docker
- GitHub Actions

## Repository Structure

- `frontend/` - Angular applications
- `backend/` - Spring Boot services and APIs
- `docs/architecture/` - Architecture documentation
- `docs/api/` - API documentation
- `docs/diagrams/` - Architecture and system diagrams
- `docs/development/` - Development standards and contributor guidance
- `docker/` - Container configuration
- `scripts/` - Development and automation scripts
- `infrastructure/` - Infrastructure configuration
- `.github/workflows/` - CI/CD workflows

## Engineering Standards

When reviewing or modifying code:

- Preserve clear separation of concerns.
- Follow layered backend architecture.
- Keep controllers focused on HTTP concerns.
- Place business logic in the service layer.
- Use repositories for persistence access.
- Use DTOs for API boundaries where appropriate.
- Validate external input.
- Avoid exposing persistence entities directly through public APIs.
- Do not commit credentials, secrets, tokens, or environment-specific passwords.
- Prefer environment variables and externalized configuration.
- Add or update tests when behavior changes.
- Keep changes focused on the purpose of the pull request.
- Avoid unnecessary dependencies.
- Maintain backward compatibility unless a breaking change is intentional and documented.

## Backend Standards

CustomerConnect backend code should follow this general structure:

```text
controller
service
repository
entity
dto
mapper
config
exception
security