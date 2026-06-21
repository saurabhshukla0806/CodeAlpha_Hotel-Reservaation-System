import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

// ─────────────────────────────────────────────
// ROOM MODEL
// Represents a single hotel room
// ─────────────────────────────────────────────
class Room {
    int roomNumber;
    String type;       // Standard, Deluxe, Suite
    double pricePerNight;
    boolean isAvailable;

    Room(int roomNumber, String type, double pricePerNight) {
        this.roomNumber    = roomNumber;
        this.type          = type;
        this.pricePerNight = pricePerNight;
        this.isAvailable   = true;
    }

    // Returns emoji icon based on room type — used in the UI
    String getIcon() {
        switch (type) {
            case "Suite":   return "🏨";
            case "Deluxe":  return "⭐";
            default:        return "🛏";
        }
    }

    @Override
    public String toString() {
        return roomNumber + "|" + type + "|" + pricePerNight + "|" + isAvailable;
    }
}

// ─────────────────────────────────────────────
// BOOKING MODEL
// Represents one reservation
// ─────────────────────────────────────────────
class Booking {
    static int counter = 1000; // auto-increment booking ID
    int bookingId;
    String customerName;
    int roomNumber;
    String checkIn, checkOut; // stored as strings "DD/MM/YYYY"
    boolean isPaid;
    double totalAmount;

    Booking(String customerName, int roomNumber, String checkIn, String checkOut, double totalAmount) {
        this.bookingId    = ++counter;
        this.customerName = customerName;
        this.roomNumber   = roomNumber;
        this.checkIn      = checkIn;
        this.checkOut     = checkOut;
        this.totalAmount  = totalAmount;
        this.isPaid       = false; // payment happens separately
    }

    // Constructor for loading from file (ID already known)
    Booking(int id, String customerName, int roomNumber, String checkIn, String checkOut, double totalAmount, boolean isPaid) {
        this.bookingId    = id;
        this.customerName = customerName;
        this.roomNumber   = roomNumber;
        this.checkIn      = checkIn;
        this.checkOut     = checkOut;
        this.totalAmount  = totalAmount;
        this.isPaid       = isPaid;
        if (id > counter) counter = id; // keep counter ahead of loaded IDs
    }

    // Serialize to one line in the file — fields separated by "|"
    @Override
    public String toString() {
        return bookingId + "|" + customerName + "|" + roomNumber + "|"
             + checkIn + "|" + checkOut + "|" + totalAmount + "|" + isPaid;
    }
}

// ─────────────────────────────────────────────
// HOTEL MANAGER
// All business logic + file I/O lives here
// This follows the principle of separating data/logic from the UI
// ─────────────────────────────────────────────
class HotelManager {
    private ArrayList<Room>    rooms    = new ArrayList<>();
    private ArrayList<Booking> bookings = new ArrayList<>();
    private static final String FILE = "bookings.txt";

    HotelManager() {
        initRooms();
        loadFromFile(); // restore previous bookings on startup
    }

    // Pre-populate 12 rooms across 3 categories
    void initRooms() {
        String[] types = {"Standard", "Deluxe", "Suite"};
        double[] prices = {1500, 3000, 6000};
        int[][] ranges  = {{101,104}, {201,204}, {301,304}};

        for (int t = 0; t < 3; t++) {
            for (int n = ranges[t][0]; n <= ranges[t][1]; n++) {
                rooms.add(new Room(n, types[t], prices[t]));
            }
        }
    }

    // Returns only rooms that match the type filter AND are available
    ArrayList<Room> getAvailableRooms(String type) {
        ArrayList<Room> result = new ArrayList<>();
        for (Room r : rooms) {
            if (r.isAvailable && (type.equals("All") || r.type.equals(type)))
                result.add(r);
        }
        return result;
    }

    ArrayList<Room> getAllRooms() { return rooms; }
    ArrayList<Booking> getAllBookings() { return bookings; }

    Room findRoom(int roomNumber) {
        for (Room r : rooms) if (r.roomNumber == roomNumber) return r;
        return null;
    }

    Booking findBooking(int bookingId) {
        for (Booking b : bookings) if (b.bookingId == bookingId) return b;
        return null;
    }

