# BankSystem — Техническа документация

Банкова система за управление на клиенти, сметки и кредити с уеб интерфейс.
Документът описва техническото решение, структурата на проекта и ключовите
компоненти от бизнес логиката.

---

## 1. Общ преглед

BankSystem е full-stack приложение, изградено от два независими модула:

| Модул | Технология | Роля |
|-------|-----------|------|
| **Backend** (`banksystem/`) | Java 21, Spring Boot 3.2 | REST API, бизнес логика, достъп до базата |
| **Frontend** (`frontend/`) | React 19, Vite | Single Page Application (SPA) клиент |

Комуникацията между двата слоя е през REST/JSON. Аутентикацията е stateless,
базирана на **JWT** токени. Базата данни по подразбиране е **MySQL**
(`spring.jpa.hibernate.ddl-auto=update` — схемата се генерира автоматично от JPA).

```
┌──────────────────┐      HTTP/JSON + JWT      ┌────────────────────────────┐
│  React SPA       │ ────────────────────────► │  Spring Boot REST API      │
│  (Vite, :5173)   │ ◄──────────────────────── │  (:8080)                   │
└──────────────────┘                           │   Controller → Service →   │
                                               │   Repository (Spring Data) │
                                               └─────────────┬──────────────┘
                                                             │ JPA / Hibernate
                                                             ▼
                                                     ┌───────────────┐
                                                     │  MySQL        │
                                                     └───────────────┘
```

---

## 2. Технологичен стек

### Backend
- **Java 21**, **Spring Boot 3.2.0** (parent POM)
- **Spring Web** — REST контролери
- **Spring Data JPA / Hibernate** — ORM и репозиторита
- **Spring Security** — защита на endpoint-и, ролеви достъп (`@EnableMethodSecurity`)
- **JJWT 0.11.5** — генериране и валидиране на JWT
- **BCrypt** — хеширане на пароли
- **Jakarta Bean Validation** — валидация на DTO/entity
- **Lombok** — намаляване на boilerplate код (getters/setters/builder)
- **MySQL Connector** (runtime); **H2** е наличен за тестове
- **Maven** — билд (`mvnw` wrapper в проекта)

### Frontend
- **React 19** + **Vite 8**
- Hash-базиран рутинг (без външна routing библиотека)
- `fetch` обвит в тънък API слой
- ESLint за статичен анализ

---

## 3. Структура на проекта

```
BankSystem/
├── banksystem/                         # Spring Boot backend
│   ├── pom.xml
│   └── src/main/java/com/banksystem/
│       ├── Application.java            # entry point + bootstrap на default admin
│       ├── config/
│       │   └── SecurityConfig.java     # SecurityFilterChain, CORS, JWT филтър
│       ├── controller/                 # REST endpoint-и
│       │   ├── AuthController.java
│       │   ├── AccountController.java
│       │   ├── ClientController.java
│       │   ├── LoansController.java
│       │   └── HealthController.java
│       ├── service/                    # Бизнес логика
│       │   ├── EmployeeService.java
│       │   ├── AccountService.java
│       │   ├── ClientService.java
│       │   ├── LoanService.java
│       │   └── RepaymentPlanService.java
│       ├── repository/                 # Spring Data JPA интерфейси
│       ├── model/                      # JPA entities
│       ├── dto/                        # Request/Response обекти
│       ├── exception/                  # Доменни изключения + global handler
│       └── security/                   # JwtUtil, JwtAuthenticationFilter
│   └── src/main/resources/
│       ├── application.properties      # DB, JPA, JWT конфигурация
│       └── data.sql                    # Начални данни
│
└── frontend/                           # React SPA
    └── src/
        ├── App.jsx                     # Root компонент + рутинг/навигация
        ├── pages/                      # Екрани (Login, Loans, Accounts, ...)
        ├── components/                 # Преизползваеми UI компоненти
        ├── data/                       # Конфигурация на страници/типове кредити
        └── lib/                        # api.js (HTTP), auth.js (сесия), format.js
```

