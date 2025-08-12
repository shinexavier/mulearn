# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial commit of the μLearn Framework.

### Changed
- Refactored DID resolution to use a generic ACA-Py resolver.
- Updated ACA-Py configuration to connect to the BCovrin Test ledger.
- Reorganized documentation files.

### Removed
- Redundant DID resolver implementations (`DidKeyResolver`, `DidWebResolver`).
- `von-network` components and related files.