    // Calculate number of nights between two date strings
    // Using simple parsing — no external library needed
    int calcNights(String checkIn, String checkOut) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            long diff = sdf.parse(checkOut).getTime() - sdf.parse(checkIn).getTime();
            return (int)(diff / (1000 * 60 * 60 * 24)); // convert ms to days
        } catch (Exception e) {
            return 1;
        }
    }

    // BOOK: creates booking, marks room unavailable, saves to file
    String makeBooking(String name, int roomNum, String checkIn, String checkOut) {
        Room room = findRoom(roomNum);
        if (room == null)          return "Room not found.";
        if (!room.isAvailable)     return "Room is not available.";
        if (name.trim().isEmpty()) return "Enter customer name.";

        int nights = calcNights(checkIn, checkOut);
        if (nights <= 0) return "Check-out must be after check-in.";

        double total = room.pricePerNight * nights;
        Booking b = new Booking(name, roomNum, checkIn, checkOut, total);
        bookings.add(b);
        room.isAvailable = false;
        saveToFile();
        return "SUCCESS|" + b.bookingId + "|" + total;
    }

    // CANCEL: removes booking, frees the room
    String cancelBooking(int bookingId) {
        Booking b = findBooking(bookingId);
        if (b == null) return "Booking not found.";
        Room room = findRoom(b.roomNumber);
        if (room != null) room.isAvailable = true;
        bookings.remove(b);
        saveToFile();
        return "Booking #" + bookingId + " cancelled.";
    }

    // PAYMENT: marks booking as paid
    String payBooking(int bookingId) {
        Booking b = findBooking(bookingId);
        if (b == null)   return "Booking not found.";
        if (b.isPaid)    return "Already paid.";
        b.isPaid = true;
        saveToFile();
        return "Payment of ₹" + String.format("%.0f", b.totalAmount) + " confirmed!";
    }

    // FILE I/O — save all bookings to a plain text file
    // Format per line: bookingId|name|room|checkIn|checkOut|total|isPaid
    void saveToFile() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE))) {
            for (Booking b : bookings) pw.println(b.toString());
        } catch (IOException e) {
            System.out.println("Save error: " + e.getMessage());
        }
    }

    // FILE I/O — load bookings on startup, restore room availability
    void loadFromFile() {
        File f = new File(FILE);
        if (!f.exists()) return; // first run, no file yet

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|"); // split by "|"
                if (p.length < 7) continue;
                int id       = Integer.parseInt(p[0]);
                String name  = p[1];
                int roomNum  = Integer.parseInt(p[2]);
                String ci    = p[3], co = p[4];
                double total = Double.parseDouble(p[5]);
                boolean paid = Boolean.parseBoolean(p[6]);

                bookings.add(new Booking(id, name, roomNum, ci, co, total, paid));

                // Mark room as booked
                Room room = findRoom(roomNum);
                if (room != null) room.isAvailable = false;
            }
        } catch (IOException e) {
            System.out.println("Load error: " + e.getMessage());
        }
    }
}

// ─────────────────────────────────────────────
// ROUNDED PANEL — same as Grade Tracker
// ─────────────────────────────────────────────
class HRoundedPanel extends JPanel {
    private int radius;
    private Color bg;

    HRoundedPanel(int radius, Color bg) {
        this.radius = radius;
        this.bg = bg;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        g2.dispose();
        super.paintComponent(g);
    }
}

// ─────────────────────────────────────────────
// MAIN GUI
// ─────────────────────────────────────────────
public class HotelReservationSystem extends JFrame {

    // Color palette
    static final Color BG        = new Color(236, 240, 247);
    static final Color SIDEBAR   = new Color(25,  35,  60);
    static final Color ACCENT    = new Color(99,  102, 241);
    static final Color GREEN     = new Color(39,  174,  96);
    static final Color RED       = new Color(220,  80,  80);
    static final Color GOLD      = new Color(243, 156,  18);
    static final Color CARD_BG   = Color.WHITE;
    static final Color TEXT_MAIN = new Color(30,  40,  60);
    static final Color TEXT_MUTE = new Color(130, 140, 160);

    private HotelManager manager = new HotelManager();

    // Tabs
    private JPanel contentArea;
    private CardLayout cardLayout;

    // Rooms tab
    private JComboBox<String> filterCombo;
    private DefaultTableModel roomsModel;

    // Booking tab
    private JTextField bookNameField, bookRoomField, checkInField, checkOutField;

    // Manage tab
    private DefaultTableModel bookingsModel;
    private JTextField actionIdField;

    public HotelReservationSystem() {
        setTitle("Hotel Reservation System — CodeAlpha");
        setSize(1000, 660);
        setMinimumSize(new Dimension(860, 560));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildSidebar(), BorderLayout.WEST);

        cardLayout  = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setBackground(BG);
        contentArea.add(buildRoomsPanel(),    "ROOMS");
        contentArea.add(buildBookingPanel(),  "BOOK");
        contentArea.add(buildManagePanel(),   "MANAGE");

