# 🚀 Getting Started with common-import-export

Ce guide vous montre comment intégrer `common-import-export` dans votre microservice en 10 minutes.

---

## 📦 Installation

### 1. Ajouter JitPack repository

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

### 2. Ajouter la dépendance

```xml
<dependency>
    <groupId>com.github.salifbiaye</groupId>
    <artifactId>common-import-export</artifactId>
    <version>v1.0.0</version>
</dependency>
```

### 3. Configuration (optionnelle)

`application.yml`:
```yaml
common:
  import-export:
    max-file-size: 10MB      # Taille max fichier (défaut: 10MB)
    max-rows: 5000           # Lignes max (défaut: 5000)
    batch-size: 100          # Batch size (défaut: 100)
    timeout: 120000          # Timeout ms (défaut: 2min)
```

---

## 📥 Votre Premier Import

### Étape 1: Créer le Mapper

Créez une classe qui implémente `ImportMapper<T>`:

```java
package com.example.user.mapper;

import com.crm_bancaire.common.importexport.mapper.ImportMapper;
import com.example.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class UserImportMapper implements ImportMapper<User> {

    @Override
    public User mapRow(Map<String, String> row, int rowNumber) throws Exception {
        User user = new User();

        // Map les colonnes → entité
        user.setFirstName(row.get("firstName"));
        user.setLastName(row.get("lastName"));
        user.setEmail(row.get("email"));
        user.setTelephone(row.get("telephone"));

        return user;
    }

    @Override
    public List<String> getRequiredColumns() {
        // Colonnes obligatoires
        return List.of("firstName", "lastName", "email");
    }

    @Override
    public List<String> getOptionalColumns() {
        // Colonnes optionnelles
        return List.of("telephone");
    }

    @Override
    public User getExampleRow() {
        // Exemple pour le template
        User example = new User();
        example.setFirstName("John");
        example.setLastName("Doe");
        example.setEmail("john.doe@example.com");
        example.setTelephone("+221771234567");
        return example;
    }
}
```

### Étape 2: Annoter le Service

```java
package com.example.user.service;

import com.crm_bancaire.common.importexport.annotation.Importable;
import com.crm_bancaire.common.importexport.enums.FailureStrategy;
import com.example.user.mapper.UserImportMapper;
import org.springframework.stereotype.Service;

@Service
@Importable(
    entity = "User",                              // Nom de l'entité
    mapper = UserImportMapper.class,              // Mapper créé ci-dessus
    failureStrategy = FailureStrategy.SKIP_ERRORS,// Skip les erreurs
    maxRows = 5000,                               // Max 5000 lignes
    batchSize = 100                               // Batch de 100
)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // IMPORTANT: Méthode save pour l'import
    public User save(User user) {
        return userRepository.save(user);
    }

    // Ou saveAll pour batch
    public List<User> saveAll(List<User> users) {
        return userRepository.saveAll(users);
    }

    // Vos autres méthodes...
}
```

### Étape 3: Tester!

**1. Télécharger le template:**
```bash
GET http://localhost:8082/api/users/import/template?format=xlsx
```

**2. Remplir le template:**
```
┌──────────────┬──────────────┬───────────────────────────┬──────────────────┐
│  firstName * │  lastName *  │        email *            │    telephone     │
├──────────────┼──────────────┼───────────────────────────┼──────────────────┤
│    John      │     Doe      │    john.doe@example.com   │  +221771234567   │  ← Exemple
├──────────────┼──────────────┼───────────────────────────┼──────────────────┤
│   Salif      │    Biaye     │   salif@gmail.com         │  +221775589632   │  ← Vos données
│   Moussa     │    Diallo    │   moussa@gmail.com        │  +221771234567   │
│   Fatou      │    Sall      │   fatou@gmail.com         │  +221769876543   │
└──────────────┴──────────────┴───────────────────────────┴──────────────────┘
```

