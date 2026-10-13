import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Smart Inventory Expiry Alert System
 * -----------------------------------
 * A console-based application that tracks products, their quantities and
 * expiry dates, and raises alerts for items that are expired or about to expire.
 *
 * Data is stored in a simple CSV file (inventory.csv) so it persists between runs.
 * Requires Java 8 or above. No external libraries needed.
 */
public class SmartInventoryExpiryAlert {

    // ---------------------------------------------------------------
    // Alert levels
    // ---------------------------------------------------------------
    enum AlertLevel {
        EXPIRED("EXPIRED"),
        CRITICAL("CRITICAL"),   // expires within 3 days
        WARNING("WARNING"),     // expires within the user-defined threshold
        SAFE("SAFE");

        private final String label;

        AlertLevel(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    // ---------------------------------------------------------------
    // Product model
    // ---------------------------------------------------------------
    static class Product {
        private final int id;
        private String name;
        private String category;
        private int quantity;
        private LocalDate expiryDate;

        Product(int id, String name, String category, int quantity, LocalDate expiryDate) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.quantity = quantity;
            this.expiryDate = expiryDate;
        }

        int getId() { return id; }
        String getName() { return name; }
        String getCategory() { return category; }
        int getQuantity() { return quantity; }
        LocalDate getExpiryDate() { return expiryDate; }

        void setQuantity(int quantity) { this.quantity = quantity; }

        long daysLeft() {
            return ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
        }

        AlertLevel getAlertLevel(int warningDays) {
            long days = daysLeft();
            if (days < 0) return AlertLevel.EXPIRED;
            if (days <= 3) return AlertLevel.CRITICAL;
            if (days <= warningDays) return AlertLevel.WARNING;
            return AlertLevel.SAFE;
        }

        String toCsv() {
            return id + "," + clean(name) + "," + clean(category) + "," + quantity + "," + expiryDate;
        }

        private static String clean(String s) {
            return s.replace(",", " ").trim();
        }

        static Product fromCsv(String line) {
            String[] p = line.split(",");
            if (p.length != 5) {
                throw new IllegalArgumentException("Invalid record: " + line);
            }
            return new Product(
                    Integer.parseInt(p[0].trim()),
                    p[1].trim(),
                    p[2].trim(),
                    Integer.parseInt(p[3].trim()),
                    LocalDate.parse(p[4].trim()));
        }
    }

    // ---------------------------------------------------------------
    // Inventory manager (business logic + persistence)
    // ---------------------------------------------------------------
    static class InventoryManager {
        private final List<Product> products = new ArrayList<>();
        private final Path storagePath;
        private int nextId = 1;
        private int warningDays = 7;

        InventoryManager(String fileName) {
            this.storagePath = Paths.get(fileName);
            load();
        }

        int getWarningDays() { return warningDays; }

        void setWarningDays(int days) { this.warningDays = days; }

        Product addProduct(String name, String category, int quantity, LocalDate expiry) {
            Product p = new Product(nextId++, name, category, quantity, expiry);
            products.add(p);
            save();
            return p;
        }

        boolean removeProduct(int id) {
            boolean removed = products.removeIf(p -> p.getId() == id);
            if (removed) save();
            return removed;
        }

        boolean updateQuantity(int id, int newQty) {
            for (Product p : products) {
                if (p.getId() == id) {
                    p.setQuantity(newQty);
                    save();
                    return true;
                }
            }
            return false;
        }

        List<Product> getAllSortedByExpiry() {
            return products.stream()
                    .sorted(Comparator.comparing(Product::getExpiryDate))
                    .collect(Collectors.toList());
        }

        List<Product> getAlerts() {
            return getAllSortedByExpiry().stream()
                    .filter(p -> p.getAlertLevel(warningDays) != AlertLevel.SAFE)
                    .collect(Collectors.toList());
        }

        List<Product> search(String keyword) {
            String k = keyword.toLowerCase();
            return getAllSortedByExpiry().stream()
                    .filter(p -> p.getName().toLowerCase().contains(k)
                            || p.getCategory().toLowerCase().contains(k))
                    .collect(Collectors.toList());
        }

        int count() { return products.size(); }

        // ---------- persistence ----------
        private void load() {
            if (!Files.exists(storagePath)) return;
            try {
                List<String> lines = Files.readAllLines(storagePath, StandardCharsets.UTF_8);
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    try {
                        Product p = Product.fromCsv(line);
                        products.add(p);
                        nextId = Math.max(nextId, p.getId() + 1);
                    } catch (RuntimeException ex) {
                        System.out.println("Skipping bad record -> " + ex.getMessage());
                    }
                }
            } catch (IOException e) {
                System.out.println("Could not read data file: " + e.getMessage());
            }
        }

        private void save() {
            List<String> lines = products.stream().map(Product::toCsv).collect(Collectors.toList());
            try {
                Files.write(storagePath, lines, StandardCharsets.UTF_8);
            } catch (IOException e) {
                System.out.println("Could not save data: " + e.getMessage());
            }
        }
    }

    // ---------------------------------------------------------------
    // Console UI
    // ---------------------------------------------------------------
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        InventoryManager manager = new InventoryManager("inventory.csv");