        add(contentArea, BorderLayout.CENTER);
        cardLayout.show(contentArea, "ROOMS");

        setVisible(true);
    }

    // ── SIDEBAR ──────────────────────────────────
    private JPanel buildSidebar() {
        JPanel s = new JPanel();
        s.setBackground(SIDEBAR);
        s.setPreferredSize(new Dimension(200, 0));
        s.setLayout(new BoxLayout(s, BoxLayout.Y_AXIS));
        s.setBorder(new EmptyBorder(30, 16, 30, 16));

        JLabel icon  = label("🏩", 32, Color.WHITE, Font.PLAIN);
        JLabel title = label("Grand Vista", 18, Color.WHITE, Font.BOLD);
        JLabel sub   = label("Hotel & Suites", 11, TEXT_MUTE, Font.PLAIN);
        icon.setAlignmentX(0); title.setAlignmentX(0); sub.setAlignmentX(0);

        s.add(icon);
        s.add(Box.createVerticalStrut(6));
        s.add(title);
        s.add(sub);
        s.add(Box.createVerticalStrut(30));
        s.add(separator());
        s.add(Box.createVerticalStrut(20));

        // Nav buttons — each switches the CardLayout panel
        s.add(navButton("🛏  Rooms",           "ROOMS"));
        s.add(Box.createVerticalStrut(8));
        s.add(navButton("📋  Make Booking",    "BOOK"));
        s.add(Box.createVerticalStrut(8));
        s.add(navButton("📁  Manage Bookings", "MANAGE"));
        s.add(Box.createVerticalGlue());

        JLabel ver = label("v1.0 • CodeAlpha", 10, new Color(70, 85, 120), Font.PLAIN);
        ver.setAlignmentX(0);
        s.add(ver);

        return s;
    }

    // ── ROOMS PANEL ──────────────────────────────
    private JPanel buildRoomsPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(24, 24, 24, 24));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG);
        header.setBorder(new EmptyBorder(0, 0, 16, 0));

        JLabel heading = label("Available Rooms", 22, TEXT_MAIN, Font.BOLD);

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterRow.setBackground(BG);
        filterCombo = new JComboBox<>(new String[]{"All", "Standard", "Deluxe", "Suite"});
        filterCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        filterCombo.addActionListener(e -> refreshRooms());

        JButton refreshBtn = accentBtn("Refresh", ACCENT);
        refreshBtn.addActionListener(e -> refreshRooms());

        filterRow.add(new JLabel("Filter: "));
        filterRow.add(filterCombo);
        filterRow.add(refreshBtn);

        header.add(heading, BorderLayout.WEST);
        header.add(filterRow, BorderLayout.EAST);

        // Table
        String[] cols = {"Room No.", "Type", "Price/Night", "Status"};
        roomsModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = styledTable(roomsModel);

        // Color-code the Status column
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBorder(new EmptyBorder(0, 14, 0, 14));
                if (!sel) {
                    setBackground(row % 2 == 0 ? CARD_BG : new Color(248, 250, 255));
                    if (col == 3) {
                        boolean avail = "Available".equals(v);
                        setForeground(avail ? GREEN : RED);
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        setForeground(TEXT_MAIN);
                        setFont(new Font("Segoe UI", Font.PLAIN, 13));
                    }
                }
                return this;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        HRoundedPanel card = new HRoundedPanel(14, CARD_BG);
        card.setLayout(new BorderLayout());
        card.add(table.getTableHeader(), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        p.add(header, BorderLayout.NORTH);
        p.add(card, BorderLayout.CENTER);

        refreshRooms();
        return p;
    }

    // ── BOOKING PANEL ─────────────────────────────
    private JPanel buildBookingPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel heading = label("Make a Reservation", 22, TEXT_MAIN, Font.BOLD);
        heading.setBorder(new EmptyBorder(0, 0, 20, 0));
        p.add(heading, BorderLayout.NORTH);

        // Form card
        HRoundedPanel card = new HRoundedPanel(14, CARD_BG);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(30, 36, 30, 36));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets  = new Insets(10, 10, 10, 10);
        gc.fill    = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;

        bookNameField  = formField("e.g. Rahul Sharma");
        bookRoomField  = formField("e.g. 201");
        checkInField   = formField("DD/MM/YYYY");
        checkOutField  = formField("DD/MM/YYYY");

        // Row 0 — customer name
        addFormRow(card, gc, 0, "👤  Customer Name", bookNameField);
        // Row 1 — room number
        addFormRow(card, gc, 1, "🚪  Room Number",   bookRoomField);
        // Row 2 — check in
        addFormRow(card, gc, 2, "📅  Check-In Date", checkInField);
        // Row 3 — check out
        addFormRow(card, gc, 3, "📅  Check-Out Date", checkOutField);

        // Book button
        gc.gridy  = 4; gc.gridx = 0; gc.gridwidth = 2;
        gc.insets = new Insets(20, 10, 10, 10);
        JButton bookBtn = accentBtn("Confirm Booking", GREEN);
        bookBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        bookBtn.setPreferredSize(new Dimension(0, 46));
        bookBtn.addActionListener(e -> doBooking());
        card.add(bookBtn, gc);

        // Info label
        gc.gridy = 5;
        gc.insets = new Insets(4, 10, 0, 10);
        JLabel infoLbl = label("Tip: Check 'Rooms' tab for available room numbers.", 11, TEXT_MUTE, Font.PLAIN);
        card.add(infoLbl, gc);

        // Wrap card so it doesn't stretch
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setBackground(BG);
        GridBagConstraints wc = new GridBagConstraints();
        wc.fill = GridBagConstraints.BOTH;
        wc.weightx = 0.6; wc.weighty = 1;
        wrap.add(card, wc);
        wc.weightx = 0.4;
        JPanel spacer = new JPanel(); spacer.setOpaque(false);
        wrap.add(spacer, wc);

        p.add(wrap, BorderLayout.CENTER);
        return p;
    }

    // ── MANAGE PANEL ──────────────────────────────
    private JPanel buildManagePanel() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel heading = label("Manage Bookings", 22, TEXT_MAIN, Font.BOLD);
        p.add(heading, BorderLayout.NORTH);

        // Actions bar
        HRoundedPanel actBar = new HRoundedPanel(12, CARD_BG);
        actBar.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 12));

        actionIdField = new JTextField(10);
        actionIdField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        actionIdField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 215, 230)),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        JButton cancelBtn = accentBtn("❌  Cancel Booking", RED);
        cancelBtn.addActionListener(e -> {
            String idStr = actionIdField.getText().trim();
            try {
                String result = manager.cancelBooking(Integer.parseInt(idStr));
                JOptionPane.showMessageDialog(this, result);
                refreshBookings();
                refreshRooms();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter a valid Booking ID.");
            }
        });

        JButton payBtn = accentBtn("💳  Pay Now", ACCENT);
        payBtn.addActionListener(e -> {
            String idStr = actionIdField.getText().trim();
            try {
                String result = manager.payBooking(Integer.parseInt(idStr));
                JOptionPane.showMessageDialog(this, result);
                refreshBookings();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter a valid Booking ID.");
            }
        });

        JButton refreshBtn = accentBtn("🔄  Refresh", GOLD);
        refreshBtn.addActionListener(e -> refreshBookings());

        actBar.add(label("Booking ID:", 13, TEXT_MAIN, Font.BOLD));
        actBar.add(actionIdField);
        actBar.add(cancelBtn);
        actBar.add(payBtn);
        actBar.add(refreshBtn);

        // Bookings table
        String[] cols = {"ID", "Customer", "Room", "Check-In", "Check-Out", "Total (₹)", "Payment"};
        bookingsModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = styledTable(bookingsModel);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBorder(new EmptyBorder(0, 14, 0, 14));
                if (!sel) {
                    setBackground(row % 2 == 0 ? CARD_BG : new Color(248, 250, 255));
                    if (col == 6) {
                        boolean paid = "✅ Paid".equals(v);
                        setForeground(paid ? GREEN : GOLD);
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        setForeground(TEXT_MAIN);
                        setFont(new Font("Segoe UI", Font.PLAIN, 13));
                    }
                }
                return this;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        HRoundedPanel tableCard = new HRoundedPanel(14, CARD_BG);
        tableCard.setLayout(new BorderLayout());
        tableCard.add(table.getTableHeader(), BorderLayout.NORTH);
        tableCard.add(scroll, BorderLayout.CENTER);

        p.add(actBar, BorderLayout.CENTER);
        p.add(tableCard, BorderLayout.SOUTH);

        // Give table more vertical space
        tableCard.setPreferredSize(new Dimension(0, 380));

        refreshBookings();
        return p;
    }

    // ─────────────────────────────────────────────
    // LOGIC METHODS
    // ─────────────────────────────────────────────

    void doBooking() {
        String name    = bookNameField.getText().trim();
        String roomStr = bookRoomField.getText().trim();
        String ci      = checkInField.getText().trim();
        String co      = checkOutField.getText().trim();

        int roomNum;
        try {
            roomNum = Integer.parseInt(roomStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Enter a valid room number.");
            return;
        }

        String result = manager.makeBooking(name, roomNum, ci, co);

        if (result.startsWith("SUCCESS")) {
            // Parse success response: "SUCCESS|bookingId|total"
            String[] parts = result.split("\\|");
            int id     = Integer.parseInt(parts[1]);
            double amt = Double.parseDouble(parts[2]);

            JOptionPane.showMessageDialog(this,
                "✅ Booking confirmed!\n\nBooking ID: #" + id
                + "\nTotal: ₹" + String.format("%.0f", amt)
                + "\n\nGo to 'Manage Bookings' to pay.",
                "Booking Confirmed", JOptionPane.INFORMATION_MESSAGE);

            // Clear form
            bookNameField.setText(""); bookRoomField.setText("");
            checkInField.setText("DD/MM/YYYY"); checkOutField.setText("DD/MM/YYYY");
            refreshRooms();
            refreshBookings();
        } else {
            JOptionPane.showMessageDialog(this, result, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    void refreshRooms() {
        roomsModel.setRowCount(0);
        String filter = filterCombo != null ? (String) filterCombo.getSelectedItem() : "All";
        for (Room r : manager.getAllRooms()) {
            if (filter.equals("All") || r.type.equals(filter)) {
                roomsModel.addRow(new Object[]{
                    r.roomNumber,
                    r.getIcon() + " " + r.type,
                    "₹" + String.format("%.0f", r.pricePerNight),
                    r.isAvailable ? "Available" : "Booked"
                });
            }
        }
    }

    void refreshBookings() {
        bookingsModel.setRowCount(0);
        for (Booking b : manager.getAllBookings()) {
            bookingsModel.addRow(new Object[]{
                "#" + b.bookingId,
                b.customerName,
                b.roomNumber,
                b.checkIn,
                b.checkOut,
                "₹" + String.format("%.0f", b.totalAmount),
                b.isPaid ? "✅ Paid" : "⏳ Pending"
            });
        }
    }

    // ─────────────────────────────────────────────
    // UI HELPERS
    // ─────────────────────────────────────────────

    JLabel label(String text, int size, Color color, int style) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", style, size));
        l.setForeground(color);
        return l;
    }

    JButton accentBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(8, 16, 8, 16));
        return b;
    }

    JButton navButton(String text, String card) {
        JButton b = new JButton(text);
        b.setBackground(new Color(40, 55, 90));
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(new EmptyBorder(8, 14, 8, 14));
        b.addActionListener(e -> cardLayout.show(contentArea, card));
        return b;
    }

    JTextField formField(String placeholder) {
        JTextField f = new JTextField(placeholder);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setForeground(TEXT_MUTE);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 215, 235)),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (f.getText().equals(placeholder)) { f.setText(""); f.setForeground(TEXT_MAIN); }
            }
            public void focusLost(FocusEvent e) {
                if (f.getText().isEmpty()) { f.setText(placeholder); f.setForeground(TEXT_MUTE); }
            }
        });
        return f;
    }

    void addFormRow(JPanel p, GridBagConstraints gc, int row, String labelText, JTextField field) {
        gc.gridy = row; gc.gridx = 0; gc.gridwidth = 1; gc.weightx = 0.3;
        gc.insets = new Insets(10, 10, 10, 10);
        JLabel lbl = label(labelText, 13, TEXT_MAIN, Font.BOLD);
        p.add(lbl, gc);
        gc.gridx = 1; gc.weightx = 0.7;
        p.add(field, gc);
    }

    JTable styledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(38);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(230, 235, 245));
        table.setSelectionBackground(new Color(220, 225, 255));
        table.setBackground(CARD_BG);
        table.setForeground(TEXT_MAIN);
        table.setIntercellSpacing(new Dimension(0, 1));

        JTableHeader h = table.getTableHeader();
        h.setFont(new Font("Segoe UI", Font.BOLD, 12));
        h.setBackground(new Color(245, 247, 252));
        h.setForeground(TEXT_MUTE);
        h.setPreferredSize(new Dimension(0, 40));
        h.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(220, 225, 240)));
        return table;
    }

    JSeparator separator() {
        JSeparator s = new JSeparator();
        s.setForeground(new Color(50, 65, 100));
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return s;
    }

    // ─────────────────────────────────────────────
    // ENTRY POINT
    // ─────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(HotelReservationSystem::new);
    }
}
