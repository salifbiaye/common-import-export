# 🎨 Styles Guide - Excel Styling

Guide complet pour customiser l'apparence de vos exports Excel avec la syntaxe simplifiée.

---

## Table des Matières

1. [Syntaxe de base](#syntaxe-de-base)
2. [Propriétés disponibles](#propriétés-disponibles)
3. [Couleurs](#couleurs)
4. [Formats](#formats)
5. [Mapping conditionnel](#mapping-conditionnel)
6. [Exemples](#exemples)

---

## Syntaxe de base

### Format

```java
"nomColonne: propriété1=valeur, propriété2=valeur, ..."
```

### Exemple simple

```java
@Exportable(
    entity = "User",
    fields = {"firstName", "lastName", "email"},
    columnStyles = {
        "firstName: width=20",              // Largeur 20
        "email: width=30, bold=true"        // Largeur 30 + gras
    }
)
```

---

## Propriétés disponibles

### Width (Largeur)

```java
"firstName: width=20"  // 20 caractères de large
```

**Unité:** Caractères (1 caractère ≈ 7 pixels)

**Valeurs typiques:**
- Nom court: `width=15`
- Email: `width=30`
- Description: `width=50`
- ID: `width=10`

### Color (Couleur du texte)

```java
"status: color=GREEN"           // Texte vert
"balance: color=GREEN|RED"      // Vert si >0, Rouge si <0
```

**Couleurs disponibles:**
- RED, GREEN, BLUE
- ORANGE, YELLOW, PURPLE
- GRAY, WHITE, BLACK

### Background (Couleur de fond)

```java
"status: bg=LIGHT_GREEN"        // Fond vert clair
"balance: bg=LIGHT_RED"         // Fond rouge clair
```

**Backgrounds disponibles:**
- LIGHT_GREEN, LIGHT_RED
- LIGHT_BLUE, LIGHT_YELLOW
- LIGHT_ORANGE, LIGHT_GRAY

### Bold (Gras)

```java
"customerNumber: bold=true"     // Texte en gras
```

### Align (Alignement)

```java
"balance: align=RIGHT"          // Aligné à droite
"status: align=CENTER"          // Centré
"description: align=LEFT"       // Aligné à gauche (défaut)
```

**Valeurs:**
- LEFT (défaut)
- CENTER
- RIGHT

### Format (Format nombre/date)

```java
"balance: format=#,##0.00"              // 1,234.56
"dateCreation: format=dd/MM/yyyy"       // 24/10/2024
"percentage: format=0.00%"              // 12.50%
```

**Formats nombres:**
- `#,##0.00` → 1,234.56
- `#,##0` → 1,235
- `0.00%` → 12.50%
- `#,##0.00 FCFA` → 1,234.56 FCFA

**Formats dates:**
- `dd/MM/yyyy` → 24/10/2024
- `MM/dd/yyyy` → 10/24/2024
- `yyyy-MM-dd` → 2024-10-24
- `dd MMM yyyy` → 24 Oct 2024

---

## Couleurs

### Couleurs de base

```java
columnStyles = {
    "firstName: color=BLUE",
    "lastName: color=GREEN",
    "email: color=PURPLE"
}
```

**Résultat:**
```
┌──────────────┬──────────────┬─────────────────────────┐
│  firstName   │  lastName    │        email            │
├──────────────┼──────────────┼─────────────────────────┤
│   Salif      │    Biaye     │   salif@gmail.com       │
│  (blue)      │   (green)    │      (purple)           │
└──────────────┴──────────────┴─────────────────────────┘
```

### Couleurs conditionnelles

**Boolean (2 couleurs):**
```java
"isActive: color=GREEN|RED"
// true → GREEN
// false → RED
```

**Nombres (2 couleurs):**
```java
"balance: color=GREEN|RED"
// Positif → GREEN
// Négatif/Zéro → RED
```

**Enum (3+ couleurs):**
```java
"status: color=GREEN|ORANGE|RED, mapping=ACTIVE:GREEN,PENDING:ORANGE,CLOSED:RED"
// ACTIVE → GREEN
// PENDING → ORANGE
// CLOSED → RED
```

### Backgrounds conditionnels

```java
"status: bg=LIGHT_GREEN|LIGHT_RED, mapping=ACTIVE:LIGHT_GREEN,BLOCKED:LIGHT_RED"
```

**Résultat:**
```
┌─────────────┐
│   status    │
├─────────────┤
│   ACTIVE    │
│ (green bg)  │
├─────────────┤
│   BLOCKED   │
│  (red bg)   │
└─────────────┘
```

### Combinaison couleur + background

```java
"status: color=WHITE, bg=GREEN"  // Texte blanc sur fond vert
```

---

## Formats

### Formats monétaires

```java
// Format simple
"balance: format=#,##0.00"
// Résultat: 1,234,567.89

// Avec devise
"balance: format=#,##0.00 FCFA"
// Résultat: 1,234,567.89 FCFA

// Avec alignement
"balance: align=RIGHT, format=#,##0.00 FCFA"
```

### Formats dates

```java
// Format français
"dateCreation: format=dd/MM/yyyy"
// Résultat: 24/10/2024

// Format US
"dateCreation: format=MM/dd/yyyy"
// Résultat: 10/24/2024

// Format ISO
"dateCreation: format=yyyy-MM-dd"
// Résultat: 2024-10-24

// Avec heure
"timestamp: format=dd/MM/yyyy HH:mm:ss"
// Résultat: 24/10/2024 14:30:00
```

### Formats pourcentages

```java
"percentage: format=0.00%"
// Input: 0.1250
// Output: 12.50%

"percentage: format=0%"
// Input: 0.1250
// Output: 13%
```

---

## Mapping conditionnel

### Mapping simple (3 valeurs)

```java
"status: color=GREEN|ORANGE|RED, mapping=ACTIVE:GREEN,PENDING:ORANGE,CLOSED:RED"
```

**Syntaxe:**
```
mapping=KEY1:COLOR1,KEY2:COLOR2,KEY3:COLOR3
```

### Mapping multiple valeurs

```java
"priority: color=RED|ORANGE|GREEN, mapping=HIGH:RED,URGENT:RED,MEDIUM:ORANGE,NORMAL:ORANGE,LOW:GREEN"
```

**Plusieurs clés peuvent avoir la même couleur:**
- HIGH, URGENT → RED
- MEDIUM, NORMAL → ORANGE
- LOW → GREEN

### Mapping avec background

```java
"status: bg=LIGHT_RED|LIGHT_ORANGE|LIGHT_GREEN, mapping=FAILED:LIGHT_RED,PENDING:LIGHT_ORANGE,SUCCESS:LIGHT_GREEN"
```

### Auto-mapping (sans spécifier)

**Boolean → 2 couleurs:**
```java
"isActive: color=GREEN|RED"
// Automatique: true=GREEN, false=RED
```

**Nombre → 2 couleurs:**
```java
"balance: color=GREEN|RED"
// Automatique: >0=GREEN, <=0=RED
```

**Enum 2 valeurs → 2 couleurs:**
```java
"gender: color=BLUE|PURPLE"
// Automatique: première valeur=BLUE, deuxième=PURPLE
```

---

## Exemples

### Exemple 1: Export Utilisateurs Simple

```java
@Exportable(
    entity = "User",
    fields = {"firstName", "lastName", "email", "telephone", "isActive"},
    columnStyles = {
        "firstName: width=20",
        "lastName: width=20",
        "email: width=30",
        "telephone: width=15",
        "isActive: color=GREEN|RED"
    }
)
```

**Résultat:**
```
┌──────────────┬──────────────┬─────────────────────────┬──────────────┬────────────┐
│  firstName   │  lastName    │        email            │  telephone   │  isActive  │
│  (width 20)  │  (width 20)  │      (width 30)         │  (width 15)  │            │
├──────────────┼──────────────┼─────────────────────────┼──────────────┼────────────┤
│   Salif      │    Biaye     │   salif@gmail.com       │ +221775...   │    ✓       │
│              │              │                         │              │  (green)   │
│   Moussa     │    Diallo    │   moussa@gmail.com      │ +221771...   │    ✗       │
│              │              │                         │              │   (red)    │
└──────────────┴──────────────┴─────────────────────────┴──────────────┴────────────┘
```

### Exemple 2: Export Financier

```java
@Exportable(
    entity = "Account",
    fields = {"accountNumber", "balance", "currency", "lastTransaction", "status"},
    columnStyles = {
        "accountNumber: width=15, bold=true",
        "balance: width=20, align=RIGHT, format=#,##0.00, color=GREEN|RED",
        "currency: width=8, align=CENTER",
        "lastTransaction: width=12, format=dd/MM/yyyy",
        "status: bg=LIGHT_GREEN|LIGHT_RED, mapping=ACTIVE:LIGHT_GREEN,BLOCKED:LIGHT_RED"
    }
)
```

**Résultat:**
```
┌──────────────┬────────────────────┬──────────┬────────────────┬─────────────┐
│ accountNumber│      balance       │ currency │ lastTransaction│   status    │
│   (bold)     │     (right)        │ (center) │                │             │
├──────────────┼────────────────────┼──────────┼────────────────┼─────────────┤
│  ACC-001     │   1,250,500.00     │   FCFA   │   24/10/2024   │   ACTIVE    │
│              │     (green)        │          │                │ (green bg)  │
│  ACC-002     │    -15,000.00      │   FCFA   │   23/10/2024   │   BLOCKED   │
│              │      (red)         │          │                │  (red bg)   │
└──────────────┴────────────────────┴──────────┴────────────────┴─────────────┘
```

### Exemple 3: Export Clients avec Statut

```java
@Exportable(
    entity = "Customer",
    fields = {"customerNumber", "fullName", "status", "customerType", "dateCreation"},
    columnStyles = {
        "customerNumber: width=15, bold=true",
        "fullName: width=30",
        "status: color=GREEN|ORANGE|RED, mapping=ACTIVE:GREEN,PENDING:ORANGE,CLOSED:RED",
        "customerType: color=BLUE",
        "dateCreation: width=12, format=dd/MM/yyyy"
    }
)
```

**Résultat:**
```
┌──────────────┬─────────────────────────┬─────────────┬───────────────┬──────────────┐
│ customerNumber│      fullName          │   status    │ customerType  │ dateCreation │
│   (bold)      │                        │             │    (blue)     │              │
├──────────────┼─────────────────────────┼─────────────┼───────────────┼──────────────┤
│  CUST-001    │  Salif Biaye           │   ACTIVE    │  PARTICULIER  │  24/10/2024  │
│              │                        │  (green)    │               │              │
│  CUST-002    │  Moussa Diallo         │  PENDING    │  ENTREPRISE   │  23/10/2024  │
│              │                        │  (orange)   │               │              │
│  CUST-003    │  Fatou Sall            │   CLOSED    │  PARTICULIER  │  22/10/2024  │
│              │                        │   (red)     │               │              │
└──────────────┴─────────────────────────┴─────────────┴───────────────┴──────────────┘
```

### Exemple 4: Style Complet

```java
@Exportable(
    entity = "Product",
    fields = {"code", "name", "price", "stock", "status", "lastUpdate"},
    columnStyles = {
        "code: width=10, bold=true",
        "name: width=40",
        "price: width=15, align=RIGHT, format=#,##0.00 FCFA, color=BLUE",
        "stock: width=10, align=CENTER, color=GREEN|ORANGE|RED, mapping=HIGH:GREEN,MEDIUM:ORANGE,LOW:RED",
        "status: width=12, align=CENTER, bg=LIGHT_GREEN|LIGHT_RED, mapping=AVAILABLE:LIGHT_GREEN,OUT_OF_STOCK:LIGHT_RED",
        "lastUpdate: width=12, format=dd/MM/yyyy"
    }
)
```

---

## Defaults Automatiques

Si vous ne spécifiez pas de style, la lib applique des defaults intelligents:

### Par type de données

| Type | Width | Align | Format |
|------|-------|-------|--------|
| String | 20 | LEFT | - |
| Number | 15 | RIGHT | #,##0.00 |
| Date | 12 | CENTER | dd/MM/yyyy |
| Boolean | 10 | CENTER | ✓/✗ |
| Enum | 15 | LEFT | Capitalize |

### Headers

**Toujours:**
- Bold: true
- Background: Blue
- Color: White
- Freeze: Ligne 1 fixe

### Lignes alternées

**Automatique:**
- Ligne paire: White background
- Ligne impaire: Light gray background

---

## Tips & Best Practices

### 1. Largeurs adaptées au contenu

```java
// Trop petit → texte tronqué
"email: width=10"  // ❌ "salif@gmai..."

// Adapté
"email: width=30"  // ✅ "salif@gmail.com"
```

### 2. Alignement selon type

```java
// Nombres → RIGHT
"balance: align=RIGHT"

// Texte → LEFT (défaut)
"name: align=LEFT"

// Status/Code → CENTER
"status: align=CENTER"
```

### 3. Formats monétaires

```java
// Avec séparateurs de milliers
"balance: format=#,##0.00"  // ✅ 1,234.56

// Sans séparateurs
"balance: format=0.00"      // ❌ 1234.56 (moins lisible)
```

### 4. Couleurs signifiantes

```java
// ✅ Bon: Vert = positif, Rouge = négatif
"balance: color=GREEN|RED"

// ❌ Éviter: couleurs arbitraires
"balance: color=PURPLE|YELLOW"
```

### 5. Ne pas surcharger

```java
// ❌ Trop de styles tue le style
"name: width=30, bold=true, color=BLUE, bg=YELLOW, align=CENTER"

// ✅ Simple et efficace
"name: width=30"
```

---

Vous maîtrisez maintenant les styles Excel! 🎨

Voir aussi:
- **[EXPORT_GUIDE.md](EXPORT_GUIDE.md)** - Guide export complet
- **[EXAMPLES.md](EXAMPLES.md)** - Exemples réels
