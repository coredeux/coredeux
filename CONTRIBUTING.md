# Contributing to Coredeux

Thank you for your interest in contributing to Coredeux.

We welcome contributions from developers, architects, and enterprises
who want to help improve the framework.

------------------------------------------------------------------------

## Ways to Contribute

You can contribute in many ways:

-   Reporting bugs
-   Suggesting enhancements
-   Improving documentation
-   Adding new modules or utilities
-   Writing tests
-   Performance improvements
-   Refactoring for better maintainability
-   Creating sample applications

------------------------------------------------------------------------

## Before You Start

Please ensure:

-   You have read the Code of Conduct
-   An issue exists for the change you want to implement
-   The feature aligns with the roadmap or project vision
-   The design is discussed with maintainers for large changes

------------------------------------------------------------------------

## Development Setup

1.  Fork the repository\
2.  Clone your fork

``` bash
git clone https://github.com/<your-username>/coredeux.git
```

3.  Build the project

``` bash
mvn clean install
```

4.  Create a feature branch

``` bash
git checkout -b feature/<short-description>
```

------------------------------------------------------------------------

## Coding Guidelines

-   Follow clean architecture principles
-   Write readable and maintainable code
-   Follow existing package structure and naming conventions
-   Avoid breaking public APIs
-   Prefer composition over inheritance
-   Ensure multi-tenant safety in all modules

------------------------------------------------------------------------

## Testing Requirements

-   Add unit tests for new functionality
-   Maintain or improve test coverage
-   Ensure all tests pass before submitting PR

------------------------------------------------------------------------

## Commit Message Convention

Use meaningful commit messages.

Examples:

-   feat: add tenant-aware redis cache module\
-   fix: resolve lazy loading issue in export listener\
-   refactor: simplify tenant context resolution\
-   docs: update architecture diagram

------------------------------------------------------------------------

## Pull Request Process

-   Ensure the build passes
-   Ensure quality checks pass
-   Add description explaining design decisions
-   Link related issues
-   Wait for maintainer review

------------------------------------------------------------------------

## Large Feature Contributions

For major features:

-   Open a design discussion first
-   Provide architecture diagram
-   Provide sample usage
-   Provide migration considerations

------------------------------------------------------------------------

## Community Philosophy

Coredeux values:

-   Engineering excellence\
-   Enterprise readiness\
-   Long-term maintainability\
-   Performance and scalability\
-   Respectful collaboration
