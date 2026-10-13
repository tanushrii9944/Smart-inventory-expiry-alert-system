[README (2).md](https://github.com/user-attachments/files/33367878/README.2.md)
# Smart Inventory Expiry Alert System

A lightweight, console-based Java application that helps shops, pharmacies, kitchens and warehouses keep track of stock and **get alerted before products expire** — reducing waste and financial loss.

## Features

- Add products with name, category, quantity and expiry date
- Automatic expiry alerts shown on startup
- Four status levels:
  | Status   | Meaning                                   |
  |----------|-------------------------------------------|
  | EXPIRED  | Expiry date has already passed            |
  | CRITICAL | Expires within 3 days                     |
  | WARNING  | Expires within your chosen threshold      |
  | SAFE     | No immediate concern                      |
- Configurable warning threshold (default: 7 days)
- View all products sorted by nearest expiry date
- Update quantity, remove products, search by name or category
- Persistent storage in a CSV file (`inventory.csv`) - no database required
- Input validation for dates and numbers

## Tech Stack

- Java 8 or above
- Java standard library only (no external dependencies)

## Getting Started

### Prerequisites
- JDK 8+ installed (`java -version` to check)

### Compile
```bash
javac SmartInventoryExpiryAlert.java
```

### Run
```bash
java SmartInventoryExpiryAlert
```

## Usage

```
=========================================
   SMART INVENTORY EXPIRY ALERT SYSTEM
=========================================

---------------- MENU ----------------
1. Add product
2. View all products (sorted by expiry)
3. View expiry alerts
4. Update product quantity
5. Remove product
6. Search product / category
7. Change warning threshold (current: 7 days)
0. Exit
--------------------------------------
```

### Sample alert output

```
== EXPIRY ALERTS (threshold: 7 days) ==
ID   NAME                 CATEGORY       QTY   EXPIRY       DAYS LEFT  STATUS
----------------------------------------------------------------------------------
2    Fresh Milk           Dairy          20    2026-10-12   -1         EXPIRED
1    Paracetamol          Medicine       50    2026-10-15   2          CRITICAL
3    Bread                Bakery         15    2026-10-19   6          WARNING
Summary: 3 alert(s), 1 already expired.
```

## Project Structure

```
.
├── SmartInventoryExpiryAlert.java   # Complete application source
├── README.md                        # Project documentation
└── inventory.csv                    # Auto-generated data file (created on first save)
```

## Data Format

Records are stored one per line in `inventory.csv`:

```
id,name,category,quantity,expiryDate
1,Paracetamol,Medicine,50,2026-10-15
```

## How It Works

1. On launch, the app loads saved products from `inventory.csv`.
2. For each product it calculates the days left until expiry.
3. Products are classified as EXPIRED / CRITICAL / WARNING / SAFE.
4. Any non-safe items are shown immediately as alerts.
5. Every change (add, update, remove) is saved automatically.

## Future Improvements

- Email / SMS / desktop notifications
- Barcode scanning support
- GUI using JavaFX or Swing
- Database integration (MySQL / SQLite)
- Batch-wise tracking and reports export (PDF / Excel)

## License

This project is open source and available under the [MIT License](https://opensource.org/licenses/MIT).

