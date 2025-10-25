# 📚 Examples - Exemples Réels

Collection d'exemples complets prêts à l'emploi pour différents cas d'usage.

---

## Table des Matières

1. [User Management](#1-user-management)
2. [Customer Management](#2-customer-management)
3. [Financial Data](#3-financial-data)
4. [E-commerce Products](#4-e-commerce-products)
5. [Event Logs](#5-event-logs)

---

## 1. User Management

### Entity

```java
@Entity
public class User {
    @Id
    private String id;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @Email
    @NotBlank
    private String email;

    @Pattern(regexp = "^\\+221[0-9]{9}$")
    private String telephone;

    @NotNull
    private UserRole role;

    private Boolean isActive;

    private LocalDate dateCreation;
}
```

### Import Mapper

```java
@Component
public class UserImportMapper implements ImportMapper<User> {

    @Autowired
    private UserRepository userRepository;

    @Override
    public User mapRow(Map<String, String> row, int rowNumber) throws Exception {
        User user = new User();

        user.setFirstName(row.get("firstName"));
        user.setLastName(row.get("lastName"));

        // Normaliser email
        String email = row.get("email");
        if (email != null) {
            user.setEmail(email.toLowerCase().trim());
        }

        // Normaliser téléphone
        String phone = row.get("telephone");
        if (phone != null) {
            phone = phone.replaceAll("[\\s-]", "");
            if (!phone.startsWith("+")) {
                phone = "+221" + phone;
            }
            user.setTelephone(phone);
        }

        // Parse role
        String roleStr = row.get("role");
        if (roleStr != null) {
            user.setRole(UserRole.valueOf(roleStr.toUpperCase()));
        }

        // Defaults
        user.setActive(true);
        user.setDateCreation(LocalDate.now());

        return user;
    }

    @Override
    public List<String> getRequiredColumns() {
        return List.of("firstName", "lastName", "email", "role");
    }

    @Override
    public List<String> getOptionalColumns() {
        return List.of("telephone");
    }

    @Override
    public User getExampleRow() {
        User example = new User();
        example.setFirstName("John");
        example.setLastName("Doe");
        example.setEmail("john.doe@example.com");
        example.setTelephone("+221771234567");
        example.setRole(UserRole.CLIENT);
        return example;
    }

    @Override
    public void validate(User user, int rowNumber) throws Exception {
        // Vérifier doublon email
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email déjà utilisé: " + user.getEmail());
        }
    }
}
```

### Service

```java
@Service
@Importable(
    entity = "User",
    mapper = UserImportMapper.class,
    failureStrategy = FailureStrategy.SKIP_ERRORS,
    maxRows = 5000
)
@Exportable(
    entity = "User",
    fields = {"firstName", "lastName", "email", "telephone", "role", "isActive", "dateCreation"},
    columnStyles = {
        "firstName: width=20",
        "lastName: width=20",
        "email: width=30",
        "telephone: width=15",
        "role: color=BLUE, bold=true",
        "isActive: color=GREEN|RED",
        "dateCreation: width=12, format=dd/MM/yyyy"
    }
)
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User save(User user) {
        return userRepository.save(user);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public List<User> findByFilters(Map<String, Object> filters) {
        Specification<User> spec = Specification.where(null);

        if (filters.containsKey("isActive")) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("isActive"), filters.get("isActive"))
            );
        }

        if (filters.containsKey("role")) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("role"), filters.get("role"))
            );
        }

        return userRepository.findAll(spec);
    }
}
```

### Usage

**Template:**
```bash
GET /api/users/import/template?format=xlsx
```

**Import:**
```bash
POST /api/users/import
File: users.xlsx
```

**Export:**
```bash
# Tous les users
GET /api/users/export?format=xlsx

# Users actifs seulement
GET /api/users/export?format=xlsx&isActive=true

# Users clients seulement
GET /api/users/export?format=xlsx&role=CLIENT
```

---

## 2. Customer Management

### Entity (avec champs imbriqués)

```java
@Entity
public class Customer {
    @Id
    private String id;

    private String customerNumber;

    @Embedded
    private PersonalIdentity personalIdentity;

    @Embedded
    private ResidenceLocation residenceLocation;

    private CustomerStatus status;

    private LocalDate dateCreation;
}

@Embeddable
public class PersonalIdentity {
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
}

@Embeddable
public class ResidenceLocation {
    private String city;
    private String country;
}
```

### Import Mapper

```java
@Component
public class CustomerImportMapper implements ImportMapper<Customer> {

    @Override
    public Customer mapRow(Map<String, String> row, int rowNumber) throws Exception {
        Customer customer = new Customer();

        customer.setCustomerNumber(row.get("customerNumber"));

        // Personal Identity
        PersonalIdentity identity = new PersonalIdentity();
        identity.setFirstName(row.get("firstName"));
        identity.setLastName(row.get("lastName"));

        String dobStr = row.get("dateOfBirth");
        if (dobStr != null) {
            identity.setDateOfBirth(LocalDate.parse(dobStr, DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }

        customer.setPersonalIdentity(identity);

        // Residence
        ResidenceLocation residence = new ResidenceLocation();
        residence.setCity(row.get("city"));
        residence.setCountry(row.get("country"));
        customer.setResidenceLocation(residence);

        // Status
        customer.setStatus(CustomerStatus.PENDING);
        customer.setDateCreation(LocalDate.now());

        return customer;
    }

    @Override
    public List<String> getRequiredColumns() {
        return List.of("customerNumber", "firstName", "lastName", "city", "country");
    }

    @Override
    public List<String> getOptionalColumns() {
        return List.of("dateOfBirth");
    }

    @Override
    public Customer getExampleRow() {
        Customer example = new Customer();
        example.setCustomerNumber("CUST-001");

        PersonalIdentity identity = new PersonalIdentity();
        identity.setFirstName("Salif");
        identity.setLastName("Biaye");
        identity.setDateOfBirth(LocalDate.of(1990, 1, 15));

        ResidenceLocation residence = new ResidenceLocation();
        residence.setCity("Dakar");
        residence.setCountry("Sénégal");

        example.setPersonalIdentity(identity);
        example.setResidenceLocation(residence);

        return example;
    }
}
```

### Service

```java
@Service
@Importable(
    entity = "Customer",
    mapper = CustomerImportMapper.class
)
@Exportable(
    entity = "Customer",
    fields = {
        "customerNumber",
        "personalIdentity.firstName",
        "personalIdentity.lastName",
        "personalIdentity.dateOfBirth",
        "residenceLocation.city",
        "residenceLocation.country",
        "status",
        "dateCreation"
    },
    columnStyles = {
        "customerNumber: width=15, bold=true",
        "personalIdentity.firstName: width=20",
        "personalIdentity.lastName: width=20",
        "personalIdentity.dateOfBirth: width=12, format=dd/MM/yyyy",
        "residenceLocation.city: width=15",
        "residenceLocation.country: width=15",
        "status: color=GREEN|ORANGE|RED, mapping=ACTIVE:GREEN,PENDING:ORANGE,CLOSED:RED",
        "dateCreation: width=12, format=dd/MM/yyyy"
    }
)
public class CustomerService {
    // ... implementation
}
```

---

## 3. Financial Data

### Entity

```java
@Entity
public class Account {
    @Id
    private String id;

    private String accountNumber;

    private BigDecimal balance;

    private String currency;

    private LocalDate lastTransaction;

    private AccountStatus status;
}
```

### Service avec Formatting Avancé

```java
@Exportable(
    entity = "Account",
    fields = {"accountNumber", "balance", "currency", "lastTransaction", "status"},
    columnStyles = {
        "accountNumber: width=15, bold=true",
        "balance: width=20, align=RIGHT, format=#,##0.00 FCFA, color=GREEN|RED",
        "currency: width=8, align=CENTER",
        "lastTransaction: width=12, format=dd/MM/yyyy",
        "status: bg=LIGHT_GREEN|LIGHT_RED, mapping=ACTIVE:LIGHT_GREEN,BLOCKED:LIGHT_RED"
    }
)
public class AccountService {

    public List<Account> findByFilters(Map<String, Object> filters) {
        Specification<Account> spec = Specification.where(null);

        // Filtrer par balance positive/négative
        if (filters.containsKey("balanceType")) {
            String type = (String) filters.get("balanceType");
            if ("positive".equals(type)) {
                spec = spec.and((root, query, cb) ->
                    cb.greaterThan(root.get("balance"), BigDecimal.ZERO)
                );
            } else if ("negative".equals(type)) {
                spec = spec.and((root, query, cb) ->
                    cb.lessThan(root.get("balance"), BigDecimal.ZERO)
                );
            }
        }

        // Filtrer par status
        if (filters.containsKey("status")) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("status"), filters.get("status"))
            );
        }

        return accountRepository.findAll(spec);
    }
}
```

**Usage:**
```bash
# Comptes avec balance positive
GET /api/accounts/export?format=xlsx&balanceType=positive

# Comptes actifs seulement
GET /api/accounts/export?format=xlsx&status=ACTIVE

# Comptes bloqués avec balance négative
GET /api/accounts/export?format=xlsx&status=BLOCKED&balanceType=negative
```

---

## 4. E-commerce Products

### Entity

```java
@Entity
public class Product {
    @Id
    private String id;

    private String code;

    private String name;

    private String description;

    private BigDecimal price;

    private Integer stock;

    private StockLevel stockLevel; // HIGH, MEDIUM, LOW

    private ProductStatus status; // AVAILABLE, OUT_OF_STOCK

    private LocalDate lastUpdate;
}
```

### Import Mapper avec Validation

```java
@Component
public class ProductImportMapper implements ImportMapper<Product> {

    @Override
    public Product mapRow(Map<String, String> row, int rowNumber) throws Exception {
        Product product = new Product();

        product.setCode(row.get("code"));
        product.setName(row.get("name"));
        product.setDescription(row.get("description"));

        // Parse price
        String priceStr = row.get("price");
        if (priceStr != null) {
            product.setPrice(new BigDecimal(priceStr));
        }

        // Parse stock
        String stockStr = row.get("stock");
        if (stockStr != null) {
            int stock = Integer.parseInt(stockStr);
            product.setStock(stock);

            // Auto-calculate stock level
            if (stock > 100) {
                product.setStockLevel(StockLevel.HIGH);
            } else if (stock > 20) {
                product.setStockLevel(StockLevel.MEDIUM);
            } else {
                product.setStockLevel(StockLevel.LOW);
            }

            // Auto-calculate status
            product.setStatus(stock > 0 ? ProductStatus.AVAILABLE : ProductStatus.OUT_OF_STOCK);
        }

        product.setLastUpdate(LocalDate.now());

        return product;
    }

    @Override
    public List<String> getRequiredColumns() {
        return List.of("code", "name", "price", "stock");
    }

    @Override
    public List<String> getOptionalColumns() {
        return List.of("description");
    }

    @Override
    public void validate(Product product, int rowNumber) throws Exception {
        if (product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le prix doit être positif");
        }

        if (product.getStock() < 0) {
            throw new IllegalArgumentException("Le stock ne peut pas être négatif");
        }
    }

    @Override
    public Product getExampleRow() {
        Product example = new Product();
        example.setCode("PROD-001");
        example.setName("Produit Exemple");
        example.setDescription("Description du produit");
        example.setPrice(new BigDecimal("15000.00"));
        example.setStock(50);
        return example;
    }
}
```

### Service avec Style Avancé

```java
@Service
@Importable(
    entity = "Product",
    mapper = ProductImportMapper.class,
    failureStrategy = FailureStrategy.SKIP_ERRORS
)
@Exportable(
    entity = "Product",
    fields = {"code", "name", "price", "stock", "stockLevel", "status", "lastUpdate"},
    columnStyles = {
        "code: width=10, bold=true",
        "name: width=40",
        "price: width=15, align=RIGHT, format=#,##0.00 FCFA, color=BLUE",
        "stock: width=10, align=CENTER, bold=true",
        "stockLevel: width=12, align=CENTER, color=GREEN|ORANGE|RED, mapping=HIGH:GREEN,MEDIUM:ORANGE,LOW:RED",
        "status: width=15, align=CENTER, bg=LIGHT_GREEN|LIGHT_RED, mapping=AVAILABLE:LIGHT_GREEN,OUT_OF_STOCK:LIGHT_RED",
        "lastUpdate: width=12, format=dd/MM/yyyy"
    }
)
public class ProductService {
    // ... implementation
}
```

---

## 5. Event Logs

### Entity

```java
@Entity
public class AuditLog {
    @Id
    private String id;

    private String actorEmail;

    private String action;

    private String entity;

    private String entityId;

    private AuditStatus status; // SUCCESS, FAILED

    private String errorMessage;

    private LocalDateTime timestamp;
}
```

### Service Export Only

```java
@Service
@Exportable(
    entity = "AuditLog",
    fields = {"timestamp", "actorEmail", "action", "entity", "entityId", "status", "errorMessage"},
    columnStyles = {
        "timestamp: width=18, format=dd/MM/yyyy HH:mm:ss",
        "actorEmail: width=30",
        "action: width=12, bold=true, color=BLUE",
        "entity: width=12",
        "entityId: width=15",
        "status: width=10, align=CENTER, color=GREEN|RED, mapping=SUCCESS:GREEN,FAILED:RED",
        "errorMessage: width=50"
    }
)
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public List<AuditLog> findByFilters(Map<String, Object> filters,
                                        String sortBy, String sortDir,
                                        Integer page, Integer size) {
        Specification<AuditLog> spec = Specification.where(null);

        // Filtrer par acteur
        if (filters.containsKey("actorEmail")) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("actorEmail")), "%" + filters.get("actorEmail").toString().toLowerCase() + "%")
            );
        }

        // Filtrer par action
        if (filters.containsKey("action")) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("action"), filters.get("action"))
            );
        }

        // Filtrer par status
        if (filters.containsKey("status")) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("status"), filters.get("status"))
            );
        }

        // Date range
        if (filters.containsKey("dateFrom")) {
            LocalDateTime from = LocalDate.parse((String) filters.get("dateFrom")).atStartOfDay();
            spec = spec.and((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("timestamp"), from)
            );
        }

        if (filters.containsKey("dateTo")) {
            LocalDateTime to = LocalDate.parse((String) filters.get("dateTo")).atTime(23, 59, 59);
            spec = spec.and((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("timestamp"), to)
            );
        }

        // Sort
        Sort sort = Sort.by(Sort.Direction.DESC, "timestamp"); // Défaut: plus récent d'abord
        if (sortBy != null) {
            sort = Sort.by(
                "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy
            );
        }

        // Pagination
        if (page != null && size != null) {
            return auditLogRepository.findAll(spec, PageRequest.of(page, size, sort)).getContent();
        }

        return auditLogRepository.findAll(spec, sort);
    }
}
```

**Usage avancé:**
```bash
# Logs du mois dernier
GET /api/auditlogs/export?format=xlsx&dateFrom=2024-09-01&dateTo=2024-09-30

# Logs d'erreur seulement
GET /api/auditlogs/export?format=xlsx&status=FAILED

# Logs d'un utilisateur spécifique
GET /api/auditlogs/export?format=xlsx&actorEmail=salif@gmail.com

# Logs avec tri et pagination
GET /api/auditlogs/export?format=xlsx&sortBy=timestamp&sortDir=desc&page=0&size=1000
```

---

## Tips & Patterns Communs

### 1. Validation avec Repository

```java
@Override
public void validate(User user, int rowNumber) throws Exception {
    // Check email unique
    if (userRepository.existsByEmail(user.getEmail())) {
        throw new IllegalArgumentException("Email déjà utilisé");
    }

    // Check username unique
    if (user.getUsername() != null && userRepository.existsByUsername(user.getUsername())) {
        throw new IllegalArgumentException("Username déjà utilisé");
    }
}
```

### 2. Transformation Dates

```java
// Plusieurs formats de date acceptés
String dateStr = row.get("dateNaissance");
if (dateStr != null) {
    try {
        // Essayer dd/MM/yyyy
        user.setDateNaissance(LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    } catch (Exception e1) {
        try {
            // Essayer yyyy-MM-dd
            user.setDateNaissance(LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE));
        } catch (Exception e2) {
            throw new IllegalArgumentException("Format de date invalide. Formats acceptés: dd/MM/yyyy ou yyyy-MM-dd");
        }
    }
}
```

### 3. Defaults Intelligents

```java
@Override
public Customer mapRow(Map<String, String> row, int rowNumber) {
    Customer customer = new Customer();

    // ... mapping ...

    // Defaults intelligents
    customer.setStatus(CustomerStatus.PENDING);
    customer.setCustomerType(CustomerType.PARTICULIER);
    customer.setDateCreation(LocalDate.now());
    customer.setCreatedBy(getCurrentUser());

    return customer;
}
```

### 4. Export avec Calculs

```java
@Exportable(
    entity = "User",
    fields = {"firstName", "lastName", "totalOrders", "totalSpent", "averageOrderValue"}
)
public class UserService {

    public List<UserWithStats> findAll() {
        List<User> users = userRepository.findAll();

        return users.stream()
            .map(user -> {
                UserWithStats stats = new UserWithStats(user);
                stats.setTotalOrders(orderRepository.countByUserId(user.getId()));
                stats.setTotalSpent(orderRepository.sumAmountByUserId(user.getId()));
                stats.setAverageOrderValue(stats.getTotalSpent().divide(new BigDecimal(stats.getTotalOrders())));
                return stats;
            })
            .collect(Collectors.toList());
    }
}
```

---

Vous avez maintenant tous les exemples nécessaires! 🎉

Ces patterns couvrent 90% des cas d'usage réels.