**3. Uploader le fichier:**
```bash
POST http://localhost:8082/api/users/import
Content-Type: multipart/form-data

file: users-import.xlsx
```

**4. Réponse:**
```json
{
  "success": true,
  "totalRows": 3,
  "successCount": 3,
  "errorCount": 0,
  "duration": "0.8s",
  "errors": []
}
```

---

## 📤 Votre Premier Export

### Étape 1: Annoter le Service

```java
@Service
@Exportable(
    entity = "User",
    fields = {"firstName", "lastName", "email", "telephone", "dateCreation"},
    filename = "users-export"
)
public class UserService {

    private final UserRepository userRepository;

    // IMPORTANT: Méthode findAll pour l'export
    public List<User> findAll() {
        return userRepository.findAll();
    }

    // Vos autres méthodes...
}
```

### Étape 2: Tester!

**1. Export Excel:**
```bash
GET http://localhost:8082/api/users/export?format=xlsx
```

**2. Export CSV:**
```bash
GET http://localhost:8082/api/users/export?format=csv
```

**Résultat:** Fichier téléchargé automatiquement!

---

## 🎨 Ajouter des Styles (Optionnel)

```java
@Exportable(
    entity = "User",
    fields = {"firstName", "lastName", "email", "isActive"},
    columnStyles = {
        "firstName: width=20",
        "email: width=30",
        "isActive: color=GREEN|RED"  // Vert si true, Rouge si false
    }
)
```

---

## 📊 Import + Export Complet

```java
@Service
@Importable(
    entity = "User",
    mapper = UserImportMapper.class,
    failureStrategy = FailureStrategy.SKIP_ERRORS
)
@Exportable(
    entity = "User",
    fields = {"firstName", "lastName", "email", "telephone", "isActive"},
    columnStyles = {
        "firstName: width=20",
        "lastName: width=20",
        "email: width=30",
        "isActive: color=GREEN|RED"
    }
)
public class UserService {

    private final UserRepository userRepository;

    // Pour import
    public User save(User user) {
        return userRepository.save(user);
    }

    // Pour export
    public List<User> findAll() {
        return userRepository.findAll();
    }
}
```

**Endpoints générés automatiquement:**
```
POST /api/users/import
GET  /api/users/import/template?format=xlsx
GET  /api/users/export?format=xlsx
GET  /api/users/export?format=csv
```

---

## ✅ Checklist

- [ ] Ajouter dépendance `common-import-export`
- [ ] Créer `ImportMapper<T>` avec `@Component`
- [ ] Annoter service avec `@Importable`
- [ ] Annoter service avec `@Exportable`
- [ ] Ajouter méthode `save(T)` ou `saveAll(List<T>)`
- [ ] Ajouter méthode `findAll()`
- [ ] Tester `/import/template`
- [ ] Tester `/import` avec fichier
- [ ] Tester `/export`

---

## 🐛 Troubleshooting

### Endpoint non trouvé (404)

**Problème:** `POST /api/users/import` retourne 404

**Solutions:**
1. Vérifier que `@Importable` est sur le service
2. Vérifier que le service est un `@Service` Spring
3. Vérifier que `entity = "User"` correspond au nom attendu
4. Redémarrer l'application

### Bean not found

**Problème:** `ImportMapper bean not found`

**Solution:** Ajouter `@Component` sur votre mapper

### Save method not found

**Problème:** `No suitable save method found`

**Solution:** Ajouter une méthode `save(T)` ou `saveAll(List<T>)` dans votre service

---

## 📚 Prochaines Étapes

- Lire le **[Import Guide](IMPORT_GUIDE.md)** pour validation, error handling, etc.
- Lire le **[Export Guide](EXPORT_GUIDE.md)** pour filtres, pagination, etc.
- Lire le **[Styles Guide](STYLES_GUIDE.md)** pour customiser vos exports
- Voir les **[Examples](EXAMPLES.md)** pour cas d'usage réels

---

Félicitations! Vous avez configuré votre premier import/export! 🎉
