# Shop Inventory Scanner App - Implementation Plan

## 1. OBJECTIVE
A deployable, professional Android mobile application ("SmartShopV1.0.0") that uses ML Kit and CameraX to scan barcodes/QR codes in shop inventory, manage chart/checkout with UGX currency, provide an authenticated admin mode for inventory management with encrypted backup/restore, and low stock device notifications.

## 2. CONTEXT SUMMARY
- **Type**: Native Android application (Kotlin)
- **Language**: Kotlin with Jetpack Compose UI
- **Architecture**: MVVM + Clean Architecture
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- **Key Dependencies**: 
  - ML Kit Barcode Scanning (all formats)
  - CameraX for camera
  - Room DB for local storage
  - Jetpack Compose for UI
  - MPAndroidChart for graphs
  - Android TTS (built-in TextToSpeech API)
  - Android Vibrator (haptic feedback)
  - SoundPool (beep sound)
  - iText PDF (for PDF export)

## 3. APPROACH OVERVIEW + ARCHITECTURE
The app follows a two-mode design:
1. **Scanner Mode (Default)**: Camera-based barcode/QR scanning → Cart → Checkout with receipt
2. **Manager Dashboard**: 6-digit PIN-authenticated screen for inventory CRUD, analytics, restocking

### COLOR PALETTE (Brown + Blue):
| Element | Color | Hex |
|---------|-------|-----|
| Primary | Saddle Brown | #8B4513 |
| Primary Dark | Dark Brown | #5D3A1A |
| Secondary | Dodger Blue | #1E90FF |
| Secondary Dark | Deep Blue | #104E8B |
| Background | Wheat | #F5DEB3 |
| Surface | White | #FFFFFF |
| Text Primary | Dark Brown | #3E2723 |
| Text Secondary | Blue | #0D47A1 |

### SCREENS (7 screens):
1. **SetupScreen** (First launch) - Set 6-digit PIN
2. **ScannerScreen** (Default) - Camera with small scan area + 70% blur
3. **ImagePreviewScreen** (Modal overlay) - Shows scanned item **rounded** image for 2s
4. **CartScreen** - Items added + total in UGX
5. **CheckoutScreen** - Receipt + "Paid" button
6. **SettingsScreen** - Settings + inventory management access button
7. **ManagerDashboard** - CRUD items + Analytics charts + restock

### DATA LAYER (Clean Architecture):
- **Room Database** (`SmartShopDB`): Contains all data
  - `InventoryItem` table (id, barcode, name, quantity, category, price, imagePath, createdAt, updatedAt, salesCount)
  - `Transaction` table (id, itemsJson, total, timestamp)
- **Internal Storage Folders** (app-private):
  - `/data/files/backups/` - Encrypted backup files
  - `/data/files/exports/` - CSV and PDF exports
- **InventoryDao** - CRUD + analytics queries
- **TransactionDao** - Insert + SUM queries for sales stats
- **InventoryRepository** - Single source of truth for inventory
- **CartItem** (temp, cleared after checkout)
- **CartRepository** - Manages cart items (ViewModel + StateFlow)
- **BackupManager** - Encrypts/decrypts to `/data/files/backups/`
- **ExportManager** - Generates CSV/PDF to `/data/files/exports/`
- **AuthManager** - PIN verification
- **LowStockWorker** (WorkManager) - Periodic check → Notification

### ALL USER FLOWS:

**Flow 1: First Launch**
App Starts → Check SharedPreferences "pin_set" → false → SetupScreen → Enter PIN (6 digits) → Confirm → Save PIN → ScannerScreen

**Flow 2: Scan Item**
ScannerScreen active → Camera + ML Kit → Detect barcode → Lookup in InventoryRepository
→ Found → Show ImagePreview (2s) → Add to CartRepository → TTS "Item added" → Resume scanning
→ NOT Found → TTS "Item not found" → Keep scanning

**Flow 3: Checkout**
ScannerScreen → Click "Checkout" button → CartScreen → Shows items + total UGX
→ Click "Proceed" → CheckoutScreen → Show receipt → Receptionist clicks "Paid"
→ TransactionRepository.save(transaction) for each checkout
→ InventoryRepository.deductQty() for each item → Clear Cart → ScannerScreen

