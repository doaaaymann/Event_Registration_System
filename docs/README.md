# Documentation

This folder contains the main project documentation that is still relevant to the current Event Registration System implementation.

## Structure

- [`architecture/`](./architecture)
  - Contains architecture-focused assets such as the professional ERD.
  - Includes OCL business-rule documentation for the implemented system.
  - Includes design-pattern documentation that maps patterns to implementation files.
- [`srs/`](./srs)
  - Intended for the Software Requirements Specification document and supporting generation assets.
- [`../cloud/`](../cloud)
  - Contains cloud deployment notes and Kubernetes manifests for the microservices architecture.

## Notes

- Temporary render folders and empty placeholder directories were removed during cleanup.
- The project uses a microservices architecture, so some documentation diagrams show logical cross-service relationships rather than only physical database foreign keys.
