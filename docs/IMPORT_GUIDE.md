# 📥 Import Guide - Documentation Complète

Guide complet pour l'import de fichiers Excel/CSV avec validation, gestion d'erreurs, et stratégies avancées.

---

## Table des Matières

1. [Configuration de base](#configuration-de-base)
2. [Template Generation](#template-generation)
3. [Validation](#validation)
4. [Failure Strategies](#failure-strategies)
5. [Batch Processing](#batch-processing)
6. [Error Handling](#error-handling)
7. [Advanced Topics](#advanced-topics)

---

## Configuration de base

### Annotation @Importable

```java
@Service
@Importable(
    entity = "User",                              // Nom entité (pour URLs)
    mapper = UserImportMapper.class,              // Mapper
    failureStrategy = FailureStrategy.SKIP_ERRORS,// Stratégie erreur
    maxRows = 5000,                               // Max lignes
    batchSize = 100,                              // Taille batch
    saveMethod = "save"                           // Méthode save (auto-détecté)
)
public class UserService { }
```

### Paramètres

| Paramètre | Type | Défaut | Description |
|-----------|------|--------|-------------|
| `entity` | String | **requis** | Nom de l'entité (ex: "User" → /api/users/import) |
| `mapper` | Class | **requis** | Classe ImportMapper |
| `failureStrategy` | Enum | SKIP_ERRORS | Stratégie en cas d'erreur |
| `maxRows` | int | 5000 | Nombre max de lignes |
| `batchSize` | int | 100 | Taille des batchs |
| `saveMethod` | String | "save" | Nom méthode save |

---

## Template Generation

### Endpoint

```
GET /api/{entity}/import/template?format={xlsx|csv}
```

### Exemple

```bash
GET /api/users/import/template?format=xlsx
```

**Télécharge:**
```
user-import-template.xlsx
┌──────────────┬──────────────┬───────────────────────────┬──────────────────┐
│  firstName * │  lastName *  │        email *            │    telephone     │
├──────────────┼──────────────┼───────────────────────────┼──────────────────┤
│    John      │     Doe      │    john.doe@example.com   │  +221771234567   │
└──────────────┴──────────────┴───────────────────────────┴──────────────────┘

Ligne 1: Headers (colonnes obligatoires marquées avec *)
Ligne 2: Exemple (à supprimer ou modifier)
```

### Headers

- **Colonnes obligatoires** marquées avec `*`
- Proviennent de `getRequiredColumns()`
- **Colonnes optionnelles** sans `*`
- Proviennent de `getOptionalColumns()`

### Exemple dans Mapper

```java
@Override
public List<String> getRequiredColumns() {
    return List.of("firstName", "lastName", "email");
}

@Override
public List<String> getOptionalColumns() {
    return List.of("telephone", "address");
}

@Override
public User getExampleRow() {
    User example = new User();
    example.setFirstName("John");
    example.setLastName("Doe");
    example.setEmail("john.doe@example.com");
    example.setTelephone("+221771234567");
    return example;
}
```

---

## Validation

### 1. Validation des Headers

Avant de parser les données, la lib vérifie que **toutes les colonnes obligatoires** sont présentes.

```java
@Override
public List<String> getRequiredColumns() {
    return List.of("firstName", "lastName", "email");
}
```

**Si manquant:**
```json
{
  "success": false,
  "message": "Colonnes manquantes: [firstName, email]"
}
```

### 2. Bean Validation

Utilisez les annotations Bean Validation sur vos entités:

```java
public class User {

    @NotBlank(message = "Le prénom est requis")
    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
    private String firstName;

    @NotBlank(message = "Le nom est requis")
    private String lastName;

    @Email(message = "Email invalide")
    @NotBlank(message = "L'email est requis")
    private String email;

    @Pattern(regexp = "^\\+221[0-9]{9}$", message = "Format téléphone invalide: +221XXXXXXXXX")
    private String telephone;
}
```

**Résultat si invalide:**
```json
{
  "success": false,
  "totalRows": 100,
  "successCount": 95,
  "errorCount": 5,
  "errors": [
    {
      "row": 12,
      "field": "email",
      "value": "invalidemail",
      "message": "Email invalide"
    },
    {
      "row": 45,
      "field": "telephone",
      "value": "+221123",
      "message": "Format téléphone invalide: +221XXXXXXXXX"
    }
  ]
}
```

### 3. Validation Custom

Ajoutez de la validation custom dans votre mapper:

```java
@Override
public void validate(User user, int rowNumber) throws Exception {
    // Validation custom
    if (user.getEmail() != null && userRepository.existsByEmail(user.getEmail())) {
        throw new IllegalArgumentException("Email déjà utilisé: " + user.getEmail());
    }

    if (user.getAge() != null && user.getAge() < 18) {
        throw new IllegalArgumentException("L'utilisateur doit avoir au moins 18 ans");
    }
}
```

**Résultat:**
```json
{
  "errors": [
    {
      "row": 23,
      "field": "email",
      "value": "salif@gmail.com",
      "message": "Email déjà utilisé: salif@gmail.com"
    },
    {
      "row": 67,
      "field": "age",
      "value": "16",
      "message": "L'utilisateur doit avoir au moins 18 ans"
    }
  ]
}
```

---

## Failure Strategies

### 1. FAIL_FAST (Tout ou rien)

Arrête à la **première erreur** et rollback tout.

```java
@Importable(
    entity = "User",
    mapper = UserImportMapper.class,
    failureStrategy = FailureStrategy.FAIL_FAST
)
```

**Comportement:**
- Ligne 1-49: ✅ OK
- Ligne 50: ❌ Erreur → **STOP**
- **Aucune** donnée sauvegardée

**Réponse:**
```json
{
  "success": false,
  "totalRows": 1000,
  "successCount": 0,
  "errorCount": 1,
  "errors": [
    {
      "row": 50,
      "message": "Email invalide"
    }
  ]
}
```

**Cas d'usage:**
- Import de configuration critique
- Données financières
- Quand l'intégrité est primordiale

---

### 2. SKIP_ERRORS (Recommandé)

Continue l'import, sauve uniquement les **lignes valides**.

```java
@Importable(
    entity = "User",
    mapper = UserImportMapper.class,
    failureStrategy = FailureStrategy.SKIP_ERRORS  // ← Défaut
)
```

**Comportement:**
- Ligne 1-49: ✅ Sauvegardées
- Ligne 50: ❌ Skippée
- Ligne 51-1000: ✅ Sauvegardées

**Réponse:**
```json
{
  "success": true,
  "totalRows": 1000,
  "successCount": 998,
  "errorCount": 2,
  "errors": [
    {
      "row": 50,
      "message": "Email invalide"
    },
    {
      "row": 234,
      "message": "Téléphone requis"
    }
  ]
}
```

**Cas d'usage:**
- Import utilisateurs (skip les doublons)
- Données non critiques
- Import incrémental

---

### 3. COLLECT_ALL (Mode validation)

Collecte **toutes les erreurs** sans rien sauvegarder.

```java
@Importable(
    entity = "User",
    mapper = UserImportMapper.class,
    failureStrategy = FailureStrategy.COLLECT_ALL
)
```

**Comportement:**
- Parse toutes les 1000 lignes
- Valide toutes les lignes
- Collecte toutes les erreurs
- **Aucune** donnée sauvegardée

**Réponse:**
```json
{
  "success": false,
  "totalRows": 1000,
  "successCount": 0,
  "errorCount": 25,
  "errors": [
    { "row": 12, "message": "Email invalide" },
    { "row": 34, "message": "Téléphone requis" },
    { "row": 56, "message": "Email invalide" },
    // ... 22 autres erreurs
  ]
}
```

**Cas d'usage:**
- Validation avant import réel
- Pré-vérification fichier
- Génération rapport complet

---

## Batch Processing

### Configuration

```java
@Importable(
    entity = "User",
    mapper = UserImportMapper.class,
    batchSize = 100  // ← Insère par batch de 100
)
```

### Comment ça marche?

**Sans batch (batchSize = 1):**
```
save(user1)  → DB
save(user2)  → DB
save(user3)  → DB
...
save(user1000) → DB
= 1000 requêtes SQL
```

**Avec batch (batchSize = 100):**
```
saveAll([user1...user100])    → DB
saveAll([user101...user200])  → DB
...
saveAll([user901...user1000]) → DB
= 10 requêtes SQL
```

**Performance:**
- 1000 lignes sans batch: ~15 secondes
- 1000 lignes avec batch 100: ~2 secondes

### Méthodes Save

La lib cherche automatiquement:

**Option 1: saveAll (Recommandé)**
```java
public List<User> saveAll(List<User> users) {
    return userRepository.saveAll(users);
}
```

**Option 2: save (fallback)**
```java
public User save(User user) {
    return userRepository.save(user);
}
```

**Option 3: Custom**
```java
@Importable(
    entity = "User",
    mapper = UserImportMapper.class,
    saveMethod = "createUsers"  // ← Custom
)
public class UserService {

    public List<User> createUsers(List<User> users) {
        // Logique custom
        return userRepository.saveAll(users);
    }
}
```

---

## Error Handling

### Structure ImportError

```java
{
  "row": 45,           // Numéro ligne (commence à 2)
  "field": "email",    // Champ en erreur
  "value": "invalid",  // Valeur invalide
  "message": "Email invalide - format attendu: xxx@xxx.xxx"
}
```

### Types d'Erreurs

**1. Erreur de parsing**
```json
{
  "row": 0,
  "message": "Fichier corrompu - impossible de lire le Excel"
}
```

**2. Erreur de header**
```json
{
  "row": 1,
  "message": "Colonnes manquantes: [firstName, email]"
}
```

**3. Erreur de mapping**
```json
{
  "row": 23,
  "field": "dateCreation",
  "value": "2024-13-45",
  "message": "Date invalide"
}
```

**4. Erreur de validation**
```json
{
  "row": 67,
  "field": "email",
  "value": "invalidemail",
  "message": "Email invalide"
}
```

**5. Erreur métier**
```json
{
  "row": 89,
  "field": "email",
  "value": "salif@gmail.com",
  "message": "Email déjà utilisé"
}
```

---

## Advanced Topics

### Champs Imbriqués

```java
@Override
public Customer mapRow(Map<String, String> row, int rowNumber) {
    Customer customer = new Customer();

    // Champs simples
    customer.setCustomerNumber(row.get("customerNumber"));

    // Champs imbriqués
    PersonalIdentity identity = new PersonalIdentity();
    identity.setFirstName(row.get("firstName"));
    identity.setLastName(row.get("lastName"));
    customer.setPersonalIdentity(identity);

    return customer;
}

@Override
public List<String> getRequiredColumns() {
    return List.of("customerNumber", "firstName", "lastName");
}
```

**Template:**
```
┌────────────────┬──────────────┬──────────────┐
│ customerNumber │  firstName   │  lastName    │
├────────────────┼──────────────┼──────────────┤
│   CUST-001     │    Salif     │    Biaye     │
└────────────────┴──────────────┴──────────────┘
```

### Transformation de Données

```java
@Override
public User mapRow(Map<String, String> row, int rowNumber) {
    User user = new User();

    // Transformation date
    String dateStr = row.get("dateNaissance");
    if (dateStr != null) {
        user.setDateNaissance(LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    // Transformation enum
    String roleStr = row.get("role");
    if (roleStr != null) {
        user.setRole(UserRole.valueOf(roleStr.toUpperCase()));
    }

    // Transformation boolean
    String activeStr = row.get("isActive");
    user.setActive("true".equalsIgnoreCase(activeStr) || "oui".equalsIgnoreCase(activeStr));

    // Valeurs par défaut
    user.setStatus(UserStatus.PENDING);
    user.setDateCreation(LocalDateTime.now());

    return user;
}
```

### Pré-traitement

```java
@Override
public User mapRow(Map<String, String> row, int rowNumber) {
    User user = new User();

    // Normaliser email
    String email = row.get("email");
    if (email != null) {
        user.setEmail(email.toLowerCase().trim());
    }

    // Normaliser téléphone
    String phone = row.get("telephone");
    if (phone != null) {
        // Enlever espaces et tirets
        phone = phone.replaceAll("[\\s-]", "");
        // Ajouter +221 si manquant
        if (!phone.startsWith("+")) {
            phone = "+221" + phone;
        }
        user.setTelephone(phone);
    }

    return user;
}
```

### Warnings (non-bloquants)

```java
@Override
public void validate(User user, int rowNumber) throws Exception {
    // Validation bloquante
    if (user.getEmail() == null) {
        throw new IllegalArgumentException("Email requis");
    }

    // Warning non-bloquant
    if (user.getTelephone() == null) {
        log.warn("Row {}: Téléphone manquant pour {}", rowNumber, user.getEmail());
    }
}
```

---

## Limites

| Limite | Valeur | Raison |
|--------|--------|--------|
| Max file size | 10 MB | Configurable dans application.yml |
| Max rows | 5000 | Performance (synchrone) |
| Timeout | 2 minutes | Configurable |
| Formats | xlsx, csv | POI + OpenCSV |

**Besoin de plus?** Attendez v2.0.0 avec async processing!

---

## Exemples Complets

Voir **[EXAMPLES.md](EXAMPLES.md)** pour:
- Import users avec validation
- Import customers avec champs imbriqués
- Import produits avec transformations
- Import financial data

---

Vous maîtrisez maintenant l'import! 🎉