---

## 4. Слоеста архитектура (backend)

Прилага се класически **layered architecture** модел:

```
Controller  ──►  Service  ──►  Repository  ──►  Database
   (HTTP)       (бизнес        (Spring Data
                 логика)         JPA)
```

- **Controller** — приема HTTP заявки, валидира входа (`@Valid`), мапва
  изключения към HTTP статуси, делегира към service-а. Не съдържа бизнес логика.
- **Service** — цялата бизнес логика и транзакции (`@Transactional`).
- **Repository** — `JpaRepository` интерфейси с derived queries
  (напр. `findByEmail`, `existsByIban`, `findByLoanOrderByMonthNumberAsc`).
- **DTO** — изолира API контракта от вътрешния доменен модел; entity-тата
  не се сериализират директно навън (с малки изключения).

---

## 5. Доменен модел

### Йерархия на клиентите
`Client` е абстрактен entity с **TABLE_PER_CLASS** наследяване и две конкретни
имплементации:

- **`Customer`** — физическо лице; идентификатор = ЕГН (UCN, 10 цифри).
- **`Merchant`** — фирма; идентификатор = ЕИК (EIK, 9 или 13 цифри).

### Основни entity-та

| Entity | Описание | Ключови полета |
|--------|----------|----------------|
| `Client` (abstract) | Базов клиент | `id`, времеви маркери |
| `Customer` | Физическо лице | `ucn`, `firstName`, `lastName` |
| `Merchant` | Фирма | `eik`, `companyName`, представител |
| `Employee` | Потребител на системата | `email`, `encryptedPassword`, `role` |
| `Account` | Банкова сметка | `iban`, `balance`, `status`, `type` |
| `LoanType` | Продукт/тип кредит | `interestRate`, `maxTermMonths`, `maxAmount` |
| `Loan` | Кредит | `initialAmount`, `remainingAmount`, `monthlyPayment`, `status` |
| `Repayment` | Една вноска от погасителния план | `monthNumber`, `dueDate`, expected/actual суми, `status` |

### Изброени типове (enums)
- `Employee.Role`: `ADMIN`, `LOAN_OFFICER`, `MANAGER`, `CUSTOMER`
- `Account.AccountStatus`: `ACTIVE`, `CLOSED`; `Account.AccountType`: `CHECKING`, `SAVINGS`, `BUSINESS`, `LOAN`
- `Loan.LoanStatus`: `PENDING`, `APPROVE`, `ACTIVE`, `REJECTED`, `PAID_OFF`
- `Repayment.RepaymentStatus`: `PENDING`, `PAID`, `OVERDUE`

---

## 6. Сигурност и аутентикация

Аутентикацията е **stateless JWT**:

1. **Login** (`POST /api/auth/login`) — `EmployeeService.authenticate` намира
   потребителя по email и сравнява паролата чрез `BCryptPasswordEncoder.matches`.
   При успех `JwtUtil.generateToken` издава HS256 токен с `subject = email` и
   claim `role`. Токенът се връща на клиента.
2. **Всяка следваща заявка** носи `Authorization: Bearer <token>`.
   `JwtAuthenticationFilter` (наследник на `OncePerRequestFilter`) парсва токена,
   валидира го и поставя `Authentication` с authority `ROLE_<role>` в
   `SecurityContextHolder`.
3. **`SecurityConfig`** конфигурира:
   - `SessionCreationPolicy.STATELESS` (без HTTP сесии)
   - публични endpoint-и: `/api/auth/login`, `/api/auth/register`, `/api/health/**`
   - всичко останало изисква аутентикация
   - CORS за `localhost:5173` / `localhost:3000` (frontend dev сървъри)
   - CSRF е изключен (stateless API)
4. **Ролеви достъп** на ниво метод чрез `@PreAuthorize`, напр. преглед и
   управление на кредити е достъпно само за `ADMIN`, `LOAN_OFFICER`, `MANAGER`.

При стартиране `Application.init` (CommandLineRunner) създава default admin
(`admin@bank.com` / `admin123`), ако такъв липсва.

