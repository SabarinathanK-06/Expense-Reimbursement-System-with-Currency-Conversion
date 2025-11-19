# Expense Reimbursement System with Currency Conversion

A complete Java Spring Boot application implementing an Expense Reimbursement System with built-in currency conversion. This repository contains a full backend implementation (REST API) in Java using Spring Boot, covering expense submission, approvals, user/role management, reporting, and currency conversion for multi-currency reimbursements.

> NOTE: This README assumes the codebase is a full Java Spring Boot application (backend). If you also have a frontend, add instructions for running it and links to the frontend project.

## Key features

- User management (employees, managers, admins)
- Expense submission with multiple currency support
- Automatic currency conversion using external exchange-rate provider (configurable)
- Expense approval workflow (submit → manager review → finance payout)
- RESTful APIs for all core flows
- Persistence with Spring Data JPA (supports H2 for dev, MySQL/Postgres for production)
- Input validation and consistent error responses
- Unit & integration tests (JUnit + Spring Test)
- API documentation (Swagger/OpenAPI)
- Dockerfile + optional docker-compose for easy local setup

## Tech stack

- Java 21 
- Spring Boot (Web, Data JPA, Security)
- Spring Security (JWT or form-based — adjust to repo specifics)
- Spring Data JPA (Hibernate)
- PostgreSQL (production)
- REST controllers, DTOs, services, repositories
- MapStruct (optional) for DTO mapping
- Swagger / Springdoc OpenAPI for API docs
- HTTP client ( WebClient) for exchange-rate calls
- Maven build tool
  
## Configuration

Application properties live in `src/main/resources/application.properties`.

Example (application.yml) snippets you may need to set:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:db;DB_CLOSE_DELAY=-1
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

app:
  currency:
    provider: EXTERNAL    # EXTERNAL or MOCK
    api:
      url: https://api.exchangeratesapi.io/latest
      key: ${EXCHANGE_API_KEY:}   # set in env or CI secrets
  security:
    jwt:
      secret: ${JWT_SECRET:change-me}
      expiration-ms: 3600000
```

Set environment variables for production:
- EXCHANGE_API_KEY — (optional) API key for the exchange rate provider
- SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD — production DB
- JWT_SECRET — JWT signing secret

## Running locally

Using Maven:

1. Build the project:
   mvn clean package

2. Run the application:
   mvn spring-boot:run
   or
   java -jar target/expense-reimbursement-system-0.0.1-SNAPSHOT.jar

By default the app runs on port 8080. Visit:
- API base: http://localhost:8080/api
- Swagger UI : http://localhost:8080/swagger-ui.html or /swagger-ui/index.html

## Database
- Production: configure a persistent DB (Postgres). Ensure `spring.datasource.*` properties point to the production DB and that JPA ddl-auto is set appropriately (validate/update depending on workflow).

Basic schema concepts:
- User (id, username, password, roles, profile fields)
- Role (EMPLOYEE, FINANCE_ADMIN, SUPER_ADMIN)
- Expense (id, user_id, amount, currency, converted_amount, converted_currency, category, submitted_at, status, manager_comments)
- ExpenseAttachment (id, expense_id, filename, url or blob)
- Approval (id, expense_id, approver_id, status, decision_at, comments)
- ExchangeRateCache (optional) to reduce external API calls

## API overview

Common endpoints (sample):

- Authentication
  - POST /api/auth/login — login, returns JWT
  - POST /api/auth/register — create new user (if enabled)

- Users
  - GET /api/users — list users (admin)
  - GET /api/users/{id} — user details
  - PUT /api/users/{id} — update user

- Expenses
  - POST /api/expenses — submit new expense (amount + currency + attachments)
  - GET /api/expenses — list expenses (with filtering by user, status, date)
  - GET /api/expenses/{id} — get expense
  - PUT /api/expenses/{id}/submit — update/submit
  - POST /api/expenses/{id}/approve — manager approve/reject
  - GET /api/expenses/reports — aggregated reporting

- Currency
  - GET /api/currency/rates?base=USD&symbols=EUR,INR — get rates from provider/cache
  - POST /api/currency/convert — convert amount between currencies

Example cURL to submit expense (replace token):
curl -X POST http://localhost:8080/api/expenses \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 100.00,
    "currency": "EUR",
    "category": "Travel",
    "date": "2025-11-15",
    "description": "Taxi fare from airport"
  }'

## Currency conversion behavior

- When an expense is submitted, the service:
  - Fetches or uses cached exchange rates to convert submitted currency into the organization's base currency (e.g., USD).
  - Stores both original amount/currency and converted amount/baseCurrency.
  - The exchange rate provider is configurable; there is a fallback/mock implementation for development.

## Testing

- Run unit and integration tests:
  mvn test

- Tests cover services, repositories, controllers, and currency conversion logic (mocking external API calls).

## Extensibility & Notes

- Add support for file storage (S3, local FS) for attachments.
- Add audit logs for approvals and changes.
- Improve security by integrating OAuth2 / OpenID Connect for SSO.
- Add rate-limit and caching for exchange-rate calls to avoid hitting external API limits.

## Troubleshooting

- If the app fails to start related to DB:
  - Check `spring.datasource.*` environment variables.
  - Ensure DB is accessible and correct JDBC driver is on the classpath.

- If currency API calls fail:
  - Check EXCHANGE_API_KEY and provider URL in config.
  - Use MOCK provider for local development.

## Contributing

Contributions are welcome. Suggested workflow:
1. Fork the repo
2. Create a feature branch: git checkout -b feature/your-feature
3. Make changes, add tests
4. Run tests locally: mvn test
5. Open a pull request describing your changes

Please follow the project's code style and add tests for any new functionality.

## License

Specify the repository license here (e.g., MIT, Apache-2.0). If none, add a LICENSE file or contact the maintainer.

## Contact / Maintainer

Maintained by @SabarinathanK-06

If you want me to tailor this README to the exact code (list real endpoints, DB tables, env vars, or include README badges and screenshots), point me to specific source files or tell me which details to include and I will update it accordingly.