        System.out.println("=========================================");
        System.out.println("   SMART INVENTORY EXPIRY ALERT SYSTEM   ");
        System.out.println("=========================================");

        // Show alerts automatically on startup
        showAlerts(manager);

        boolean running = true;
        while (running) {
            printMenu(manager);
            int choice = readInt("Enter choice: ");
            switch (choice) {
                case 1: addProduct(manager); break;
                case 2: listProducts(manager.getAllSortedByExpiry(), manager, "ALL PRODUCTS"); break;
                case 3: showAlerts(manager); break;
                case 4: updateQuantity(manager); break;
                case 5: removeProduct(manager); break;
                case 6: searchProducts(manager); break;
                case 7: changeThreshold(manager); break;
                case 0:
                    running = false;
                    System.out.println("Goodbye! Data saved to inventory.csv");
                    break;
                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private static void printMenu(InventoryManager m) {
        System.out.println("\n---------------- MENU ----------------");
        System.out.println("1. Add product");
        System.out.println("2. View all products (sorted by expiry)");
        System.out.println("3. View expiry alerts");
        System.out.println("4. Update product quantity");
        System.out.println("5. Remove product");
        System.out.println("6. Search product / category");
        System.out.println("7. Change warning threshold (current: " + m.getWarningDays() + " days)");
        System.out.println("0. Exit");
        System.out.println("--------------------------------------");
    }

    private static void addProduct(InventoryManager m) {
        System.out.println("\n-- Add New Product --");
        String name = readNonEmpty("Product name: ");
        String category = readNonEmpty("Category (e.g. Dairy, Medicine): ");
        int qty = readPositiveInt("Quantity: ");
        LocalDate expiry = readDate("Expiry date (YYYY-MM-DD): ");

        Product p = m.addProduct(name, category, qty, expiry);
        System.out.println("Added: " + p.getName() + " (ID " + p.getId() + ")");
        if (p.getAlertLevel(m.getWarningDays()) != AlertLevel.SAFE) {
            System.out.println("Note: this product is already flagged as "
                    + p.getAlertLevel(m.getWarningDays()).getLabel() + "!");
        }
    }

    private static void showAlerts(InventoryManager m) {
        List<Product> alerts = m.getAlerts();
        if (alerts.isEmpty()) {
            System.out.println("\nNo expiry alerts. All products are safe.");
            return;
        }
        listProducts(alerts, m, "EXPIRY ALERTS (threshold: " + m.getWarningDays() + " days)");
        long expired = alerts.stream().filter(p -> p.getAlertLevel(m.getWarningDays()) == AlertLevel.EXPIRED).count();
        System.out.println("Summary: " + alerts.size() + " alert(s), " + expired + " already expired.");
    }

    private static void listProducts(List<Product> list, InventoryManager m, String title) {
        System.out.println("\n== " + title + " ==");
        if (list.isEmpty()) {
            System.out.println("No products found.");
            return;
        }
        String fmt = "%-4s %-20s %-14s %-5s %-12s %-10s %-9s%n";
        System.out.printf(fmt, "ID", "NAME", "CATEGORY", "QTY", "EXPIRY", "DAYS LEFT", "STATUS");
        System.out.println(new String(new char[82]).replace('\0', '-'));
        for (Product p : list) {
            System.out.printf(fmt,
                    p.getId(),
                    trim(p.getName(), 20),
                    trim(p.getCategory(), 14),
                    p.getQuantity(),
                    p.getExpiryDate(),
                    p.daysLeft(),
                    p.getAlertLevel(m.getWarningDays()).getLabel());
        }
    }

    private static void updateQuantity(InventoryManager m) {
        int id = readInt("Product ID to update: ");
        int qty = readPositiveInt("New quantity: ");
        System.out.println(m.updateQuantity(id, qty) ? "Quantity updated." : "Product ID not found.");
    }

    private static void removeProduct(InventoryManager m) {
        int id = readInt("Product ID to remove: ");
        System.out.println(m.removeProduct(id) ? "Product removed." : "Product ID not found.");
    }

    private static void searchProducts(InventoryManager m) {
        String keyword = readNonEmpty("Search keyword: ");
        listProducts(m.search(keyword), m, "SEARCH RESULTS");
    }

    private static void changeThreshold(InventoryManager m) {
        int days = readPositiveInt("Warn me this many days before expiry: ");
        m.setWarningDays(days);
        System.out.println("Warning threshold set to " + days + " days.");
    }

    // ---------------------------------------------------------------
    // Input helpers
    // ---------------------------------------------------------------
    private static String trim(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "~";
    }

    private static String readNonEmpty(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = scanner.nextLine().trim();
            if (!s.isEmpty()) return s;
            System.out.println("Input cannot be empty.");
        }
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static int readPositiveInt(String prompt) {
        while (true) {
            int n = readInt(prompt);
            if (n >= 0) return n;
            System.out.println("Value cannot be negative.");
        }
    }

    private static LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return LocalDate.parse(scanner.nextLine().trim());
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date. Use format YYYY-MM-DD (e.g. 2026-12-31).");
            }
        }
    }
}