> **Бележка за продукция:** JWT secret-ът и DB паролата в момента са в
> `application.properties`. За реален deployment те трябва да се изнесат в
> environment променливи / secret store.

---

## 7. Ключова бизнес логика

### 7.1 Кандидатстване за кредит (`LoanService.applyForLoan`)
1. Намира/създава клиента (по `clientId` или email — `resolveFrontendClient`).
2. Резолвва `LoanType` от заявката (`resolveFrontendLoanType` мапва frontend
   стойности като `PERSONAL`, `MORTGAGE`, `AUTO` към конкретни продукти и при
   нужда ги създава с `ensureLoanType`).
3. Валидира искана сума и срок спрямо лимитите на типа
   (`maxAmount`, `maxTermMonths`) — иначе `LoanTypeCriteriaMismatchException`.
4. Създава **LOAN сметка** с уникален IBAN (`generateUniqueIban`, до 10 опита).
5. Изчислява месечната вноска и записва кредит със статус `PENDING`.

### 7.2 Преглед/одобрение от служител (`reviewLoan` / `approveLoan`)
- Само `PENDING` кредити могат да се ревюират.
- `APPROVE` → `activateLoan`: статус `ACTIVE`, активира сметката, зарежда
  баланса с отпуснатата сума и **генерира погасителен план**, ако още няма.
- `REJECT`/`DISAPPROVE` → `rejectLoan`: статус `REJECTED`, изтрива плана,
  затваря и нулира сметката.
- Записва кой служител е ревюирал кредита (`reviewedBy`).

### 7.3 Генериране на погасителен план (`RepaymentPlanService`)
- **`calculateMonthlyPayment`** — анюитетна формула:
  `PMT = P·r / (1 − (1+r)^−n)`, където `r` е месечната лихва;
  при нулева лихва — равно разпределение на главницата.
- **`generateRepaymentSchedule`** — за всеки месец изчислява лихвена и главнична
  част, остатъчна главница; последната вноска изравнява остатъка до 0.
- Всички парични стойности минават през `normalizeCurrency`
  (2 знака, `HALF_UP`, без отрицателни стойности).

### 7.4 Плащане на вноска (`markInstallmentAsPaid`)
Сложна транзакционна логика с няколко правила:
- Кредитът трябва да е на клиента и в статус `ACTIVE`.
- Може да се плаща само **следващата неплатена** вноска (по ред на `monthNumber`).
- Платената сума не може да е под очакваната; при **надплащане** е задължителна
  `OverpaymentStrategy`.
- Проверки за активна сметка и достатъчна наличност; балансът се намалява.
- При надплащане планът се преизгражда (`rebuildScheduleAfterPayment`):
  - **`REDUCE_TERM`** — запазва вноската, скъсява срока (`buildReducedTermSchedule`).
  - намаляване на вноската — преизчислява вноските за остатъчния срок
    (`buildInstallmentCostReductionSchedule`).
- При нулева остатъчна главница кредитът става `PAID_OFF`.

### 7.5 Редактиране на вноска (`updateInstallment`)
Позволява на служител да промени дата/сума на бъдеща (неплатена) вноска;
лихвата се преизчислява спрямо реалния остатък преди месеца, а следващите
вноски се регенерират.

### 7.6 Сметки (`AccountService`)
- `openAccount` — валидира клиента, генерира/проверява IBAN, валидира тип и
  първоначален депозит, създава сметка.
- `closeAccount` — затваря само сметки с нулев баланс, принадлежащи на клиента.

---

## 8. REST API (основни endpoint-и)