**Flow 4: Inventory Management Access**
ScannerScreen → Click Settings icon → PIN Dialog → Enter 6-digit PIN → Verify in AuthManager
→ Success → AdminDashboard (for inventory CRUD) | Failure → Error + retry

**Flow 5: Inventory Management CRUD**
AdminDashboard:
- Create Item → Fill form (name, category, price) + capture image → Compress to 80KB JPEG → InventoryRepository.insert()
- Edit Item → Modify fields → InventoryRepository.update()
- Delete Item → Confirm dialog → InventoryRepository.delete()
- Restock → Enter qty → InventoryRepository.updateQuantity()
- Analytics → Show charts: bar chart by category, top items, stock progress bars

**Flow 6: Data Loss Protection**
User goes Settings → App Info → Clear Data → **Device Policy** fires → Warning dialog
→ User confirms disable → Policy disabled → User can Clear Data → DB cleared
→ App reopens → Check backup file in `/files/backups/` exists? → Yes → Show "Restore?" dialog → Enter PIN → Restore

**Flow 7: Low Stock Notification (Background)**
WorkManager runs hourly → InventoryRepository.getLowStockItems(threshold=5)
→ For each item → NotificationManager.notify("Low stock: [item] only [X] left")

### KEY INTEGRATION POINTS:
- **ScannerScreen** → InventoryRepository.lookup() → CartRepository.add() → ImagePreview
- **CartScreen** → CartRepository.items.collect() → total = sum * price
- **CheckoutScreen** → InventoryRepository.deduct() after "Paid"
- **SettingsScreen** → AuthManager.verify() → ManagerDashboard
- **ManagerDashboard** → InventoryRepository full CRUD
- **LowStockWorker** → InventoryRepository → NotificationManager
- **DeviceAdminReceiver** (policy) → intercepts disable via device_admin.xml

This architecture ensures all components connect properly with no orphan flows.

## 4. IMPLEMENTATION STEPS

### Step 1: Project Setup & Dependencies
- Create Gradle wrapper and project structure
- Set up build.gradle with all dependencies
- Configure AndroidManifest.xml with camera/storage permissions
- **Create device_admin.xml** for Device Admin policy (used by Chrome/Play Services pattern)

### Step 2: Database Layer + Backup System (Room DB)
- Create Entity: `InventoryItem` (id, barcode, name, quantity, category, price, imagePath, createdAt, updatedAt, salesCount)
- Create Entity: `Transaction` (id, items JSON, total, timestamp) - records each checkout
- Create DAO: 
  - CRUD operations for InventoryItem
  - Queries for analytics: totalSales(), todaySales(), inventoryWorth(), topItems(), lowStock()
- Create Repository pattern
- Pre-populate with sample inventory items

### Step 3: Authentication System + Device Policy + Data Protection
- SharedPreferences for PIN storage (encrypted with EncryptedSharedPreferences)
- First-launch PIN setup screen
- PIN verification for inventory management access
- Session management
- **Device Policy Receiver**: Register in manifest to intercept disable attempts
  - Implement `DeviceAdminReceiver` with `getRemoveWarning()` to show warning message
  - When user tries to disable in Settings, Android shows warning dialog first
  - This forces user to confirm before "Clear Data" can work
  - Works same as Chrome/TikTok Lite/Play Services pattern
- **Backup system**: Auto backup to `/files/backups/` (encrypted) before policy disabled, restore after
- **Data restore screen**: If backup exists but DB is empty (after data cleared), prompt for PIN before restoring

### Step 4: Scanner Activity + TTS + Audio/Haptic Feedback
- CameraX preview integration
- **Small rectangular scan area** centered for barcode alignment
- **70% blur overlay** on non-scan area (dimmed background)
- **Torch button**: Toggle flash on/off (click to turn on, click to turn off)
- **TTS integration**:
  - On scan success: "Item added to cart"
  - Low light detected: "Environment is dark, consider enabling torch"
  - Before scan: "Align product in scannable area"
  - **Audio mode check**: Only trigger TTS if device audio mode is Speaker/Normal; skip if Vibration/Silent
