# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added
- _New features that are not yet released go here._

### Changed
- _Changes to existing functionality go here._

### Fixed
- _Bug fixes go here._

---

## [1.0.0] - 2025-XX-XX

### Added
- **Core library** (`secret-manager-core`) with SPI interfaces, auto-configuration, and Spring Boot integration
- **`@Secret` annotation** for declarative field injection of secrets
- **`${secret://}` placeholder** resolution in Spring property sources (YAML, properties files)
- **AES-256-GCM encrypted cache** with unique IV per entry and SHA-256 hash for rotation detection
- **Automatic rotation detection** via scheduled polling with configurable intervals
- **`SecretRotationEvent`** — Spring application event fired on secret change for zero-downtime rotation handling
- **CyberArk Conjur provider** (`secret-manager-conjur`) — pure Java `HttpClient`, no vendor SDK required
- **In-memory cache** (`secret-manager-memory-cache`) — `ConcurrentHashMap`-backed encrypted cache with TTL
- **Map vault** (`secret-manager-map-vault`) — YAML-driven vault for development and local testing
- **Mock vault** (`secret-manager-mock-vault`) — testable vault with `updateSecret()` for simulating rotation
- **Demo application** (`secret-manager-demo`) — full Spring Boot app with JPA, HikariCP, rotation simulation, and REST endpoints
- **SPI plugin architecture** — add new vault providers or cache backends by implementing an interface and dropping a JAR
- **Comprehensive documentation** — architecture guide, configuration reference, security model, and custom provider tutorial

---

<!--
Template for future releases:

## [X.Y.Z] - YYYY-MM-DD

### Added
- New features

### Changed
- Changes to existing functionality

### Deprecated
- Features that will be removed in upcoming releases

### Removed
- Features removed in this release

### Fixed
- Bug fixes

### Security
- Vulnerability fixes
-->

[Unreleased]: https://github.com/your-org/secret-manager/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/your-org/secret-manager/releases/tag/v1.0.0