| Метод | Път | Достъп | Описание |
|-------|-----|--------|----------|
| POST | `/api/auth/login` | публичен | Вход, връща JWT |
| POST | `/api/auth/register` | публичен | Регистрация (роля `CUSTOMER`) |
| POST | `/api/accounts` | аутентикиран | Откриване на сметка |
| GET | `/api/accounts/{id}` | аутентикиран | Сметка по id |
| DELETE | `/api/accounts/{accountId}/clients/{clientId}` | аутентикиран | Затваряне на сметка |
| POST | `/api/clients` | аутентикиран | Добавяне на клиент |
| GET | `/api/clients/{id}` | аутентикиран | Клиент по id |
| GET | `/api/clients/{id}/loans` | аутентикиран | Кредити на клиент |
| GET | `/api/clients/{id}/loans/{loanId}` | аутентикиран | Детайли + погасителен план |
| GET | `/api/clients/{id}/accounts` | аутентикиран | Сметки на клиент |
| POST | `/api/loans/apply` | аутентикиран | Кандидатстване за кредит |
| GET | `/api/loans?status=` | ADMIN/LOAN_OFFICER/MANAGER | Списък кредити |
| GET | `/api/loans/{id}` | аутентикиран | Кредит по id |
| PATCH | `/api/loans/{id}/review` | ADMIN/LOAN_OFFICER/MANAGER | Одобрение/отказ |
| PATCH | `/api/loans/{id}` | ADMIN/LOAN_OFFICER/MANAGER | Редакция на кредит |
| POST | `/api/loans/payments` | аутентикиран | Плащане на вноска |
| PATCH | `/api/loans/{loanId}/installments/{monthNumber}` | ADMIN/LOAN_OFFICER/MANAGER | Редакция на вноска |
| GET | `/api/health/**` | публичен | Health check |

### Обработка на грешки
- `GlobalExceptionHandler` + доменни изключения (`BusinessException`,
  `ResourceNotFoundException`, `LoanTypeCriteriaMismatchException`,
  `CreditLimitExceededException`, и др.).
- Контролерите мапват изключенията към HTTP статуси (`400`, `401`, `404`,
  `409`, `500`) и връщат `ErrorResponseDTO` с описателно съобщение.

---

## 9. Frontend архитектура

- **`App.jsx`** е root компонент: държи сесията (`session`) и текущия маршрут.
  Рутингът е **hash-базиран** (`window.location.hash`) — без routing библиотека.
  `parseRouteFromHash` / `getPageFromHash` определят активната страница; видимите
  страници зависят от ролята (`getVisiblePages`) — клиентите виждат различно меню
  от служителите.
- **`lib/api.js`** — централизиран `requestJson` helper: добавя `Bearer` токена,
  сериализира JSON, нормализира грешките от backend-а.
- **`lib/auth.js`** — съхранение/четене на сесията (токен + роля) и logout.
- **`pages/`** — отделен компонент за всеки екран: вход/регистрация, начало,
  сметки, кредити (клиентски и служителски изгледи), детайли на кредит,
  управление на кредити, администриране на потребители.
- **`data/pages.js`** — декларативна конфигурация на навигацията
  (id, label, hash, разрешени роли).

---

## 10. Конфигурация и стартиране

### Backend
```bash
cd banksystem
./mvnw spring-boot:run        # стартира на http://localhost:8080
```
Конфигурация в `src/main/resources/application.properties`:
- DB: MySQL на `localhost:3306` (auto-create схема)
- `spring.jpa.hibernate.ddl-auto=update`
- JWT secret и валидност (`jwt.expiration=86400000` ≈ 24 часа)

### Frontend
```bash
cd frontend
npm install
npm run dev                   # стартира на http://localhost:5173
```
API адресът се конфигурира чрез `VITE_API_BASE_URL` (по подразбиране
`http://localhost:8080/api`).

### Default admin
При първо стартиране автоматично се създава: `admin@bank.com` / `admin123`.

---

## 11. Тестове

Unit тестове (JUnit 5 + Mockito + Spring Security Test) в
`banksystem/src/test/java/com/banksystem/`:
- `service/` — `AccountServiceTest`, `ClientServiceTest`, `EmployeeServiceTest`,
  `LoanServiceTest`
- `security/JwtUtilTest`
- `ApplicationTests` — context load smoke test

Стартиране:
```bash
cd banksystem
./mvnw test
```
