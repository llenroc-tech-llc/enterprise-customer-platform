# Client platform foundations

CustomerConnect uses two purpose-built clients:

- `frontend/customerconnect-web`: responsive Angular browser application.
- `mobile/customerconnect-mobile`: Ionic Angular application hosted by Capacitor.

Both clients use the Spring Boot API in `backend/customerconnect-api` and align on
authentication terminology, request shapes, design tokens, accessibility, and route
intent. Platform-specific layout and persistence adapters remain separate.

## Authentication boundary

The access token is handled by a centralized session service and attached by an HTTP
interceptor. The refresh token remains an HttpOnly cookie controlled by the backend.
Feature components never read browser storage or cookies directly. Mobile persistence
uses a `SessionStoragePort`; production device builds should replace its browser
fallback with a reviewed native secure-storage adapter.

Refresh requests are single-purpose, avoid recursive interception, retry an original
request once, and clear local authentication state on renewal failure.

## API contracts

The TypeScript interfaces currently mirror the backend response envelope and safe user
summary. Once an OpenAPI specification is published, generate a versioned contracts
package and consume it from both clients. Generated transport contracts should not
replace platform-specific view models or storage implementations.

## AI extension point

Both clients currently use local unavailable-state services. They must eventually call
the Spring Boot `POST /api/ai/chat` boundary. Azure credentials and enterprise data
access must remain server-side, with tenant authorization, source controls, and audit
logging applied before any model request.
