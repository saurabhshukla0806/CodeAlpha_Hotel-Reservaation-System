# CodeAlpha_Hotel-Reservaation-System

# 🏨 Hotel Reservation System

A Java Swing desktop application to search, book, and manage hotel room reservations, built as part of the **CodeAlpha Java Programming Internship**.

---

## 🖥️ Tech Stack

- **Language:** Java (JDK 8+)
- **UI Framework:** Java Swing
- **Data Storage:** File I/O (`bookings.txt`)

---

## 🏗️ Project Structure

```
HotelReservationSystem.java
├── class Room                    → Model for a hotel room
├── class Booking                 → Model for a reservation
├── class HotelManager            → All business logic + file I/O
├── class HRoundedPanel           → Custom JPanel with rounded corners
└── class HotelReservationSystem  → Main JFrame — all UI and navigation
```

### Class Breakdown

#### `Room`
Represents a single hotel room.

| Field | Description |
|---|---|
| `roomNumber` | Unique room identifier (e.g. 101, 201, 301) |
| `type` | Standard / Deluxe / Suite |
| `pricePerNight` | Nightly rate in ₹ |
| `isAvailable` | `true` if not currently booked |

Pre-loaded rooms:

| Range | Type | Price/Night |
|---|---|---|
| 101 – 104 | Standard | ₹1,500 |
| 201 – 204 | Deluxe   | ₹3,000 |
| 301 – 304 | Suite    | ₹6,000 |

#### `Booking`
Represents one reservation.

| Field | Description |
|---|---|
| `bookingId` | Auto-incremented from 1001 |
| `customerName` | Guest's name |
| `roomNumber` | Room assigned |
| `checkIn / checkOut` | Dates in `DD/MM/YYYY` format |
| `totalAmount` | `pricePerNight × nights` |
| `isPaid` | Payment status |

Two constructors:
- **New booking** — auto-generates `bookingId`
- **Load from file** — accepts existing `bookingId` to restore saved data

#### `HotelManager`
All business logic is separated here, completely independent of the UI.

| Method | Description |
|---|---|
| `makeBooking()` | Validates input, calculates nights, creates booking, marks room unavailable |
| `cancelBooking()` | Removes booking, frees the room |
| `payBooking()` | Sets `isPaid = true` |
| `calcNights()` | Uses `SimpleDateFormat` to find difference in days between two dates |
| `saveToFile()` | Writes all bookings to `bookings.txt` using `PrintWriter` |
| `loadFromFile()` | Reads `bookings.txt` on startup using `BufferedReader`, restores room availability |

**File format** (`bookings.txt`) — one line per booking, fields separated by `|`:
```
1001|Saurabh Shukla|201|01/07/2025|03/07/2025|6000.0|true
1002|Rahul Sharma|301|05/07/2025|07/07/2025|12000.0|false
```

#### `HotelReservationSystem` (Main JFrame)
Uses **`CardLayout`** to switch between 3 screens without opening new windows:

| Screen | Description |
|---|---|
| 🛏 Rooms | View all rooms, filter by type, check availability |
| 📋 Make Booking | Form to enter guest name, room, check-in/out dates |
| 📁 Manage Bookings | View all reservations, cancel or pay by Booking ID |

Key Swing concepts used:
- `CardLayout` — switches panels inside one container; nav buttons call `cardLayout.show()`
- `JTable` + `DefaultTableModel` — tables built from models, never edited directly
- `DefaultTableCellRenderer` — custom renderer for color-coded status columns
- `GridBagLayout` — used for the booking form to align labels and fields cleanly
- `SimpleDateFormat` — parses date strings to calculate number of nights
- `SwingUtilities.invokeLater` — ensures UI runs on the Event Dispatch Thread (EDT)

---

## ✨ Features

- 🛏 Browse **12 pre-loaded rooms** across Standard, Deluxe, and Suite categories
- 🔍 Filter rooms by type
- 📋 Book a room by entering guest name, room number, and dates
- 💰 Auto-calculates **total cost** based on nights × price
- 💳 Simulate **payment** per booking
- ❌ **Cancel** any reservation and free the room instantly
- 💾 **Persistent storage** — bookings saved to `bookings.txt` and restored on next launch
- 🎨 Color-coded status — green for Available/Paid, red for Booked, gold for Pending

---

## 🚀 How to Run

**Prerequisites:** Java JDK 8 or above installed.

```bash
# 1. Clone the repository
git clone https://github.com/saurabhshukla/CodeAlpha_HotelReservationSystem.git

# 2. Navigate into the folder
cd CodeAlpha_HotelReservationSystem

# 3. Compile
javac HotelReservationSystem.java

# 4. Run
java HotelReservationSystem
```

> A `bookings.txt` file will be auto-created in the same folder on your first booking.

---

## 📖 User Manual

### Viewing Rooms
1. Click **"🛏 Rooms"** in the sidebar
2. All 12 rooms are listed with type, price, and availability status
3. Use the **Filter dropdown** (top right) to show only Standard, Deluxe, or Suite rooms
4. Green = **Available**, Red = **Booked**

### Making a Booking
1. Click **"📋 Make Booking"** in the sidebar
2. Fill in the form:
   - **Customer Name** — guest's full name
   - **Room Number** — pick an available room from the Rooms tab (e.g. `201`)
   - **Check-In Date** — format: `DD/MM/YYYY`
   - **Check-Out Date** — format: `DD/MM/YYYY`
3. Click **"Confirm Booking"**
4. A dialog shows your **Booking ID** and **total amount**
5. The room status updates to **Booked** automatically

### Paying for a Booking
1. Click **"📁 Manage Bookings"** in the sidebar
2. Find your booking in the table and note the **Booking ID** (e.g. `#1001`)
3. Type the ID (without `#`) in the **Booking ID field**
4. Click **"💳 Pay Now"**
5. Status updates to ✅ **Paid**

### Cancelling a Booking
1. Click **"📁 Manage Bookings"** in the sidebar
2. Type the **Booking ID** in the field
3. Click **"❌ Cancel Booking"**
4. The booking is removed and the room becomes **Available** again

### Payment Status

| Status | Meaning |
|---|---|
| ⏳ Pending | Booking confirmed but not yet paid |
| ✅ Paid | Payment completed |

---

## 📸 Screenshots

<img width="1028" height="718" alt="Screenshot from 2026-06-21 22-35-42" src="https://github.com/user-attachments/assets/b32e7502-f121-4b84-a39b-0f455fc465ca" />
<img width="1028" height="718" alt="Screenshot from 2026-06-21 22-35-35" src="https://github.com/user-attachments/assets/0c272652-ab56-4797-b83c-ce059245a875" />
<img width="1028" height="718" alt="Screenshot from 2026-06-21 22-35-26" src="https://github.com/user-attachments/assets/cd4083ec-8857-4876-bd80-ac0a8662ab9a" />


---

## 👨‍💻 Author

**Saurabh Shukla**  
B.Tech CSE — Babu Sunder Singh Institute of Technology  
CodeAlpha Java Programming Intern
