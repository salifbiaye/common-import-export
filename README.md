# 📊 Common Import-Export

**Automatic Excel/CSV import-export library for Spring Boot microservices**

Stop writing repetitive import/export code! Just add `@Importable` / `@Exportable` and get automatic endpoints with template generation, validation, and styled exports.

[![](https://jitpack.io/v/salifbiaye/common-import-export.svg)](https://jitpack.io/#salifbiaye/common-import-export)

---

## ✨ Features

- ✅ **@Importable annotation** - Auto-generate import endpoints
- ✅ **@Exportable annotation** - Auto-generate export endpoints
- ✅ **Template generation** - GET /{entity}/import/template with example
- ✅ **Excel dropdown lists** - User-friendly data entry with validation (NEW v1.0.1)
- ✅ **Excel (.xlsx) & CSV** support
- ✅ **Automatic validation** - Bean Validation integration
- ✅ **3 Failure Strategies** - FAIL_FAST, SKIP_ERRORS, COLLECT_ALL
- ✅ **Error handling** - Detailed error reports with row numbers
- ✅ **Styled Excel exports** - Colors, bold, alignment, formats
- ✅ **Simple config** - `"firstName: width=20, color=GREEN"`
- ✅ **Zero boilerplate** - No controller code needed
- ✅ **Synchronous** - No RabbitMQ/Kafka required (500 rows max)

---

## 🚀 Quick Start

### Installation

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.salifbiaye</groupId>
    <artifactId>common-import-export</artifactId>
    <version>v1.0.1</version>
</dependency>
```

### Usage

#### 1. Add annotations to your service:

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
    fields = {"firstName", "lastName", "email", "telephone", "isActive"},
    columnStyles = {
        "firstName: width=20",
        "email: width=30",
        "isActive: color=GREEN|RED"
    }
)
public class UserService {
    // Your normal service methods - nothing to change!
}
```

#### 2. Create your ImportMapper:

```java
@Component
public class UserImportMapper implements ImportMapper<User> {

    @Override
    public User mapRow(Map<String, String> row, int rowNumber) {
        User user = new User();
        user.setFirstName(row.get("firstName"));
        user.setLastName(row.get("lastName"));
        user.setEmail(row.get("email"));
        return user;
    }

    @Override
    public List<String> getRequiredColumns() {
        return List.of("firstName", "lastName", "email");
    }

    @Override
    public User getExampleRow() {
        User example = new User();
        example.setFirstName("John");
        example.setLastName("Doe");
        example.setEmail("john@example.com");
        return example;
    }

    // ✨ NEW v1.0.1: Dropdown lists for user-friendly data entry
    @Override
    public Map<String, List<String>> getDropdownOptions() {
        return Map.of(
            "status", List.of("ACTIVE", "PENDING", "CLOSED"),
            "priority", List.of("HIGH", "MEDIUM", "LOW")
        );
    }
}
```

**Result:** Excel template will have dropdown lists ▼ for `status` and `priority` columns - no typing errors!

#### 3. That's it! Auto-generated endpoints:

```
POST /api/users/import          ← Upload Excel/CSV
GET  /api/users/import/template ← Download template
GET  /api/users/export?format=xlsx ← Export to Excel
```

---

## 📖 Documentation

### Getting Started
- **[Getting Started Guide](docs/GETTING_STARTED.md)** - Installation & first import/export

### Detailed Guides
- **[Import Guide](docs/IMPORT_GUIDE.md)** - Complete import documentation
  - Template generation
  - Validation & error handling
  - Failure strategies
  - Batch processing

- **[Export Guide](docs/EXPORT_GUIDE.md)** - Complete export documentation
  - Format selection (Excel/CSV)
  - Field configuration
  - Filtering & pagination
  - Performance tips

- **[Styles Guide](docs/STYLES_GUIDE.md)** - Excel styling documentation
  - Simple syntax
  - Colors & backgrounds
  - Alignment & formatting
  - Conditional styling

### Examples
- **[Examples](docs/EXAMPLES.md)** - Real-world examples
  - User import/export
  - Customer import/export
  - Financial data with formatting
  - Status with colors

---

## 🎨 Simple Style Configuration

```java
@Exportable(
    entity = "User",
    fields = {"firstName", "lastName", "balance", "status"},

    // Ultra-simple syntax!
    columnStyles = {
        "firstName: width=20",
        "balance: width=15, align=RIGHT, format=#,##0.00, color=GREEN|RED",
        "status: color=GREEN|ORANGE|RED, mapping=ACTIVE:GREEN,PENDING:ORANGE,CLOSED:RED"
    }
)
```

**Result:**
- firstName: Width 20
- balance: Right-aligned, formatted as currency, green if positive, red if negative
- status: Green for ACTIVE, Orange for PENDING, Red for CLOSED

---

## 📊 Import Response Example

```json
{
  "success": true,
  "totalRows": 1000,
  "successCount": 998,
  "errorCount": 2,
  "duration": "2.3s",
  "errors": [
    {
      "row": 45,
      "field": "email",
      "value": "invalidemail",
      "message": "Email invalide - format attendu: xxx@xxx.xxx"
    },
    {
      "row": 234,
      "field": "telephone",
      "value": null,
      "message": "Téléphone requis pour le type CLIENT"
    }
  ]
}
```

---

## 🔥 Benefits

**Before** (per microservice):
- 150 lines: Controller avec MultipartFile handling
- 100 lines: Parser Excel/CSV
- 80 lines: Validation logic
- 120 lines: Export avec styles
- **Total: ~450 lines**

**After**:
```java
@Importable(entity = "User", mapper = UserImportMapper.class)
@Exportable(entity = "User", fields = {"firstName", "lastName", "email"})
```
- **Total: 2 lines** ✨

**For 50 microservices**: Save 22,500 lines of boilerplate!

---

## 🔧 Configuration (application.yml)

```yaml
common:
  import-export:
    max-file-size: 10MB      # Taille max fichier
    max-rows: 5000           # Lignes max par import
    batch-size: 100          # Taille des batchs
    timeout: 120000          # Timeout (2 min)
```

---

## 🎯 Why Synchronous?

**v1.0.0 is intentionally synchronous:**
- ✅ No RabbitMQ/Kafka required
- ✅ 5 minute integration
- ✅ Simple architecture
- ✅ 5000 rows in ~30 seconds
- ✅ Covers 95% of use cases

**Need async?** Wait for v2.0.0 with `common-async-jobs` (reusable for emails, PDFs, etc.)

---

## 📈 Roadmap

### v1.0.0 (Now)
- ✅ Excel + CSV
- ✅ Synchrone (max 5000 lignes)
- ✅ Template avec exemple
- ✅ Validation + rapport d'erreurs
- ✅ Styles simplifiés
- ✅ Controller auto-généré

### v2.0.0 (Future)
- 🔮 Async jobs avec `common-async-jobs`
- 🔮 Imports illimités
- 🔮 Job tracking + notifications
- 🔮 Export huge datasets

---

Made with ❤️ for Spring Boot Microservices
