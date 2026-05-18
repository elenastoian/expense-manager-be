# Smart Invoice & Expense Management Platform
Domain-Driven Design (DDD) Architecture Guide
---

## Table of Contents

1. [Application Overview](#application-overview)
2. [What is DDD?](#what-is-ddd)
3. [Core DDD Concepts](#core-ddd-concepts)
4. [Bounded Contexts](#bounded-contexts)
5. [Full Package Structure](#full-package-structure)
6. [Event Flow & Context Communication](#event-flow--context-communication)
7. [Key Design Decisions](#key-design-decisions)
8. [MVC vs DDD — Why We Made the Switch](#mvc-vs-ddd--why-we-made-the-switch)

---

## Application Overview

The **Smart Invoice & Expense Management Platform** is a full-stack enterprise-style web application designed to help small businesses and freelancers manage invoices, expenses, and financial documents in a centralized and automated way.

**Core capabilities:**

- Upload invoices and receipts
- Automatically extract key information using OCR and AI-based processing
- Categorize expenses intelligently
- Generate financial reports
- Monitor financial activity through an interactive dashboard

**Tech stack:** Java · Spring Boot · DDD Architecture · Asynchronous Processing · JWT Security · External Service Integrations (OCR, AI, Cloud Storage)

---

## What is DDD?

Domain-Driven Design (DDD) is a software development approach where the **business domain is the center of all architectural decisions**. Instead of organizing code by technical layers (controller/service/repository), it is organized by **business domains and bounded contexts**.

DDD is split into two levels of design:

| Level | Focus |
|---|---|
| **Strategic Design** | The big picture — boundaries, domains, language |
| **Tactical Design** | The building blocks — entities, aggregates, events |

---

## Core DDD Concepts

### Strategic Design

| Concept | Definition |
|---|---|
| **Domain** | The problem space the application solves (e.g. invoice & expense management) |
| **Bounded Context** | An explicit boundary within which a domain model applies. Each context has its own language and models |
| **Ubiquitous Language** | A shared vocabulary between developers and domain experts, used consistently in code and conversation |

### Tactical Design (Building Blocks)

| Concept | Definition |
|---|---|
| **Entity** | An object with a unique identity that persists over time (e.g. `Order`, `User`) |
| **Value Object** | An immutable object defined purely by its attributes, with no identity (e.g. `Money`, `Email`, `Address`) |
| **Aggregate** | A cluster of entities and value objects treated as a single unit, with one **Aggregate Root** controlling all access |
| **Aggregate Root** | The entry point to an aggregate — external objects may only reference the root, never internal entities directly |
| **Domain Event** | Something meaningful that happened in the domain (e.g. `DocumentUploaded`, `ExtractionCompleted`) |
| **Repository** | Abstracts persistence for aggregates. The interface is defined in the domain layer; the implementation lives in infrastructure |
| **Domain Service** | Stateless business logic that does not naturally belong to any single entity |
| **Application Service** | Orchestrates use cases by coordinating domain objects. Contains no business logic itself |

---

## Bounded Contexts

This application is organized into **5 bounded contexts**, each representing a distinct area of the business domain:

| Bounded Context | Responsibility |
|---|---|
| **Identity & Access** | Users, authentication, roles, and permissions |
| **Document** | Upload, storage, and lifecycle management of invoices and receipts |
| **Extraction** | OCR and AI-based processing pipeline for extracting data from documents |
| **Finance** | Expenses, categories, invoice records, and core financial logic |
| **Reporting** | Dashboard metrics, report generation, and analytics |

Each context is **fully independent**: it has its own models, its own repository interfaces, its own application services, and communicates with other contexts only through **domain events** — never through direct method calls or shared database tables.

---

## Full Package Structure

```
com.elenastoian.expense-manager
│
├── identity/                                  ← Bounded Context: Identity & Access
│   ├── domain/
│   │   ├── model/
│   │   │   ├── User.java                      ← Aggregate Root
│   │   │   ├── Role.java                      ← Entity
│   │   │   └── Email.java                     ← Value Object
│   │   ├── event/
│   │   │   └── UserRegistered.java            ← Domain Event
│   │   ├── repository/
│   │   │   └── UserRepository.java            ← Interface (port)
│   │   └── service/
│   │       └── PasswordPolicy.java            ← Domain Service
│   ├── application/
│   │   └── UserApplicationService.java        ← Register, login, update profile
│   └── infrastructure/
│       ├── persistence/
│       │   └── JpaUserRepository.java         ← Repository implementation
│       ├── security/
│       │   └── JwtTokenProvider.java          ← JWT adapter
│       └── web/
│           └── AuthController.java            ← HTTP adapter
│
├── document/                                  ← Bounded Context: Document
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Document.java                  ← Aggregate Root
│   │   │   ├── DocumentType.java              ← Enum (INVOICE, RECEIPT)
│   │   │   └── StoragePath.java               ← Value Object
│   │   ├── event/
│   │   │   └── DocumentUploaded.java          ← Domain Event → triggers Extraction
│   │   ├── repository/
│   │   │   └── DocumentRepository.java        ← Interface (port)
│   │   └── service/
│   │       └── DocumentValidator.java         ← Domain Service (format/size rules)
│   ├── application/
│   │   └── DocumentApplicationService.java    ← Upload, delete, fetch
│   └── infrastructure/
│       ├── persistence/
│       │   └── JpaDocumentRepository.java
│       ├── storage/
│       │   └── S3StorageAdapter.java          ← Cloud storage integration
│       └── web/
│           └── DocumentController.java
│
├── extraction/                                ← Bounded Context: Extraction
│   ├── domain/
│   │   ├── model/
│   │   │   ├── ExtractionJob.java             ← Aggregate Root
│   │   │   ├── ExtractionStatus.java          ← Enum (PENDING, PROCESSING, DONE, FAILED)
│   │   │   └── ExtractedData.java             ← Value Object (vendor, amount, date, etc.)
│   │   ├── event/
│   │   │   └── ExtractionCompleted.java       ← Domain Event → triggers Finance
│   │   ├── repository/
│   │   │   └── ExtractionJobRepository.java
│   │   └── service/
│   │       └── ExtractionRules.java           ← Domain Service (field validation)
│   ├── application/
│   │   └── ExtractionApplicationService.java  ← Orchestrates OCR + AI pipeline
│   └── infrastructure/
│       ├── persistence/
│       │   └── JpaExtractionJobRepository.java
│       ├── ocr/
│       │   └── TesseractOcrAdapter.java       ← OCR integration
│       ├── ai/
│       │   └── OpenAiExtractionAdapter.java   ← AI/LLM integration
│       └── web/
│           └── ExtractionController.java
│
├── finance/                                   ← Bounded Context: Finance
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Expense.java                   ← Aggregate Root
│   │   │   ├── Invoice.java                   ← Aggregate Root
│   │   │   ├── Category.java                  ← Entity
│   │   │   ├── Money.java                     ← Value Object
│   │   │   └── TaxRate.java                   ← Value Object
│   │   ├── event/
│   │   │   └── ExpenseCategorized.java        ← Domain Event → triggers Reporting
│   │   ├── repository/
│   │   │   ├── ExpenseRepository.java
│   │   │   └── InvoiceRepository.java
│   │   └── service/
│   │       └── ExpenseCategoryPolicy.java     ← Domain Service (auto-categorization rules)
│   ├── application/
│   │   └── FinanceApplicationService.java     ← Create expense, link document, categorize
│   └── infrastructure/
│       ├── persistence/
│       │   ├── JpaExpenseRepository.java
│       │   └── JpaInvoiceRepository.java
│       └── web/
│           └── FinanceController.java
│
├── reporting/                                 ← Bounded Context: Reporting
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Report.java                    ← Aggregate Root
│   │   │   ├── ReportType.java                ← Enum (MONTHLY, CATEGORY, TAX)
│   │   │   └── DateRange.java                 ← Value Object
│   │   ├── repository/
│   │   │   └── ReportRepository.java
│   │   └── service/
│   │       └── ReportGenerationService.java   ← Domain Service
│   ├── application/
│   │   └── ReportingApplicationService.java   ← Generate, export, schedule reports
│   └── infrastructure/
│       ├── persistence/
│       │   └── JpaReportRepository.java
│       ├── export/
│       │   └── PdfReportExporter.java         ← PDF generation adapter
│       └── web/
│           └── ReportController.java
│
└── shared/                                    ← Shared Kernel
    ├── domain/
    │   ├── AggregateRoot.java                 ← Base class with domain events list
    │   ├── DomainEvent.java                   ← Marker interface
    │   └── ValueObject.java                   ← Base class/interface
    └── infrastructure/
        ├── eventbus/
        │   └── SpringDomainEventPublisher.java ← Wraps ApplicationEventPublisher
        └── config/
            └── SecurityConfig.java
```

---

## Event Flow & Context Communication

Bounded contexts are **decoupled** and communicate exclusively through **domain events**. No context calls another context's application service or repository directly.

```
┌─────────────────────────────────────────────────────────┐
│  User uploads a file                                    │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
            ┌────────────────────────┐
            │   Document Context     │
            │  Stores file, validates│
            │  Publishes:            │
            │  → DocumentUploaded   │
            └────────────┬───────────┘
                         │
                         ▼
            ┌────────────────────────┐
            │  Extraction Context    │
            │  Listens: DocumentUploaded
            │  Runs OCR + AI async   │
            │  Publishes:            │
            │  → ExtractionCompleted │
            └────────────┬───────────┘
                         │
                         ▼
            ┌────────────────────────┐
            │   Finance Context      │
            │  Listens: ExtractionCompleted
            │  Creates Expense/Invoice│
            │  Auto-categorizes      │
            │  Publishes:            │
            │  → ExpenseCategorized  │
            └────────────┬───────────┘
                         │
                         ▼
            ┌────────────────────────┐
            │  Reporting Context     │
            │  Listens: ExpenseCategorized
            │  Updates dashboard     │
            │  Refreshes report data │
            └────────────────────────┘
```

---

## Key Design Decisions

### Async Processing in Extraction
OCR and AI processing are slow operations. The `ExtractionApplicationService` processes jobs **asynchronously** using Spring's `@Async` annotation or a message queue (e.g. RabbitMQ or Kafka), preventing HTTP timeouts and keeping the system responsive.

### Anti-Corruption Layer (ACL) Between Contexts
`ExtractedData` (owned by the Extraction context) is **translated** into `Expense` or `Invoice` (owned by the Finance context) via a dedicated mapper/translator. The two contexts never share model classes — each speaks its own language.

### Document Context Owns the File, Finance Owns the Record
The `Document` and `Expense`/`Invoice` aggregates are linked only by a `documentId` reference — not a JPA `@OneToOne` join across context boundaries. This preserves context independence.

### Reporting Uses Its Own Read Model (CQRS)
The Reporting context maintains its own read-optimized projections (tables or views), fed by domain events from Finance. It never queries Finance's tables directly. This follows the **Command Query Responsibility Segregation (CQRS)** pattern.

### Domain Layer Has Zero Framework Dependencies
The `domain` package in each context is **pure Java**. No Spring annotations, no JPA, no external libraries. This keeps the core business logic fully portable and easily unit-testable.

---
