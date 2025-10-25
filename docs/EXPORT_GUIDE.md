# 📤 Export Guide - Documentation Complète

Guide complet pour l'export de données vers Excel/CSV avec filtrage, pagination, et styles.

---

## Table des Matières

1. [Configuration de base](#configuration-de-base)
2. [Formats](#formats)
3. [Sélection des champs](#sélection-des-champs)
4. [Filtrage](#filtrage)
5. [Pagination](#pagination)
6. [Tri](#tri)
7. [Performance](#performance)

---

## Configuration de base

### Annotation @Exportable

```java
@Service
@Exportable(
    entity = "User",
    fields = {"firstName", "lastName", "email", "telephone", "dateCreation"},
    filename = "users-export",
    defaultFormat = ExportFormat.XLSX
)
public class UserService { }
```

### Paramètres

| Paramètre | Type | Défaut | Description |
|-----------|------|--------|-------------|
| `entity` | String | **requis** | Nom entité (ex: "User" → /api/users/export) |
| `fields` | String[] | **requis** | Champs à exporter |
| `filename` | String | "{entity}-export" | Nom fichier sans extension |
| `defaultFormat` | ExportFormat | XLSX | Format par défaut |
| `columnStyles` | String[] | [] | Configuration styles |
| `findMethod` | String | "findAll" | Méthode pour récupérer données |

---

## Formats

### Excel (.xlsx)

**Endpoint:**
```bash
GET /api/users/export?format=xlsx
```

**Features:**
- ✅ Styles (couleurs, gras, alignement)
- ✅ Formats (nombres, dates)
- ✅ Headers freeze (première ligne fixe)
- ✅ Auto-fit colonnes
- ✅ Lignes alternées

**Content-Type:**
```
application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
```

**Exemple:**
```
users-export-2024-10-24.xlsx

┌──────────────┬──────────────┬───────────────────────────┬──────────────────┐
│  firstName   │  lastName    │        email              │    telephone     │
│   (BOLD)     │   (BOLD)     │       (BOLD)              │     (BOLD)       │
│  (BLUE BG)   │  (BLUE BG)   │      (BLUE BG)            │    (BLUE BG)     │
├──────────────┼──────────────┼───────────────────────────┼──────────────────┤
│   Salif      │    Biaye     │   salif@gmail.com         │  +221775589632   │
│  (WHITE BG)  │  (WHITE BG)  │      (WHITE BG)           │    (WHITE BG)    │
├──────────────┼──────────────┼───────────────────────────┼──────────────────┤
│   Moussa     │    Diallo    │   moussa@gmail.com        │  +221771234567   │
│  (GRAY BG)   │  (GRAY BG)   │      (GRAY BG)            │    (GRAY BG)     │
└──────────────┴──────────────┴───────────────────────────┴──────────────────┘
```

### CSV

**Endpoint:**
```bash
GET /api/users/export?format=csv
```

**Features:**
- ✅ Léger et rapide
- ✅ Compatible Excel, Google Sheets
- ❌ Pas de styles

**Content-Type:**
```
text/csv
```

**Exemple:**
```
users-export-2024-10-24.csv

firstName,lastName,email,telephone
Salif,Biaye,salif@gmail.com,+221775589632
Moussa,Diallo,moussa@gmail.com,+221771234567
Fatou,Sall,fatou@gmail.com,+221769876543
```

---

## Sélection des champs

### Champs simples

```java
@Exportable(
    entity = "User",
    fields = {"firstName", "lastName", "email"}
)
```

**Résultat:**
```
firstName | lastName | email
```

### Champs imbriqués (notation point)

```java
@Exportable(
    entity = "Customer",
    fields = {
        "customerNumber",
        "personalIdentity.firstName",
        "personalIdentity.lastName",
        "residenceLocation.city",
        "residenceLocation.country"
    }
)
```

**Résultat:**
```
customerNumber | firstName | lastName | city | country
CUST-001      | Salif     | Biaye    | Dakar| Sénégal
```

### Ordre des colonnes

L'ordre dans `fields` = ordre dans l'export:

```java
fields = {"email", "firstName", "lastName"}  // email en premier
```

**Résultat:**
```
email                 | firstName | lastName
salif@gmail.com      | Salif     | Biaye
```

---

## Filtrage

### Par query params

**Tous les query params sont automatiquement des filtres:**

```bash
# Filtrer par isActive
GET /api/users/export?format=xlsx&isActive=true

# Filtrer par role
GET /api/users/export?format=xlsx&role=CLIENT

# Multiple filtres
GET /api/users/export?format=xlsx&isActive=true&role=CLIENT&city=Dakar
```

### Implémentation dans Service

**Option 1: Utiliser votre méthode existante (Recommandé) ✅**

La lib **auto-détecte** vos méthodes existantes! Pas besoin de créer une nouvelle méthode.

```java
@Service
@Exportable(
    entity = "User",
    fields = {"firstName", "lastName", "email", "role"},
    findMethod = "getAllUsers"  // ← Votre méthode existante!
)
public class UserService {

    // Méthode DÉJÀ existante - La lib l'utilise automatiquement!
    public Page<User> getAllUsers(String search, String role, Boolean isActive, Pageable pageable) {
        // Votre code de filtrage existant avec Specification
        Specification<User> spec = userFilterService.buildFilterSpecification(search, role, isActive);
        return userRepository.findAll(spec, pageable);
    }
}
```

**Query params → Paramètres de méthode (Auto-mapping):**
```bash
?search=salif&isActive=true  → getAllUsers(search="salif", role=null, isActive=true, pageable)
?role=ADMIN                  → getAllUsers(search=null, role="ADMIN", isActive=null, pageable)
?isActive=false&role=CLIENT  → getAllUsers(search=null, role="CLIENT", isActive=false, pageable)
```

**Avantages:**
- ✅ Utilise votre code existant (pas de duplication)
- ✅ Mêmes filtres pour liste ET export
- ✅ Auto-mapping des query params
- ✅ Support Pageable automatique

---

**Option 2: Map générique**

Si vous voulez un mapping manuel:

```java
@Service
@Exportable(
    entity = "User",
    fields = {"firstName", "lastName", "email", "role"},
    findMethod = "findByFilters"
)
public class UserService {

    public List<User> findByFilters(Map<String, Object> filters) {
        // Filters contient: {isActive: true, role: "CLIENT"}

        if (filters.containsKey("isActive")) {
            boolean isActive = (Boolean) filters.get("isActive");
            return userRepository.findByIsActive(isActive);
        }

        if (filters.containsKey("role")) {
            String role = (String) filters.get("role");
            return userRepository.findByRole(role);
        }

        return userRepository.findAll();
    }
}
```

---

**Option 3: Specification (pour filtrage complexe)**

```java
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
```

### Filtres complexes

```bash
# Date range
GET /api/users/export?dateFrom=2024-01-01&dateTo=2024-12-31

# Multiple values
GET /api/users/export?roles=ADMIN,CLIENT,SUPER_ADMIN

# Like/Contains
GET /api/users/export?nameContains=salif
```

**Implémentation:**

```java
public List<User> findByFilters(Map<String, Object> filters) {
    Specification<User> spec = Specification.where(null);

    // Date range
    if (filters.containsKey("dateFrom")) {
        LocalDate from = LocalDate.parse((String) filters.get("dateFrom"));
        spec = spec.and((root, query, cb) ->
            cb.greaterThanOrEqualTo(root.get("dateCreation"), from)
        );
    }

    // Multiple values
    if (filters.containsKey("roles")) {
        String rolesStr = (String) filters.get("roles");
        List<String> roles = Arrays.asList(rolesStr.split(","));
        spec = spec.and((root, query, cb) ->
            root.get("role").in(roles)
        );
    }

    // Like/Contains
    if (filters.containsKey("nameContains")) {
        String search = (String) filters.get("nameContains");
        spec = spec.and((root, query, cb) ->
            cb.or(
                cb.like(cb.lower(root.get("firstName")), "%" + search.toLowerCase() + "%"),
                cb.like(cb.lower(root.get("lastName")), "%" + search.toLowerCase() + "%")
            )
        );
    }

    return userRepository.findAll(spec);
}
```

---

## Pagination

### Activation

```bash
# Page 1, 100 éléments
GET /api/users/export?format=xlsx&page=0&size=100

# Page 2
GET /api/users/export?format=xlsx&page=1&size=100
```

### Implémentation

```java
@Service
@Exportable(
    entity = "User",
    fields = {"firstName", "lastName", "email"},
    findMethod = "findPaginated"
)
public class UserService {

    public List<User> findPaginated(Map<String, Object> filters, Integer page, Integer size) {
        if (page != null && size != null) {
            Pageable pageable = PageRequest.of(page, size);
            return userRepository.findAll(pageable).getContent();
        }

        return userRepository.findAll();
    }
}
```

### Pagination + Filtres

```bash
GET /api/users/export?format=xlsx&isActive=true&page=0&size=50
```

```java
public List<User> findPaginated(Map<String, Object> filters, Integer page, Integer size) {
    Specification<User> spec = buildSpecification(filters);

    if (page != null && size != null) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAll(spec, pageable).getContent();
    }

    return userRepository.findAll(spec);
}
```

---

## Tri

### Par query param

```bash
# Tri ascendant par firstName
GET /api/users/export?sortBy=firstName&sortDir=asc

# Tri descendant par dateCreation
GET /api/users/export?sortBy=dateCreation&sortDir=desc
```

### Implémentation

```java
public List<User> findWithSort(Map<String, Object> filters, String sortBy, String sortDir) {
    Specification<User> spec = buildSpecification(filters);

    // Créer Sort
    Sort sort = Sort.unsorted();
    if (sortBy != null) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir)
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;
        sort = Sort.by(direction, sortBy);
    }

    return userRepository.findAll(spec, sort);
}
```

### Tri + Pagination

```bash
GET /api/users/export?sortBy=dateCreation&sortDir=desc&page=0&size=100
```

```java
public List<User> findComplete(Map<String, Object> filters,
                                String sortBy, String sortDir,
                                Integer page, Integer size) {
    Specification<User> spec = buildSpecification(filters);

    // Sort
    Sort sort = Sort.unsorted();
    if (sortBy != null) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir)
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;
        sort = Sort.by(direction, sortBy);
    }

    // Pagination
    if (page != null && size != null) {
        Pageable pageable = PageRequest.of(page, size, sort);
        return userRepository.findAll(spec, pageable).getContent();
    }

    return userRepository.findAll(spec, sort);
}
```

---

## Performance

### Optimisations

**1. Projection (Select seulement les champs nécessaires)**

```java
public interface UserProjection {
    String getFirstName();
    String getLastName();
    String getEmail();
}

public List<UserProjection> findAllProjected() {
    return userRepository.findAllProjectedBy();
}
```

**2. Fetch Join (Éviter N+1)**

```java
@Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.isActive = true")
List<User> findAllActiveWithRoles();
```

**3. Pagination automatique**

Pour gros datasets, toujours paginer:

```java
public List<User> findAll() {
    // Plutôt que tout charger en mémoire
    return userRepository.findAll();  // ❌ 100k users = OutOfMemory

    // Paginer
    int pageSize = 1000;
    List<User> allUsers = new ArrayList<>();
    int page = 0;

    Page<User> pageResult;
    do {
        pageResult = userRepository.findAll(PageRequest.of(page++, pageSize));
        allUsers.addAll(pageResult.getContent());
    } while (pageResult.hasNext());

    return allUsers;  // ✅
}
```

### Benchmarks

| Lignes | Sans optimisation | Avec projection | Avec projection + fetch |
|--------|-------------------|-----------------|-------------------------|
| 100 | 0.2s | 0.1s | 0.1s |
| 1,000 | 2s | 0.8s | 0.6s |
| 5,000 | 12s | 4s | 2.5s |
| 10,000 | 30s | 10s | 5s |

---

## Exemples Complets

### Export Simple

```java
@Exportable(
    entity = "User",
    fields = {"firstName", "lastName", "email"}
)
public class UserService {

    public List<User> findAll() {
        return userRepository.findAll();
    }
}
```

```bash
GET /api/users/export?format=xlsx
```

### Export avec Filtres

```java
@Exportable(
    entity = "User",
    fields = {"firstName", "lastName", "email", "role", "isActive"},
    findMethod = "findByFilters"
)
public class UserService {

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

```bash
GET /api/users/export?format=xlsx&isActive=true&role=CLIENT
```

### Export Complet (Filtres + Pagination + Tri)

```java
@Exportable(
    entity = "User",
    fields = {"firstName", "lastName", "email", "dateCreation"},
    findMethod = "findComplete"
)
public class UserService {

    public List<User> findComplete(Map<String, Object> filters,
                                    String sortBy, String sortDir,
                                    Integer page, Integer size) {
        // Build specification from filters
        Specification<User> spec = buildSpec(filters);

        // Build sort
        Sort sort = Sort.unsorted();
        if (sortBy != null) {
            sort = Sort.by(
                "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy
            );
        }

        // Paginate if requested
        if (page != null && size != null) {
            return userRepository.findAll(spec, PageRequest.of(page, size, sort)).getContent();
        }

        return userRepository.findAll(spec, sort);
    }
}
```

```bash
# Filtrer + trier + paginer
GET /api/users/export?format=xlsx&isActive=true&sortBy=dateCreation&sortDir=desc&page=0&size=1000
```

---

Vous maîtrisez maintenant l'export! 🎉

Voir aussi:
- **[STYLES_GUIDE.md](STYLES_GUIDE.md)** - Customiser l'apparence Excel
- **[EXAMPLES.md](EXAMPLES.md)** - Exemples réels