- **Audio + Haptic Feedback**:
  - **On successful scan**: Play beep sound + Single short vibration
  - **On failed scan**: Double vibration (error feedback)
- On scan: lookup item → show **rounded image** (2s) → auto-resume scanning
- Bottom buttons: Scan More, Rescan, Checkout
- Shows product image when scanned (rounded corners, fits screen, held 2 seconds before auto-resume)

### Step 5: Cart & Checkout System
- Cart state management (ViewModel)
- Add scanned items to cart
- **Cart shows each item with**: Name, Quantity, Price, **Stock remaining** (from inventory)
- Running total in UGX
- Checkout screen with receipt
- "Paid" button → deducts inventory quantities

### Step 6: Admin Dashboard
- PIN-authenticated entry via settings button
- **Summary Stats** (at top of dashboard):
  - **Total Sales**: All-time revenue in UGX
  - **Today's Sales**: Revenue from today only
  - **Total Inventory Worth**: Sum of (quantity × price) for all items in UGX
- Item management: Create, Edit, Delete, Restock
- Price update functionality
- Image capture/selection with downscaling (80KB JPEG)
- **Export Options**:
  - Export Inventory to CSV → Saved to `/files/exports/inventory_YYYY-MM-DD.csv`
  - Export Sales Report to PDF → Saved to `/files/exports/sales_YYYY-MM-DD.pdf`
  - View exported files in Manager Dashboard
  - Date range selection for export

### Step 7: Analytics & Charts + Notifications
- **Summary Stats from Transaction table**:
  - Total Sales: SELECT SUM(total) FROM transactions
  - Today's Sales: SELECT SUM(total) FROM transactions WHERE DATE(timestamp) = today
  - Inventory Worth: SELECT SUM(quantity * price) FROM inventory_items
- Sales bar chart by category
- Top performing items list
- Stock level progress bars
- Date range filtering
- **Low stock notifications**: 
  - WorkManager periodic check (e.g., every hour)
  - When item quantity falls below threshold (e.g., ≤5), send device notification
  - Notification: "Low stock alert: [ItemName] only [X] left"
  - Uses WorkManager for reliability even when app closed

### Step 8: Polish & Build
- UI refinements
- Error handling
- **Config signing**: Release keystore (or debug for distribution)
- Build **SmartShopV1.0.0.apk** (Release build, not debug)

## 5. TESTING AND VALIDATION

### PRE-BUILD VERIFICATION (Manual checks before coding):
- [ ] Verify all 7 screens defined and navigation paths connect
- [ ] Verify InventoryItem entity has all required fields
- [ ] Verify CartItem links to InventoryItem via barcode
- [ ] Verify Checkout deducts from inventory (not creates orphan data)
- [ ] Verify DeviceAdminReceiver (policy) registered in manifest with device_admin.xml
- [ ] Verify WorkManager periodic task scheduled in Application.onCreate()
- [ ] Verify TTS checks AudioManager.getRingerMode() before speaking
- [ ] Verify Color palette applied consistently across all screens
- [ ] Verify image compression to 80KB max before saving

### RUNTIME TESTING:
- **Scanner**: 
  - Scan QR → Rounded image shows 2s → Cart updated
  - Small scan area (centered), 70% blur on background
  - Torch button toggles ON/OFF
  - TTS: Speaker mode → speaks | Vibration/Silent → quiet
  - **On successful scan**: Beep sound + Single vibration
  - **On failed scan**: Double vibration (error)
- **Cart**: Shows item + qty + price + **stock remaining**, total calculates correctly in UGX
- **Checkout**: Receipt shows, "Paid" deducts inventory (verify DB updated)
- **Manager**: PIN protects all CRUD, images compress to 80KB
  - Summary stats display correctly: Total Sales, Today's Sales, Inventory Worth
  - CSV export: Saves to `/files/exports/` folder
  - PDF export: Sales report saves to `/files/exports/` folder
- **Analytics**: Bar chart shows category sales, stock progress bars accurate
- **Data Protection**: 
  - Device Policy warning shows on disable attempt (Settings → App Info)
  - Backup restores correctly after DB clear
- **Notifications**: Low stock triggers device notification (when threshold ≤5)
- **APK**: Build completes as **SmartShopV1.0.0.apk** (Release)
