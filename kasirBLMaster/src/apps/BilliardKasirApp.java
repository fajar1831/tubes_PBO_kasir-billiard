package apps; 

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PrinterException; // Import untuk pencetakan
import java.io.BufferedWriter; // Import untuk tulis file
import java.io.File; // Import untuk file
import java.io.FileWriter; // Import untuk tulis file
import java.io.IOException; // Import untuk exception file
import java.sql.*; // Import untuk JDBC
import java.text.NumberFormat;
// import java.text.ParseException; // Tidak terpakai, bisa dihapus
import java.time.Duration;
import java.time.LocalDate; 
import java.time.LocalDateTime; 
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.WeekFields; // Untuk minggu
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.Comparator;

// Kelas untuk Dialog Login (Sama seperti sebelumnya)
class LoginDialog extends JDialog {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JLabel lblStatus;
    private boolean loggedIn = false;

    private static final String VALID_USERNAME = "kasir";
    private static final char[] VALID_PASSWORD = {'1', '2', '3', '4', '5'};

    public LoginDialog(Frame parent) {
        super(parent, "Login Kasir", true);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblUsername = new JLabel("Username:");
        gbc.gridx = 0; gbc.gridy = 0; add(lblUsername, gbc);
        txtUsername = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 0; add(txtUsername, gbc);

        JLabel lblPassword = new JLabel("Password:");
        gbc.gridx = 0; gbc.gridy = 1; add(lblPassword, gbc);
        txtPassword = new JPasswordField(20);
        gbc.gridx = 1; gbc.gridy = 1; add(txtPassword, gbc);

        btnLogin = new JButton("Login");
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER; add(btnLogin, gbc);

        lblStatus = new JLabel(" "); lblStatus.setForeground(Color.RED);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER; add(lblStatus, gbc);

        btnLogin.addActionListener(e -> performLogin());
        txtPassword.addActionListener(e -> performLogin());

        pack();
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) { System.exit(0); }
        });
    }

    private void performLogin() {
        String username = txtUsername.getText();
        char[] password = txtPassword.getPassword();
        if (username.equals(VALID_USERNAME) && Arrays.equals(password, VALID_PASSWORD)) {
            loggedIn = true;
            lblStatus.setText("Login berhasil!"); lblStatus.setForeground(Color.GREEN);
            Timer timer = new Timer(1000, e -> dispose());
            timer.setRepeats(false);
            timer.start();
        } else {
            loggedIn = false;
            lblStatus.setText("Username atau password salah!");
            txtPassword.setText("");
            txtUsername.requestFocus();
        }
        Arrays.fill(password, '0');
    }

    public boolean isLoggedIn() { return loggedIn; }
}

// Kelas untuk item menu F&B individual (Sama seperti sebelumnya)
class MenuItem {
    String name;
    double price;
    public MenuItem(String name, double price) {
        this.name = name; this.price = price;
    }
    @Override public String toString() { return name + " (" + BilliardKasirApp.currencyFormatter.format(price) + ")"; }
}

// Kelas untuk item F&B yang dipesan (individual) (Sama seperti sebelumnya)
class OrderItem {
    MenuItem menuItem;
    int quantity;
    public OrderItem(MenuItem menuItem, int quantity) {
        this.menuItem = menuItem; this.quantity = quantity;
    }
    public double getTotalPrice() { return menuItem.price * quantity; }
    @Override public String toString() { return quantity + "x " + menuItem.name; }
}

// Kelas untuk menu paket (Sama seperti sebelumnya)
class PackageMenuItem {
    String name;
    double price;
    String description; 
    int includedPlayHours; 
    boolean isTimeBasedRentalPackage; 

    public PackageMenuItem(String name, double price, String description, int includedPlayHours, boolean isTimeBasedRentalPackage) {
        this.name = name;
        this.price = price;
        this.description = description;
        this.includedPlayHours = includedPlayHours;
        this.isTimeBasedRentalPackage = isTimeBasedRentalPackage;
    }

    @Override
    public String toString() {
        if (price == 0 && name.contains("Tidak Ada")) return name; 
        return name + " (" + BilliardKasirApp.currencyFormatter.format(price) + ")";
    }
    public String getFullDescription() {
        return name + " (" + BilliardKasirApp.currencyFormatter.format(price) + ")\n  Isi: " + description + 
               (isTimeBasedRentalPackage ? "\n  Termasuk: " + includedPlayHours + " jam main" : "");
    }
}


// Kelas untuk menyimpan informasi status meja (Sama seperti sebelumnya)
class TableInfo {
    String tableName;
    boolean occupied;
    String customerName;
    LocalTime startTime;
    List<OrderItem> orderedIndividualItems = new ArrayList<>(); 
    double individualFnbTotal = 0.0;                           
    List<PackageMenuItem> orderedPackages = new ArrayList<>(); 
    double packagesTotal = 0.0;                               

    public TableInfo(String tableName) {
        this.tableName = tableName;
        this.occupied = false;
    }

    public void startSession(String customerName, LocalTime startTime, PackageMenuItem initialPackage) {
        this.occupied = true;
        this.customerName = customerName;
        this.startTime = startTime;
        this.orderedIndividualItems.clear(); 
        this.individualFnbTotal = 0.0;
        this.orderedPackages.clear();      
        this.packagesTotal = 0.0;

        if (initialPackage != null && initialPackage.price > 0) { 
            this.orderedPackages.add(initialPackage);
            this.packagesTotal += initialPackage.price;
        }
    }
    
    public void addCafeOrder(List<OrderItem> newIndividualItems, List<PackageMenuItem> newPackages) {
        if (!this.occupied) return; 

        for (OrderItem item : newIndividualItems) {
            this.orderedIndividualItems.add(item);
            this.individualFnbTotal += item.getTotalPrice();
        }
        for (PackageMenuItem pkg : newPackages) {
            boolean canAddPackage = true;
            if (pkg.isTimeBasedRentalPackage) {
                for (PackageMenuItem existingPkg : this.orderedPackages) {
                    if (existingPkg.isTimeBasedRentalPackage) {
                        canAddPackage = false;
                        break;
                    }
                }
            }
            if (canAddPackage) {
                this.orderedPackages.add(pkg);
                this.packagesTotal += pkg.price;
            }
        }
    }

    public void vacate() {
        this.occupied = false;
        this.customerName = "";
        this.startTime = null;
        this.orderedIndividualItems.clear();
        this.individualFnbTotal = 0.0;
        this.orderedPackages.clear();
        this.packagesTotal = 0.0;
    }

    // Getters
    public String getTableName() { return tableName; }
    public boolean isOccupied() { return occupied; }
    public String getCustomerName() { return customerName; }
    public LocalTime getStartTime() { return startTime; }
    public List<OrderItem> getOrderedIndividualItems() { return orderedIndividualItems; }
    public double getIndividualFnbTotal() { return individualFnbTotal; }
    public List<PackageMenuItem> getOrderedPackages() { return orderedPackages; }
    public double getPackagesTotal() { return packagesTotal; }
}

// Kelas utilitas untuk koneksi database
class DBUtil {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/db_billiard"; 
    private static final String DB_USER = "root"; 
    private static final String DB_PASSWORD = "1234"; 

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
        try { if (stmt != null) stmt.close(); } catch (SQLException ignored) {}
        try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
    }
     public static void close(Connection conn, Statement stmt) {
        close(conn, stmt, null);
    }
}

// Kelas untuk menyimpan data riwayat transaksi (untuk JTable)
class TransactionHistoryRecord {
    int transactionId;
    String tableName;
    String customerName;
    LocalDateTime transactionTime;
    double totalBill;
    String paymentMethod;

    // Data tambahan untuk struk
    LocalTime startTime;
    LocalTime endTime;
    int totalPlayDurationMinutes;
    double rentalCost;
    double individualFnbCost;
    double packageCost;
    double additionalCost;
    double memberDiscountAmount;
    double manualDiscountAmount;
    double amountPaid;
    double changeGiven;
    List<Map<String, Object>> items; 


    public TransactionHistoryRecord(int transactionId, String tableName, String customerName, LocalDateTime transactionTime, double totalBill, String paymentMethod) {
        this.transactionId = transactionId;
        this.tableName = tableName;
        this.customerName = customerName;
        this.transactionTime = transactionTime;
        this.totalBill = totalBill;
        this.paymentMethod = paymentMethod;
        this.items = new ArrayList<>();
    }

     public TransactionHistoryRecord(int transactionId, String tableName, String customerName, 
                                    Timestamp transactionTimestamp, double totalBill, String paymentMethod,
                                    Time startTimeSql, Time endTimeSql, int durationMinutes,
                                    double rentalCost, double individualFnbCost, double packageCost,
                                    double additionalCost, double memberDiscount, double manualDiscount,
                                    double amountPaid, double changeGiven) {
        this.transactionId = transactionId;
        this.tableName = tableName;
        this.customerName = customerName;
        this.transactionTime = transactionTimestamp.toLocalDateTime();
        this.totalBill = totalBill;
        this.paymentMethod = paymentMethod;
        this.startTime = startTimeSql != null ? startTimeSql.toLocalTime() : null;
        this.endTime = endTimeSql != null ? endTimeSql.toLocalTime() : null;
        this.totalPlayDurationMinutes = durationMinutes;
        this.rentalCost = rentalCost;
        this.individualFnbCost = individualFnbCost;
        this.packageCost = packageCost;
        this.additionalCost = additionalCost;
        this.memberDiscountAmount = memberDiscount;
        this.manualDiscountAmount = manualDiscount;
        this.amountPaid = amountPaid;
        this.changeGiven = changeGiven;
        this.items = new ArrayList<>(); 
    }
}


public class BilliardKasirApp extends JFrame {
    // Komponen Tab Pembayaran Kasir
    private JComboBox<String> comboNomorMejaKasir;
    private JTextField txtNamaPelangganKasir;
    private JTextField txtWaktuMulaiKasir;
    private JTextField txtWaktuSelesaiKasir;
    private JTextField txtHargaPerJamKasir; 
    private JTextField txtBiayaMakananMinumanKasir; 
    private JTextField txtTotalBiayaPaketKasir;    
    private JTextField txtBiayaTambahanKasir;
    private JTextField txtMemberIdKasirPembayaran; 
    private JButton btnCariMemberKasirPembayaran;   
    private JLabel lblInfoMemberKasirPembayaran;   
    private JTextField txtDiskonKasir; 
    private JButton btnHitungTotalKasir;
    private JButton btnResetKasir;
    private JButton btnSetWaktuSelesaiKasirSekarang;
    private JTextArea areaStrukKasir;

    private JRadioButton radioTunai, radioQris, radioTf;
    private ButtonGroup groupMetodePembayaran;
    private JTextField txtUangDiberikanKasir;
    private JTextField txtUangKembalianKasir;
    private JLabel lblGrandTotalTagihanKasir; 


    // Formatter
    public static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter dateTimeDbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter dateTimeDisplayFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    public static final DateTimeFormatter dateDisplayFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy"); 
    public static final DateTimeFormatter dateDbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); 
    public static final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

    // Konstanta Tarif Dinamis
    private static final double RATE_PAGI = 30000.0;
    private static final double RATE_MALAM = 50000.0;
    private static final LocalTime PAGI_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime PAGI_END_TIME_EXCLUSIVE = LocalTime.of(18, 0); 
    private static final LocalTime MALAM_START_TIME = LocalTime.of(18, 0);
    private static final LocalTime MALAM_END_TIME_EXCLUSIVE_NEXT_DAY = LocalTime.of(1, 0); 

    public static final double DISKON_MEMBER_REGULER = 0.05; 
    public static final double DISKON_MEMBER_GOLD = 0.10;    
    public static final double DISKON_MEMBER_ATLET = 0.20;   
    
    private static final String NAMA_TEMPAT_BILLIARD = "8Ball-Poll Master";

    // Data
    private Map<String, TableInfo> tableStatusMap = new LinkedHashMap<>();
    private TableStatusPanel tableStatusPanel;
    private OrderMejaPanel orderMejaPanel;       
    private OrderKafePanel orderKafePanel;       
    private HistoriPemesananPanel historiPemesananPanel; 
    private MemberPanel memberPanel; 
    private List<MenuItem> menuItemsList;          
    private List<PackageMenuItem> packageMenuItemsList; 
    private double currentTotalTagihanKasir = 0.0; 
    private String currentMemberCategoryKasir = null; 
    private String currentMemberNameKasir = null;     

    public BilliardKasirApp() {
        setTitle("" + NAMA_TEMPAT_BILLIARD);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1150, 800); 
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        initializeData();
        initComponents();      
        addListenersForPembayaranTab();        
        
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));
    }
    
    private void initializeData() {
        String[] defaultTableNames = {"Meja 1", "Meja 2", "Meja 3", "Meja 4", "Meja 5", "Meja 6", "VIP 1", "VIP 2", "VIP 3"};
        if (tableStatusMap.isEmpty()) {
            for (String tableName : defaultTableNames) {
                tableStatusMap.put(tableName, new TableInfo(tableName));
            }
        }

        menuItemsList = new ArrayList<>();
        menuItemsList.add(new MenuItem("Kopi Hitam", 10000));
        menuItemsList.add(new MenuItem("Es Teh Manis", 8000));
        menuItemsList.add(new MenuItem("Mie Instan Kuah", 15000));
        menuItemsList.add(new MenuItem("Mie Instan Goreng", 16000));
        menuItemsList.add(new MenuItem("Kentang Goreng", 20000));
        menuItemsList.add(new MenuItem("Roti Bakar Coklat Keju", 18000));
        menuItemsList.add(new MenuItem("Air Mineral Botol", 5000));
        menuItemsList.add(new MenuItem("Minuman Soda", 12000));

        packageMenuItemsList = new ArrayList<>();
        packageMenuItemsList.add(new PackageMenuItem("Paket Hemat A", 25000, "1 Mie Instan + 1 Es Teh", 0, false));
        packageMenuItemsList.add(new PackageMenuItem("Paket Duo F&B", 45000, "2 Minuman Soda + 1 Kentang Goreng", 0, false));
        packageMenuItemsList.add(new PackageMenuItem("Paket Ngemil Santai", 30000, "1 Roti Bakar + 1 Kopi Hitam", 0, false));
        packageMenuItemsList.add(new PackageMenuItem("Paket Pagi Happy (3 Jam)", 100000, "Main 3 Jam + 2 Air Mineral", 3, true)); 
        packageMenuItemsList.add(new PackageMenuItem("Paket Malam Seru (2 Jam)", 75000, "2 Jam Main + 1 Minuman Soda", 2, true));
    }

    private double getHourlyRateForTime(LocalTime time) {
        if (!time.isBefore(PAGI_START_TIME) && time.isBefore(PAGI_END_TIME_EXCLUSIVE)) {
            return RATE_PAGI;
        }
        if (!time.isBefore(MALAM_START_TIME)) { 
            return RATE_MALAM;
        }
        if (time.isBefore(MALAM_END_TIME_EXCLUSIVE_NEXT_DAY)) { 
            return RATE_MALAM;
        }
        if (time.isBefore(PAGI_START_TIME)) { 
             return RATE_MALAM; 
        }
        return RATE_MALAM; 
    }


    private void initComponents() {
        JPanel panelHeaderGlobal = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel lblNamaTempatGlobal = new JLabel(NAMA_TEMPAT_BILLIARD);
        lblNamaTempatGlobal.setFont(new Font("Arial", Font.BOLD, 18));
        panelHeaderGlobal.add(lblNamaTempatGlobal);
        panelHeaderGlobal.setBorder(new EmptyBorder(0,0,10,0)); 
        add(panelHeaderGlobal, BorderLayout.NORTH); 

        orderMejaPanel = new OrderMejaPanel();
        orderKafePanel = new OrderKafePanel();
        tableStatusPanel = new TableStatusPanel(); 
        historiPemesananPanel = new HistoriPemesananPanel(); 
        memberPanel = new MemberPanel(); 

        orderMejaPanel.setTableStatusPanelRef(tableStatusPanel);
        orderKafePanel.setTableStatusPanelRef(tableStatusPanel);

        // --- Panel Konten untuk Tab Pembayaran Kasir ---
        JPanel panelInputKasirFields = new JPanel(new GridBagLayout());
        panelInputKasirFields.setBorder(BorderFactory.createTitledBorder("Detail Pembayaran"));
        GridBagConstraints gbcKasir = new GridBagConstraints();
        gbcKasir.fill = GridBagConstraints.HORIZONTAL;
        gbcKasir.insets = new Insets(5, 5, 5, 5);
        gbcKasir.anchor = GridBagConstraints.WEST;

        int yPosKasir = 0;
        // Baris 0
        gbcKasir.gridx = 0; gbcKasir.gridy = yPosKasir; panelInputKasirFields.add(new JLabel("Nomor Meja:"), gbcKasir);
        comboNomorMejaKasir = new JComboBox<>(tableStatusMap.keySet().toArray(new String[0]));
        gbcKasir.gridx = 1; gbcKasir.gridwidth = 2; panelInputKasirFields.add(comboNomorMejaKasir, gbcKasir); yPosKasir++;

        // Baris 1
        gbcKasir.gridx = 0; gbcKasir.gridy = yPosKasir; gbcKasir.gridwidth = 1; panelInputKasirFields.add(new JLabel("Nama Pelanggan:"), gbcKasir);
        txtNamaPelangganKasir = new JTextField(20); txtNamaPelangganKasir.setEditable(false);
        gbcKasir.gridx = 1; gbcKasir.gridwidth = 2; panelInputKasirFields.add(txtNamaPelangganKasir, gbcKasir); yPosKasir++;

        // Baris 2
        gbcKasir.gridx = 0; gbcKasir.gridy = yPosKasir; gbcKasir.gridwidth = 1; panelInputKasirFields.add(new JLabel("Waktu Mulai (HH:mm):"), gbcKasir);
        txtWaktuMulaiKasir = new JTextField(10); txtWaktuMulaiKasir.setEditable(false);
        gbcKasir.gridx = 1; gbcKasir.gridwidth = 2; panelInputKasirFields.add(txtWaktuMulaiKasir, gbcKasir); yPosKasir++;

        // Baris 3
        gbcKasir.gridx = 0; gbcKasir.gridy = yPosKasir; gbcKasir.gridwidth = 1; panelInputKasirFields.add(new JLabel("Waktu Selesai (HH:mm):"), gbcKasir);
        txtWaktuSelesaiKasir = new JTextField(10);
        gbcKasir.gridx = 1; gbcKasir.gridwidth = 1; panelInputKasirFields.add(txtWaktuSelesaiKasir, gbcKasir);
        btnSetWaktuSelesaiKasirSekarang = new JButton("Sekarang");
        gbcKasir.gridx = 2; gbcKasir.gridwidth = 1; panelInputKasirFields.add(btnSetWaktuSelesaiKasirSekarang, gbcKasir); yPosKasir++;

        // Baris 4
        gbcKasir.gridx = 0; gbcKasir.gridy = yPosKasir; gbcKasir.gridwidth = 1; panelInputKasirFields.add(new JLabel("Tarif Sewa Meja:"), gbcKasir);
        txtHargaPerJamKasir = new JTextField("Dinamis (Pagi/Malam)", 15); 
        txtHargaPerJamKasir.setEditable(false); 
        txtHargaPerJamKasir.setFont(new Font("Arial", Font.ITALIC, 12));
        txtHargaPerJamKasir.setForeground(Color.GRAY);
        gbcKasir.gridx = 1; gbcKasir.gridwidth = 2; panelInputKasirFields.add(txtHargaPerJamKasir, gbcKasir); yPosKasir++;

        // Baris 5
        gbcKasir.gridx = 0; gbcKasir.gridy = yPosKasir; gbcKasir.gridwidth = 1; panelInputKasirFields.add(new JLabel("Biaya F&B Indv. (Rp):"), gbcKasir);
        txtBiayaMakananMinumanKasir = new JTextField("0", 15); txtBiayaMakananMinumanKasir.setEditable(false);
        gbcKasir.gridx = 1; gbcKasir.gridwidth = 2; panelInputKasirFields.add(txtBiayaMakananMinumanKasir, gbcKasir); yPosKasir++;

        // Baris 6
        gbcKasir.gridx = 0; gbcKasir.gridy = yPosKasir; gbcKasir.gridwidth = 1; panelInputKasirFields.add(new JLabel("Total Biaya Paket (Rp):"), gbcKasir);
        txtTotalBiayaPaketKasir = new JTextField("0", 15); txtTotalBiayaPaketKasir.setEditable(false);
        gbcKasir.gridx = 1; gbcKasir.gridwidth = 2; panelInputKasirFields.add(txtTotalBiayaPaketKasir, gbcKasir); yPosKasir++;
        
        // Baris 7
        gbcKasir.gridx = 0; gbcKasir.gridy = yPosKasir; gbcKasir.gridwidth = 1; panelInputKasirFields.add(new JLabel("Biaya Tambahan (Rp):"), gbcKasir);
        txtBiayaTambahanKasir = new JTextField("0", 15);
        gbcKasir.gridx = 1; gbcKasir.gridwidth = 2; panelInputKasirFields.add(txtBiayaTambahanKasir, gbcKasir); yPosKasir++;
        
        // Baris 8: ID Member untuk Pembayaran
        gbcKasir.gridx = 0; gbcKasir.gridy = yPosKasir; panelInputKasirFields.add(new JLabel("ID Member (Jika ada):"), gbcKasir);
        txtMemberIdKasirPembayaran = new JTextField(15);
        gbcKasir.gridx = 1; gbcKasir.gridwidth = 1; panelInputKasirFields.add(txtMemberIdKasirPembayaran, gbcKasir);
        btnCariMemberKasirPembayaran = new JButton("Cari");
        gbcKasir.gridx = 2; gbcKasir.gridwidth = 1; panelInputKasirFields.add(btnCariMemberKasirPembayaran, gbcKasir); yPosKasir++;

        // Baris 9: Info Member yang Ditemukan
        lblInfoMemberKasirPembayaran = new JLabel("Member: -");
        lblInfoMemberKasirPembayaran.setFont(new Font("Arial", Font.ITALIC, 12));
        gbcKasir.gridx = 1; gbcKasir.gridy = yPosKasir; gbcKasir.gridwidth = 2; panelInputKasirFields.add(lblInfoMemberKasirPembayaran, gbcKasir); yPosKasir++;


        // Baris 10
        gbcKasir.gridx = 0; gbcKasir.gridy = yPosKasir; gbcKasir.gridwidth = 1; panelInputKasirFields.add(new JLabel("Diskon Manual (Rp):"), gbcKasir);
        txtDiskonKasir = new JTextField("0", 15);
        gbcKasir.gridx = 1; gbcKasir.gridwidth = 2; panelInputKasirFields.add(txtDiskonKasir, gbcKasir); yPosKasir++;

        // Baris 11
        gbcKasir.gridx = 0; gbcKasir.gridy = yPosKasir; panelInputKasirFields.add(new JLabel("TOTAL TAGIHAN (Rp):"), gbcKasir);
        lblGrandTotalTagihanKasir = new JLabel(currencyFormatter.format(0));
        lblGrandTotalTagihanKasir.setFont(new Font("Arial", Font.BOLD, 14));
        lblGrandTotalTagihanKasir.setForeground(Color.BLUE);
        gbcKasir.gridx = 1; gbcKasir.gridwidth = 2; panelInputKasirFields.add(lblGrandTotalTagihanKasir, gbcKasir); yPosKasir++;

        // Baris 12
        gbcKasir.gridx = 0; gbcKasir.gridy = yPosKasir; panelInputKasirFields.add(new JLabel("Metode Pembayaran:"), gbcKasir);
        JPanel panelMetodeBayar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        radioTunai = new JRadioButton("Tunai"); radioTunai.setSelected(true);
        radioQris = new JRadioButton("QRIS");
        radioTf = new JRadioButton("Transfer Bank");
        groupMetodePembayaran = new ButtonGroup();
        groupMetodePembayaran.add(radioTunai); groupMetodePembayaran.add(radioQris); groupMetodePembayaran.add(radioTf);
        panelMetodeBayar.add(radioTunai); panelMetodeBayar.add(radioQris); panelMetodeBayar.add(radioTf);
        gbcKasir.gridx = 1; gbcKasir.gridwidth = 2; panelInputKasirFields.add(panelMetodeBayar, gbcKasir); yPosKasir++;

        // Baris 13
        gbcKasir.gridx = 0; gbcKasir.gridy = yPosKasir; panelInputKasirFields.add(new JLabel("Uang Diberikan (Rp):"), gbcKasir);
        txtUangDiberikanKasir = new JTextField("0", 15);
        gbcKasir.gridx = 1; gbcKasir.gridwidth = 2; panelInputKasirFields.add(txtUangDiberikanKasir, gbcKasir); yPosKasir++;

        // Baris 14
        gbcKasir.gridx = 0; gbcKasir.gridy = yPosKasir; panelInputKasirFields.add(new JLabel("Uang Kembalian (Rp):"), gbcKasir);
        txtUangKembalianKasir = new JTextField("0", 15); txtUangKembalianKasir.setEditable(false);
        txtUangKembalianKasir.setFont(new Font("Arial", Font.BOLD, 12));
        txtUangKembalianKasir.setForeground(Color.DARK_GRAY);
        gbcKasir.gridx = 1; gbcKasir.gridwidth = 2; panelInputKasirFields.add(txtUangKembalianKasir, gbcKasir); yPosKasir++;

        // Baris 15: Tombol Aksi Kasir
        JPanel panelTombolKasir = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        btnHitungTotalKasir = new JButton("Proses Pembayaran & Selesaikan Sesi");
        btnResetKasir = new JButton("Reset Form Kasir");
        panelTombolKasir.add(btnHitungTotalKasir);
        panelTombolKasir.add(btnResetKasir);
        gbcKasir.gridx = 0; gbcKasir.gridy = yPosKasir; gbcKasir.gridwidth = 3; 
        gbcKasir.fill = GridBagConstraints.HORIZONTAL; 
        gbcKasir.anchor = GridBagConstraints.CENTER; 
        panelInputKasirFields.add(panelTombolKasir, gbcKasir); yPosKasir++;


        // Area Struk Kasir
        areaStrukKasir = new JTextArea(20, 45); 
        areaStrukKasir.setEditable(false);
        areaStrukKasir.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPaneStrukKasir = new JScrollPane(areaStrukKasir);
        scrollPaneStrukKasir.setBorder(BorderFactory.createTitledBorder("Struk Pembayaran"));
        
        // Panel Utama untuk Tab Pembayaran (menggunakan BorderLayout)
        JPanel panelPembayaranTab = new JPanel(new BorderLayout(10, 0)); 
        JScrollPane scrollableInputPanel = new JScrollPane(panelInputKasirFields); 
        scrollableInputPanel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollableInputPanel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        JSplitPane splitPanePembayaran = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollableInputPanel, scrollPaneStrukKasir);
        splitPanePembayaran.setResizeWeight(0.45); 
        splitPanePembayaran.setOneTouchExpandable(true); 
        panelPembayaranTab.add(splitPanePembayaran, BorderLayout.CENTER);


        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Pesan Meja", orderMejaPanel); 
        tabbedPane.addTab("Pesan Kafe", orderKafePanel); 
        tabbedPane.addTab("Status Meja", tableStatusPanel);
        tabbedPane.addTab("Manajemen Member", memberPanel); // Tab baru
        tabbedPane.addTab("Pembayaran", panelPembayaranTab); 
        tabbedPane.addTab("Laporan", historiPemesananPanel); // Ganti nama tab
        
        tabbedPane.setSelectedComponent(tableStatusPanel); 

        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedComponent() == panelPembayaranTab) {
                String tableToLoad = orderMejaPanel.getLastSelectedStartedTable(); 
                if (tableToLoad == null) { 
                    tableToLoad = orderKafePanel.getLastSelectedActiveTable(); 
                }
                if (tableToLoad == null) {
                    for (Map.Entry<String, TableInfo> entry : tableStatusMap.entrySet()) {
                        if (entry.getValue().isOccupied()) {
                            tableToLoad = entry.getKey();
                            break;
                        }
                    }
                }
                if (tableToLoad != null) {
                    populateKasirForPayment(tableToLoad);
                } else {
                     if (comboNomorMejaKasir.getItemCount() > 0) {
                        comboNomorMejaKasir.setSelectedIndex(0); 
                        resetFormKasir(false); 
                     }
                }
            } else if (tabbedPane.getSelectedComponent() == tableStatusPanel) {
                tableStatusPanel.refreshStatusDisplay(); 
            } else if (tabbedPane.getSelectedComponent() == orderMejaPanel) {
                orderMejaPanel.refreshTableComboBox(); 
            } else if (tabbedPane.getSelectedComponent() == orderKafePanel) {
                orderKafePanel.refreshActiveTableComboBox();
            } else if (tabbedPane.getSelectedComponent() == historiPemesananPanel) {
                historiPemesananPanel.loadHistory(); 
            } else if (tabbedPane.getSelectedComponent() == memberPanel) {
                memberPanel.loadMembers(); 
            }
        });
        
        add(tabbedPane, BorderLayout.CENTER); 
    }

    private void previewTotalTagihanKasir(String tableName) {
        TableInfo tableInfo = tableStatusMap.get(tableName);
        // currentMemberCategoryKasir dan currentMemberNameKasir sudah di-handle oleh btnCariMemberKasirPembayaran

        if (tableInfo == null || !tableInfo.isOccupied() || txtWaktuSelesaiKasir.getText().trim().isEmpty()) {
            lblGrandTotalTagihanKasir.setText(currencyFormatter.format(0));
            currentTotalTagihanKasir = 0.0;
            if (radioQris.isSelected() || radioTf.isSelected()) {
                txtUangDiberikanKasir.setText(currencyFormatter.format(0));
            }
            return;
        }
        try {
            LocalTime waktuMulai = tableInfo.getStartTime();
            LocalTime waktuSelesaiInput = LocalTime.parse(txtWaktuSelesaiKasir.getText(), timeFormatter);
            
            double biayaFnbIndividual = tableInfo.getIndividualFnbTotal();
            double biayaSemuaPaket = tableInfo.getPackagesTotal();
            double biayaTambahan = 0;
            try { biayaTambahan = Double.parseDouble(txtBiayaTambahanKasir.getText().replaceAll("[^\\d]", "")); } catch (NumberFormatException ignored) {}
            double diskonManual = 0;
            try { diskonManual = Double.parseDouble(txtDiskonKasir.getText().replaceAll("[^\\d]", "")); } catch (NumberFormatException ignored) {}
            
            double biayaSewaAktual = 0;
            PackageMenuItem activeTimePackage = null;
            for (PackageMenuItem pkg : tableInfo.getOrderedPackages()) {
                if (pkg.isTimeBasedRentalPackage) { activeTimePackage = pkg; break; }
            }

            if (activeTimePackage != null) {
                LocalTime packageEndTime = waktuMulai.plusHours(activeTimePackage.includedPlayHours);
                if (waktuSelesaiInput.isAfter(packageEndTime)) { 
                    LocalTime overtimeStartTime = packageEndTime;
                    Duration actualOvertimeDuration = Duration.between(overtimeStartTime, waktuSelesaiInput);
                    if (actualOvertimeDuration.isNegative()) actualOvertimeDuration = actualOvertimeDuration.plusDays(1);
                    long overtimeMinutesPlayed = actualOvertimeDuration.toMinutes();

                    if (overtimeMinutesPlayed > 0) {
                        for (long i = 0; i < overtimeMinutesPlayed; i++) {
                            biayaSewaAktual += getHourlyRateForTime(overtimeStartTime) / 60.0;
                            overtimeStartTime = overtimeStartTime.plusMinutes(1);
                        }
                    }
                }
            } else { 
                Duration totalSessionDuration = Duration.between(waktuMulai, waktuSelesaiInput);
                if (totalSessionDuration.isNegative()) totalSessionDuration = totalSessionDuration.plusDays(1);
                long totalMinutesPlayed = totalSessionDuration.toMinutes();
                LocalTime iterTime = waktuMulai;
                if (totalMinutesPlayed > 0) {
                    for (long i = 0; i < totalMinutesPlayed; i++) {
                        biayaSewaAktual += getHourlyRateForTime(iterTime) / 60.0;
                        iterTime = iterTime.plusMinutes(1);
                    }
                }
            }
            
            double subTotal = biayaSewaAktual + biayaFnbIndividual + biayaSemuaPaket + biayaTambahan;
            
            double diskonMemberPersenOtomatis = 0;
            if (currentMemberCategoryKasir != null && currentMemberNameKasir != null) { 
                switch (currentMemberCategoryKasir) {
                    case "Reguler": diskonMemberPersenOtomatis = DISKON_MEMBER_REGULER; break;
                    case "Gold":    diskonMemberPersenOtomatis = DISKON_MEMBER_GOLD;    break;
                    case "Atlet":   diskonMemberPersenOtomatis = DISKON_MEMBER_ATLET;   break;
                }
            }
            double diskonMemberAmount = subTotal * diskonMemberPersenOtomatis;
            double totalDiskon = diskonMemberAmount + diskonManual; 

            currentTotalTagihanKasir = subTotal - totalDiskon;
            if (currentTotalTagihanKasir < 0) currentTotalTagihanKasir = 0;

            lblGrandTotalTagihanKasir.setText(currencyFormatter.format(currentTotalTagihanKasir));

            if (radioQris.isSelected() || radioTf.isSelected()) {
                txtUangDiberikanKasir.setText(currencyFormatter.format(currentTotalTagihanKasir));
                 txtUangKembalianKasir.setText(currencyFormatter.format(0));
            }

        } catch (DateTimeParseException | NumberFormatException ex) {
            lblGrandTotalTagihanKasir.setText("Error Input");
            currentTotalTagihanKasir = 0.0;
        }
    }


    public void populateKasirForPayment(String tableName) {
        TableInfo tableInfo = tableStatusMap.get(tableName);
        txtHargaPerJamKasir.setText("Tarif Dinamis"); 
        txtWaktuSelesaiKasir.setEditable(true);
        btnSetWaktuSelesaiKasirSekarang.setEnabled(true);
        radioTunai.setSelected(true); 
        txtUangDiberikanKasir.setEditable(true);
        txtUangDiberikanKasir.setText("0");
        txtUangKembalianKasir.setText(currencyFormatter.format(0));
        txtMemberIdKasirPembayaran.setText(""); 
        lblInfoMemberKasirPembayaran.setText("Member: -"); 
        lblInfoMemberKasirPembayaran.setForeground(Color.BLACK); // Reset warna label info member
        currentMemberCategoryKasir = null; 
        currentMemberNameKasir = null;


        if (tableInfo != null && tableInfo.isOccupied()) {
            comboNomorMejaKasir.setSelectedItem(tableName); 
            txtNamaPelangganKasir.setText(tableInfo.getCustomerName());
            txtWaktuMulaiKasir.setText(tableInfo.getStartTime().format(timeFormatter));
            txtBiayaMakananMinumanKasir.setText(String.format("%,.0f", tableInfo.getIndividualFnbTotal())); 
            txtTotalBiayaPaketKasir.setText(String.format("%,.0f", tableInfo.getPackagesTotal()));
            
            PackageMenuItem activeTimePackage = null;
            for (PackageMenuItem pkg : tableInfo.getOrderedPackages()) {
                if (pkg.isTimeBasedRentalPackage) {
                    activeTimePackage = pkg;
                    break;
                }
            }

            if (activeTimePackage != null) {
                LocalTime expectedEndTime = tableInfo.getStartTime().plusHours(activeTimePackage.includedPlayHours);
                txtWaktuSelesaiKasir.setText(expectedEndTime.format(timeFormatter));
            } else {
                txtWaktuSelesaiKasir.setText(""); 
            }
            areaStrukKasir.setText(""); 
            // checkMemberKasir.setSelected(false); // Dihilangkan
            txtDiskonKasir.setText("0");
            txtBiayaTambahanKasir.setText("0");
            previewTotalTagihanKasir(tableName); 
            
        } else if (tableInfo != null && !tableInfo.isOccupied()) {
            comboNomorMejaKasir.setSelectedItem(tableName);
            resetFormKasir(false); 
            lblGrandTotalTagihanKasir.setText(currencyFormatter.format(0));
            currentTotalTagihanKasir = 0.0;
        }
    }

    private void addListenersForPembayaranTab() { 
        btnSetWaktuSelesaiKasirSekarang.addActionListener(e -> {
            txtWaktuSelesaiKasir.setText(LocalTime.now().format(timeFormatter));
            previewTotalTagihanKasir((String)comboNomorMejaKasir.getSelectedItem()); 
        });
        
        txtWaktuSelesaiKasir.addActionListener(e -> previewTotalTagihanKasir((String)comboNomorMejaKasir.getSelectedItem()));
        
        btnHitungTotalKasir.addActionListener(e -> hitungDanTampilkanTagihanKasir());
        btnResetKasir.addActionListener(e -> resetFormKasir(true));

        comboNomorMejaKasir.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String selectedTable = (String) comboNomorMejaKasir.getSelectedItem();
                if (selectedTable != null) {
                    populateKasirForPayment(selectedTable); 
                    TableInfo tableInfo = tableStatusMap.get(selectedTable);
                    if (tableInfo != null && tableInfo.isOccupied()) {
                         txtWaktuSelesaiKasir.requestFocus();
                    }
                }
            }
        });

        ActionListener paymentMethodListener = e -> {
            previewTotalTagihanKasir((String)comboNomorMejaKasir.getSelectedItem()); 
            if (radioTunai.isSelected()) {
                txtUangDiberikanKasir.setEditable(true);
                txtUangDiberikanKasir.setText("0");
                txtUangDiberikanKasir.requestFocus();
                txtUangKembalianKasir.setText(currencyFormatter.format(0));
            } else { 
                txtUangDiberikanKasir.setEditable(false);
                txtUangDiberikanKasir.setText(currencyFormatter.format(currentTotalTagihanKasir)); 
                txtUangKembalianKasir.setText(currencyFormatter.format(0));
            }
        };
        radioTunai.addActionListener(paymentMethodListener);
        radioQris.addActionListener(paymentMethodListener);
        radioTf.addActionListener(paymentMethodListener);

        // Listener untuk tombol Cari Member di tab Pembayaran
        btnCariMemberKasirPembayaran.addActionListener(e -> {
            String memberId = txtMemberIdKasirPembayaran.getText().trim();
            if (memberId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Masukkan ID Member.", "Info", JOptionPane.INFORMATION_MESSAGE);
                lblInfoMemberKasirPembayaran.setText("Member: -");
                lblInfoMemberKasirPembayaran.setForeground(Color.BLACK);
                currentMemberCategoryKasir = null;
                currentMemberNameKasir = null;
                previewTotalTagihanKasir((String) comboNomorMejaKasir.getSelectedItem()); 
                return;
            }

            String sql = "SELECT member_name, member_category, is_active FROM members WHERE member_id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, memberId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    if (rs.getBoolean("is_active")) {
                        currentMemberNameKasir = rs.getString("member_name");
                        currentMemberCategoryKasir = rs.getString("member_category");
                        double diskonPersen = 0;
                        switch (currentMemberCategoryKasir) {
                            case "Reguler": diskonPersen = DISKON_MEMBER_REGULER * 100; break;
                            case "Gold":    diskonPersen = DISKON_MEMBER_GOLD * 100;    break;
                            case "Atlet":   diskonPersen = DISKON_MEMBER_ATLET * 100;   break;
                        }
                        lblInfoMemberKasirPembayaran.setText("Member: " + currentMemberNameKasir + " (" + currentMemberCategoryKasir + " - " + String.format("%.0f%%", diskonPersen) + ")");
                        lblInfoMemberKasirPembayaran.setForeground(new Color(0, 100, 0)); // Dark Green
                    } else {
                        lblInfoMemberKasirPembayaran.setText("Member: " + rs.getString("member_name") + " (Tidak Aktif)");
                        lblInfoMemberKasirPembayaran.setForeground(Color.RED);
                        currentMemberCategoryKasir = null; 
                        currentMemberNameKasir = rs.getString("member_name");
                    }
                } else {
                    lblInfoMemberKasirPembayaran.setText("Member ID tidak ditemukan.");
                    lblInfoMemberKasirPembayaran.setForeground(Color.RED);
                    currentMemberCategoryKasir = null;
                    currentMemberNameKasir = null;
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error mencari member: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
                lblInfoMemberKasirPembayaran.setText("Error mencari member.");
                lblInfoMemberKasirPembayaran.setForeground(Color.RED);
                currentMemberCategoryKasir = null;
                currentMemberNameKasir = null;
            }
            previewTotalTagihanKasir((String) comboNomorMejaKasir.getSelectedItem()); 
        });


        txtBiayaTambahanKasir.addActionListener(e -> previewTotalTagihanKasir((String)comboNomorMejaKasir.getSelectedItem()));
        txtDiskonKasir.addActionListener(e -> previewTotalTagihanKasir((String)comboNomorMejaKasir.getSelectedItem()));
        // checkMemberKasir.addActionListener(e -> previewTotalTagihanKasir((String)comboNomorMejaKasir.getSelectedItem())); // Dihilangkan
    }
    
    private void hitungDanTampilkanTagihanKasir() {
        String selectedTable = (String) comboNomorMejaKasir.getSelectedItem();
        if (selectedTable == null) {
            JOptionPane.showMessageDialog(this, "Silakan pilih meja.", "Error", JOptionPane.ERROR_MESSAGE); return;
        }
        TableInfo currentTable = tableStatusMap.get(selectedTable);

        if (currentTable == null || !currentTable.isOccupied()) {
            JOptionPane.showMessageDialog(this, "Tidak ada sesi aktif di meja " + selectedTable, "Informasi", JOptionPane.INFORMATION_MESSAGE); return;
        }
        
        previewTotalTagihanKasir(selectedTable);
        double totalTagihanFinal = currentTotalTagihanKasir; 

        if (txtWaktuSelesaiKasir.getText().trim().isEmpty()){
             PackageMenuItem activeTimePkg = currentTable.getOrderedPackages().stream().filter(p -> p.isTimeBasedRentalPackage).findFirst().orElse(null);
             if(activeTimePkg == null) { 
                JOptionPane.showMessageDialog(this, "Waktu selesai kosong!", "Input Kurang", JOptionPane.WARNING_MESSAGE); txtWaktuSelesaiKasir.requestFocus(); return;
             }
        }


        String metodePembayaranStr = "";
        double uangDiberikan = 0;
        double uangKembalian = 0;

        try {
            if (radioTunai.isSelected()) {
                metodePembayaranStr = "Tunai";
                try {
                    uangDiberikan = Double.parseDouble(txtUangDiberikanKasir.getText().replaceAll("[^\\d]", ""));
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Format uang diberikan salah.", "Error Input", JOptionPane.ERROR_MESSAGE);
                    txtUangDiberikanKasir.requestFocus();
                    return;
                }
                if (uangDiberikan < totalTagihanFinal) {
                    JOptionPane.showMessageDialog(this, "Uang yang diberikan kurang dari total tagihan.", "Input Kurang", JOptionPane.WARNING_MESSAGE);
                    txtUangDiberikanKasir.requestFocus();
                    return;
                }
                uangKembalian = uangDiberikan - totalTagihanFinal;
            } else if (radioQris.isSelected()) {
                metodePembayaranStr = "QRIS";
                uangDiberikan = totalTagihanFinal; 
                uangKembalian = 0;
            } else if (radioTf.isSelected()) {
                metodePembayaranStr = "Transfer Bank";
                uangDiberikan = totalTagihanFinal; 
                uangKembalian = 0;
            }
            txtUangDiberikanKasir.setText(currencyFormatter.format(uangDiberikan)); 
            txtUangKembalianKasir.setText(currencyFormatter.format(uangKembalian));

            String nomorMeja = currentTable.getTableName();
            String namaPelanggan = currentTable.getCustomerName();
            LocalTime waktuMulai = currentTable.getStartTime();
            LocalTime waktuSelesaiInput = LocalTime.parse(txtWaktuSelesaiKasir.getText(), timeFormatter); 
            double biayaFnbIndividual = currentTable.getIndividualFnbTotal(); 
            double biayaSemuaPaket = currentTable.getPackagesTotal();
            double biayaTambahan = Double.parseDouble(txtBiayaTambahanKasir.getText().replaceAll("[^\\d]", ""));
            double diskonManual = Double.parseDouble(txtDiskonKasir.getText().replaceAll("[^\\d]", ""));
            // boolean isMember = checkMemberKasir.isSelected(); // Dihilangkan

            double jamTotalMain = Duration.between(waktuMulai, waktuSelesaiInput).toMinutes() / 60.0;
            if (jamTotalMain < 0) jamTotalMain += 24;

            PackageMenuItem activeTimePackage = null;
            for (PackageMenuItem pkg : currentTable.getOrderedPackages()) {
                if (pkg.isTimeBasedRentalPackage) { activeTimePackage = pkg; break; }
            }
            double biayaSewaStruk = 0; 
            if (activeTimePackage != null) {
                LocalTime packageEndTime = waktuMulai.plusHours(activeTimePackage.includedPlayHours);
                if (waktuSelesaiInput.isAfter(packageEndTime)) {
                    LocalTime overtimeStartTime = packageEndTime;
                    Duration actualOvertimeDuration = Duration.between(overtimeStartTime, waktuSelesaiInput);
                    if(actualOvertimeDuration.isNegative()) actualOvertimeDuration = actualOvertimeDuration.plusDays(1);
                    long overtimeMinutesPlayed = actualOvertimeDuration.toMinutes();
                    if (overtimeMinutesPlayed > 0) {
                        for (long i = 0; i < overtimeMinutesPlayed; i++) {
                            biayaSewaStruk += getHourlyRateForTime(overtimeStartTime) / 60.0;
                            overtimeStartTime = overtimeStartTime.plusMinutes(1);
                        }
                    }
                }
            } else {
                LocalTime iterTime = waktuMulai;
                Duration totalSessionDuration = Duration.between(waktuMulai, waktuSelesaiInput);
                if (totalSessionDuration.isNegative()) totalSessionDuration = totalSessionDuration.plusDays(1);
                long totalMinutesPlayedLoop = totalSessionDuration.toMinutes();
                if (totalMinutesPlayedLoop > 0) {
                    for (long i = 0; i < totalMinutesPlayedLoop; i++) {
                        biayaSewaStruk += getHourlyRateForTime(iterTime) / 60.0;
                        iterTime = iterTime.plusMinutes(1);
                    }
                }
            }

            StringBuilder strukText = new StringBuilder();
            strukText.append("========================================\n");
            strukText.append("        STRUK PEMBAYARAN BILLIARD       \n");
            strukText.append("             ").append(NAMA_TEMPAT_BILLIARD).append("\n");
            strukText.append("========================================\n");
            strukText.append(String.format("%-25s: %s\n", "Nomor Meja", nomorMeja));
            strukText.append(String.format("%-25s: %s\n", "Nama Pelanggan", namaPelanggan));
            if (currentMemberNameKasir != null) { // Jika member terverifikasi
                strukText.append(String.format("%-25s: %s (%s)\n", "Member", currentMemberNameKasir, currentMemberCategoryKasir));
            }
            strukText.append(String.format("%-25s: %s\n", "Waktu Mulai", waktuMulai.format(timeFormatter)));
            strukText.append(String.format("%-25s: %s\n", "Waktu Selesai", waktuSelesaiInput.format(timeFormatter)));
            strukText.append(String.format("%-25s: %.2f jam (%.0f menit)\n", "Durasi Main Aktual", jamTotalMain, jamTotalMain * 60));
            strukText.append("----------------------------------------\n");

            if (activeTimePackage != null) {
                strukText.append(String.format("Paket Waktu (%s - %d jam)\n", activeTimePackage.name, activeTimePackage.includedPlayHours));
                if (biayaSewaStruk > 0) { 
                     strukText.append(String.format("%-25s: %s\n", "Biaya Sewa Overtime", currencyFormatter.format(biayaSewaStruk)));
                }
            } else {
                 strukText.append(String.format("%-25s: %s\n", "Biaya Sewa Meja", currencyFormatter.format(biayaSewaStruk)));
            }
            
            if (!currentTable.getOrderedIndividualItems().isEmpty()) {
                strukText.append("Pesanan F&B Individual:\n");
                for(OrderItem item : currentTable.getOrderedIndividualItems()){
                    strukText.append(String.format("  - %-20s: %s\n", item.toString().split(" \\(")[0], currencyFormatter.format(item.getTotalPrice())));
                }
            }
            strukText.append(String.format("%-25s: %s\n", "Total Biaya F&B Indv.", currencyFormatter.format(biayaFnbIndividual)));
            
            if (!currentTable.getOrderedPackages().isEmpty()) {
                strukText.append("Pesanan Paket Lainnya:\n"); 
                for(PackageMenuItem pkgItem : currentTable.getOrderedPackages()){
                    if (!pkgItem.isTimeBasedRentalPackage) { 
                        strukText.append(String.format("  - %-20s: %s\n", pkgItem.name, currencyFormatter.format(pkgItem.price)));
                    }
                }
            }
             strukText.append(String.format("%-25s: %s\n", "Total Biaya Semua Paket", currencyFormatter.format(biayaSemuaPaket)));


            strukText.append(String.format("%-25s: %s\n", "Biaya Tambahan", currencyFormatter.format(biayaTambahan)));
            strukText.append("----------------------------------------\n");
            double subTotalStruk = biayaSewaStruk + biayaFnbIndividual + biayaSemuaPaket + biayaTambahan;
            strukText.append(String.format("%-25s: %s\n", "Subtotal", currencyFormatter.format(subTotalStruk)));

            // Diskon Member Otomatis
            double diskonMemberOtomatisPersen = 0;
            String kategoriMemberDiskon = "";
            if (currentMemberCategoryKasir != null) {
                 switch (currentMemberCategoryKasir) {
                    case "Reguler": diskonMemberOtomatisPersen = DISKON_MEMBER_REGULER; kategoriMemberDiskon = "Reguler"; break;
                    case "Gold":    diskonMemberOtomatisPersen = DISKON_MEMBER_GOLD;    kategoriMemberDiskon = "Gold"; break;
                    case "Atlet":   diskonMemberOtomatisPersen = DISKON_MEMBER_ATLET;   kategoriMemberDiskon = "Atlet"; break;
                }
            }
            double diskonMemberAmountStruk = subTotalStruk * diskonMemberOtomatisPersen;


            double totalDiskonStruk = diskonMemberAmountStruk + diskonManual;
            
            if (diskonMemberAmountStruk > 0) {
                 strukText.append(String.format("Diskon Member (%s - %.0f%%): %s\n", kategoriMemberDiskon, diskonMemberOtomatisPersen * 100, currencyFormatter.format(diskonMemberAmountStruk)));
            }
            if (diskonManual > 0) strukText.append(String.format("%-25s: %s\n", "Diskon Manual", currencyFormatter.format(diskonManual)));
            strukText.append(String.format("%-25s: %s\n", "Total Diskon", currencyFormatter.format(totalDiskonStruk)));
            strukText.append("========================================\n");
            strukText.append(String.format("%-25s: %s\n", "TOTAL TAGIHAN", currencyFormatter.format(totalTagihanFinal)));
            strukText.append("----------------------------------------\n");
            strukText.append(String.format("%-25s: %s\n", "Metode Pembayaran", metodePembayaranStr));
            strukText.append(String.format("%-25s: %s\n", "Uang Diberikan", currencyFormatter.format(uangDiberikan)));
            strukText.append(String.format("%-25s: %s\n", "Uang Kembalian", currencyFormatter.format(uangKembalian)));
            strukText.append("========================================\n");
            strukText.append("          Terima Kasih Atas Kunjungan Anda!         \n");

            areaStrukKasir.setText(strukText.toString());
            
            saveTransactionToDB(currentTable, totalTagihanFinal, metodePembayaranStr, uangDiberikan, uangKembalian, waktuSelesaiInput, biayaSewaStruk, diskonMemberAmountStruk, diskonManual);

            currentTable.vacate(); 
            tableStatusPanel.refreshStatusDisplay(); 
            orderMejaPanel.refreshTableComboBox(); 
            orderKafePanel.refreshActiveTableComboBox(); 
            if (historiPemesananPanel != null) historiPemesananPanel.loadHistory(); 
            JOptionPane.showMessageDialog(this, "Pembayaran berhasil. Meja " + selectedTable + " kini tersedia.", "Pembayaran Selesai", JOptionPane.INFORMATION_MESSAGE);
            resetFormKasir(true); 

        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Format waktu salah! Gunakan HH:mm", "Error Input Waktu", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Input biaya/harga/diskon harus angka.", "Error Input Angka", JOptionPane.ERROR_MESSAGE);
             ex.printStackTrace(); 
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Terjadi kesalahan: " + ex.getMessage(), "Error Umum", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace(); 
        }
    }

    private void saveTransactionToDB(TableInfo tableInfo, double totalBill, String paymentMethod, 
                                 double amountPaid, double changeGiven, LocalTime endTime,
                                 double rentalCost, double memberDiscountAmount, double manualDiscountAmount) {
        Connection conn = null;
        PreparedStatement pstmtTransaction = null;
        PreparedStatement pstmtItem = null;
        ResultSet generatedKeys = null;
        int transactionId = -1;

        String insertTransactionSQL = "INSERT INTO transactions (table_name, customer_name, start_time, end_time, " +
                                      "total_play_duration_minutes, rental_cost, individual_fnb_cost, package_cost, " +
                                      "additional_cost, member_discount_amount, manual_discount_amount, total_bill, payment_method, " +
                                      "amount_paid, change_given, transaction_timestamp) " +
                                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertItemSQL = "INSERT INTO transaction_items (transaction_id, item_name, item_type, quantity, price_per_item, total_price) " +
                               "VALUES (?, ?, ?, ?, ?, ?)";

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false); 

            pstmtTransaction = conn.prepareStatement(insertTransactionSQL, Statement.RETURN_GENERATED_KEYS);
            pstmtTransaction.setString(1, tableInfo.getTableName());
            pstmtTransaction.setString(2, tableInfo.getCustomerName());
            pstmtTransaction.setTime(3, Time.valueOf(tableInfo.getStartTime()));
            pstmtTransaction.setTime(4, Time.valueOf(endTime));
            
            Duration duration = Duration.between(tableInfo.getStartTime(), endTime);
            if(duration.isNegative()) duration = duration.plusDays(1);
            pstmtTransaction.setInt(5, (int) duration.toMinutes());
            
            pstmtTransaction.setDouble(6, rentalCost); 
            pstmtTransaction.setDouble(7, tableInfo.getIndividualFnbTotal());
            pstmtTransaction.setDouble(8, tableInfo.getPackagesTotal()); 
            
            double additionalCost = 0;
            try { additionalCost = Double.parseDouble(txtBiayaTambahanKasir.getText().replaceAll("[^\\d]", "")); } catch (NumberFormatException ignored) {}
            pstmtTransaction.setDouble(9, additionalCost);

            pstmtTransaction.setDouble(10, memberDiscountAmount);
            pstmtTransaction.setDouble(11, manualDiscountAmount);
            pstmtTransaction.setDouble(12, totalBill);
            pstmtTransaction.setString(13, paymentMethod);
            pstmtTransaction.setDouble(14, amountPaid);
            pstmtTransaction.setDouble(15, changeGiven);
            pstmtTransaction.setTimestamp(16, Timestamp.valueOf(LocalDateTime.now()));

            int affectedRows = pstmtTransaction.executeUpdate();

            if (affectedRows > 0) {
                generatedKeys = pstmtTransaction.getGeneratedKeys();
                if (generatedKeys.next()) {
                    transactionId = generatedKeys.getInt(1);
                }
            } else {
                throw new SQLException("Gagal menyimpan transaksi, tidak ada baris yang terpengaruh.");
            }

            if (transactionId > 0) {
                pstmtItem = conn.prepareStatement(insertItemSQL);
                for (OrderItem item : tableInfo.getOrderedIndividualItems()) {
                    pstmtItem.setInt(1, transactionId);
                    pstmtItem.setString(2, item.menuItem.name);
                    pstmtItem.setString(3, "FNB");
                    pstmtItem.setInt(4, item.quantity);
                    pstmtItem.setDouble(5, item.menuItem.price);
                    pstmtItem.setDouble(6, item.getTotalPrice());
                    pstmtItem.addBatch();
                }
                for (PackageMenuItem pkg : tableInfo.getOrderedPackages()) {
                    pstmtItem.setInt(1, transactionId);
                    pstmtItem.setString(2, pkg.name);
                    pstmtItem.setString(3, "PAKET");
                    pstmtItem.setInt(4, 1); 
                    pstmtItem.setDouble(5, pkg.price);
                    pstmtItem.setDouble(6, pkg.price);
                    pstmtItem.addBatch();
                }
                pstmtItem.executeBatch();
            }
            conn.commit(); 
            System.out.println("Transaksi berhasil disimpan ke database.");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error saat menyimpan transaksi ke database: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback(); 
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        } finally {
            DBUtil.close(conn, pstmtItem); 
            if(pstmtTransaction != null && pstmtItem == null) DBUtil.close(null, pstmtTransaction, generatedKeys);
            else if (generatedKeys != null && pstmtItem == null) { try { generatedKeys.close(); } catch (SQLException ignored) {} }
        }
    }


    private void resetFormKasir(boolean fullReset) {
        if (fullReset) { 
          if (comboNomorMejaKasir.getItemCount() > 0) comboNomorMejaKasir.setSelectedIndex(0);
        }
        txtNamaPelangganKasir.setText("");
        txtWaktuMulaiKasir.setText("");
        txtWaktuSelesaiKasir.setText("");
        txtBiayaMakananMinumanKasir.setText("0");
        txtTotalBiayaPaketKasir.setText("0");
        txtBiayaTambahanKasir.setText("0");
        txtDiskonKasir.setText("0");
        // checkMemberKasir.setSelected(false); // Dihilangkan
        txtMemberIdKasirPembayaran.setText("");
        lblInfoMemberKasirPembayaran.setText("Member: -");
        currentMemberCategoryKasir = null;
        currentMemberNameKasir = null;

        areaStrukKasir.setText("");
        txtHargaPerJamKasir.setText("Tarif Dinamis");
        txtWaktuSelesaiKasir.setEditable(true);
        btnSetWaktuSelesaiKasirSekarang.setEnabled(true);
        
        radioTunai.setSelected(true);
        txtUangDiberikanKasir.setEditable(true);
        txtUangDiberikanKasir.setText("0");
        txtUangKembalianKasir.setText(currencyFormatter.format(0));
        lblGrandTotalTagihanKasir.setText(currencyFormatter.format(0));
        currentTotalTagihanKasir = 0.0;


        if (fullReset && comboNomorMejaKasir.getItemCount() > 0) {
            populateKasirForPayment((String)comboNomorMejaKasir.getItemAt(0));
        } else if (!fullReset && comboNomorMejaKasir.getSelectedItem() != null) {
            populateKasirForPayment((String)comboNomorMejaKasir.getSelectedItem());
        }
        if (fullReset) txtWaktuSelesaiKasir.requestFocus();
    }

    // --- INNER CLASSES START HERE ---

    // Inner class untuk Panel Pemesanan Meja (OrderMejaPanel)
    class OrderMejaPanel extends JPanel {
        private JComboBox<String> comboNomorMejaOrderMeja;
        private JTextField txtNamaPelangganOrderMeja;
        private JTextField txtWaktuMulaiOrderMeja;
        private JButton btnSetWaktuMulaiOrderMejaSekarang;
        private JComboBox<PackageMenuItem> comboInitialPackageOrderMeja;
        private JButton btnMulaiSesi;            
        private String lastSelectedStartedTable = null;
        private TableStatusPanel tableStatusPanelRefInner; 

        public OrderMejaPanel() { 
            setLayout(new BorderLayout(10, 10));
            setBorder(BorderFactory.createTitledBorder("Pesan Meja & Paket Awal"));

            JPanel panelInputOrderMeja = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(10, 10, 10, 10); 
            gbc.anchor = GridBagConstraints.WEST;
            int yOrder = 0;

            gbc.gridx = 0; gbc.gridy = yOrder; panelInputOrderMeja.add(new JLabel("Pilih Meja:"), gbc);
            comboNomorMejaOrderMeja = new JComboBox<>(tableStatusMap.keySet().toArray(new String[0]));
            gbc.gridx = 1; gbc.gridwidth = 2; panelInputOrderMeja.add(comboNomorMejaOrderMeja, gbc); yOrder++;

            gbc.gridx = 0; gbc.gridy = yOrder; gbc.gridwidth = 1; panelInputOrderMeja.add(new JLabel("Nama Pelanggan:"), gbc);
            txtNamaPelangganOrderMeja = new JTextField(25); 
            gbc.gridx = 1; gbc.gridwidth = 2; panelInputOrderMeja.add(txtNamaPelangganOrderMeja, gbc); yOrder++;

            gbc.gridx = 0; gbc.gridy = yOrder; gbc.gridwidth = 1; panelInputOrderMeja.add(new JLabel("Waktu Mulai (HH:mm):"), gbc);
            txtWaktuMulaiOrderMeja = new JTextField(12); 
            gbc.gridx = 1; gbc.gridwidth = 1; panelInputOrderMeja.add(txtWaktuMulaiOrderMeja, gbc);
            btnSetWaktuMulaiOrderMejaSekarang = new JButton("Sekarang");
            gbc.gridx = 2; gbc.gridwidth = 1; panelInputOrderMeja.add(btnSetWaktuMulaiOrderMejaSekarang, gbc); yOrder++;
            
            gbc.gridx = 0; gbc.gridy = yOrder; gbc.gridwidth = 1; panelInputOrderMeja.add(new JLabel("Pilih Paket Awal (Opsional):"), gbc);
            PackageMenuItem noPackageOption = new PackageMenuItem("-- Tidak Ada Paket Awal --", 0, "Tidak memilih paket awal", 0, false);
            List<PackageMenuItem> displayPackageList = new ArrayList<>();
            displayPackageList.add(noPackageOption); 
            displayPackageList.addAll(packageMenuItemsList);  
            comboInitialPackageOrderMeja = new JComboBox<>(displayPackageList.toArray(new PackageMenuItem[0]));
            comboInitialPackageOrderMeja.setRenderer(new DefaultListCellRenderer() { 
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof PackageMenuItem) {
                        PackageMenuItem item = (PackageMenuItem) value;
                        setText(item.toString()); 
                        setToolTipText(item.getFullDescription()); 
                    }
                    return this;
                }
            });
            gbc.gridx = 1; gbc.gridwidth = 2; panelInputOrderMeja.add(comboInitialPackageOrderMeja, gbc); yOrder++;
            
            add(panelInputOrderMeja, BorderLayout.NORTH);

            btnMulaiSesi = new JButton("Mulai Sesi di Meja");
            JPanel panelTombolOrderMeja = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20)); 
            panelTombolOrderMeja.add(btnMulaiSesi);
            add(panelTombolOrderMeja, BorderLayout.CENTER); 

            btnSetWaktuMulaiOrderMejaSekarang.addActionListener(e -> txtWaktuMulaiOrderMeja.setText(LocalTime.now().format(BilliardKasirApp.timeFormatter)));
            btnMulaiSesi.addActionListener(e -> prosesMulaiSesi());
            
            comboNomorMejaOrderMeja.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String selectedTable = (String) comboNomorMejaOrderMeja.getSelectedItem();
                    if (selectedTable != null) {
                        TableInfo tableInfo = tableStatusMap.get(selectedTable);
                        boolean isOccupied = tableInfo != null && tableInfo.isOccupied();
                        btnMulaiSesi.setEnabled(!isOccupied);
                        txtNamaPelangganOrderMeja.setEditable(!isOccupied);
                        txtWaktuMulaiOrderMeja.setEditable(!isOccupied);
                        btnSetWaktuMulaiOrderMejaSekarang.setEnabled(!isOccupied);
                        comboInitialPackageOrderMeja.setEnabled(!isOccupied);
                        if (isOccupied) {
                            JOptionPane.showMessageDialog(this, "Meja " + selectedTable + " sedang digunakan!", "Meja Terpakai", JOptionPane.WARNING_MESSAGE);
                            txtNamaPelangganOrderMeja.setText(tableInfo.getCustomerName()); 
                            txtWaktuMulaiOrderMeja.setText(tableInfo.getStartTime().format(BilliardKasirApp.timeFormatter));
                             if (!tableInfo.getOrderedPackages().isEmpty()) {
                                PackageMenuItem currentPkg = tableInfo.getOrderedPackages().get(0); 
                                comboInitialPackageOrderMeja.setSelectedItem(currentPkg);
                            } else {
                                comboInitialPackageOrderMeja.setSelectedIndex(0);
                            }
                        } else {
                            resetOrderMejaFormFields(); 
                        }
                    }
                }
            });
            if (comboNomorMejaOrderMeja.getItemCount() > 0) {
                 String firstTable = (String) comboNomorMejaOrderMeja.getItemAt(0);
                 TableInfo firstTableInfo = tableStatusMap.get(firstTable);
                 if (firstTableInfo != null && firstTableInfo.isOccupied()) {
                     btnMulaiSesi.setEnabled(false);
                     txtNamaPelangganOrderMeja.setEditable(false);
                     txtWaktuMulaiOrderMeja.setEditable(false);
                     btnSetWaktuMulaiOrderMejaSekarang.setEnabled(false);
                     comboInitialPackageOrderMeja.setEnabled(false);
                 }
            }
        }
        
        public void setTableStatusPanelRef(TableStatusPanel tspRef) { this.tableStatusPanelRefInner = tspRef; }
        public String getLastSelectedStartedTable() { return lastSelectedStartedTable; }

        public void refreshTableComboBox() { 
            String currentSelection = (String) comboNomorMejaOrderMeja.getSelectedItem();
            comboNomorMejaOrderMeja.removeAllItems(); 
            for (String tableName : tableStatusMap.keySet().stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList())) {
                comboNomorMejaOrderMeja.addItem(tableName);
            }
            if (currentSelection != null && Arrays.asList(tableStatusMap.keySet().toArray()).contains(currentSelection)) {
                comboNomorMejaOrderMeja.setSelectedItem(currentSelection);
            } else if (comboNomorMejaOrderMeja.getItemCount() > 0) {
                comboNomorMejaOrderMeja.setSelectedIndex(0); 
            }
            
            if (comboNomorMejaOrderMeja.getSelectedItem() != null) {
                if (comboNomorMejaOrderMeja.getItemListeners().length > 0) {
                    comboNomorMejaOrderMeja.getItemListeners()[0].itemStateChanged(
                        new ItemEvent(comboNomorMejaOrderMeja, ItemEvent.ITEM_STATE_CHANGED, 
                                      comboNomorMejaOrderMeja.getSelectedItem(), ItemEvent.SELECTED)
                    );
                }
            }
        }

        private void prosesMulaiSesi() {
            String selectedTable = (String) comboNomorMejaOrderMeja.getSelectedItem();
            if (selectedTable == null) {
                JOptionPane.showMessageDialog(this, "Silakan pilih meja.", "Input Kurang", JOptionPane.WARNING_MESSAGE); return;
            }
            TableInfo currentTable = tableStatusMap.get(selectedTable);
            if (currentTable.isOccupied()) {
                JOptionPane.showMessageDialog(this, "Meja " + selectedTable + " sudah terpakai!", "Error", JOptionPane.ERROR_MESSAGE); return;
            }
            String customerName = txtNamaPelangganOrderMeja.getText().trim();
            if (customerName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nama pelanggan kosong!", "Input Kurang", JOptionPane.WARNING_MESSAGE); txtNamaPelangganOrderMeja.requestFocus(); return;
            }
            if (txtWaktuMulaiOrderMeja.getText().trim().isEmpty()){
                JOptionPane.showMessageDialog(this, "Waktu mulai kosong!", "Input Kurang", JOptionPane.WARNING_MESSAGE); txtWaktuMulaiOrderMeja.requestFocus(); return;
            }

            try {
                LocalTime startTime = LocalTime.parse(txtWaktuMulaiOrderMeja.getText(), BilliardKasirApp.timeFormatter);
                PackageMenuItem initialPackage = (PackageMenuItem) comboInitialPackageOrderMeja.getSelectedItem();
                if (initialPackage != null && initialPackage.price == 0 && initialPackage.name.contains("Tidak Ada")) {
                    initialPackage = null; 
                }
                currentTable.startSession(customerName, startTime, initialPackage);
                if (tableStatusPanelRefInner != null) tableStatusPanelRefInner.refreshStatusDisplay();
                BilliardKasirApp.this.populateKasirForPayment(selectedTable); 
                lastSelectedStartedTable = selectedTable; 
                String packageInfo = (initialPackage != null) ? "\nPaket Awal: " + initialPackage.name : "\nTanpa Paket Awal";
                JOptionPane.showMessageDialog(this, "Sesi untuk meja " + selectedTable + " oleh " + customerName + " dimulai." + packageInfo, "Sesi Dimulai", JOptionPane.INFORMATION_MESSAGE);
                resetOrderMejaForm();
                if (this.getParent() != null && this.getParent().getParent() instanceof JTabbedPane) {
                     ((JTabbedPane)this.getParent().getParent()).setSelectedIndex(1); 
                }
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this, "Format Waktu Mulai salah! Gunakan HH:mm", "Error Input Waktu", JOptionPane.ERROR_MESSAGE);
                txtWaktuMulaiOrderMeja.requestFocus();
            }
        }
        
        private void resetOrderMejaFormFields() {
            txtNamaPelangganOrderMeja.setText("");
            txtWaktuMulaiOrderMeja.setText("");
            if (comboInitialPackageOrderMeja.getItemCount() > 0) comboInitialPackageOrderMeja.setSelectedIndex(0);
            lastSelectedStartedTable = null;
        }
        
        private void resetOrderMejaForm() {
            if (comboNomorMejaOrderMeja.getItemCount() > 0) comboNomorMejaOrderMeja.setSelectedIndex(0);
            if (comboNomorMejaOrderMeja.getSelectedItem() != null) {
                if (comboNomorMejaOrderMeja.getItemListeners().length > 0) { 
                     comboNomorMejaOrderMeja.getItemListeners()[0].itemStateChanged(
                        new ItemEvent(comboNomorMejaOrderMeja, ItemEvent.ITEM_STATE_CHANGED, 
                                      comboNomorMejaOrderMeja.getSelectedItem(), ItemEvent.SELECTED)
                    );
                }
            } else { 
                resetOrderMejaFormFields();
                btnMulaiSesi.setEnabled(false);
                txtNamaPelangganOrderMeja.setEditable(false);
                txtWaktuMulaiOrderMeja.setEditable(false);
                btnSetWaktuMulaiOrderMejaSekarang.setEnabled(false);
                comboInitialPackageOrderMeja.setEnabled(false);
            }
            txtNamaPelangganOrderMeja.requestFocus();
        }
    }

    // Inner class untuk Panel Pemesanan Kafe (F&B Tambahan)
    class OrderKafePanel extends JPanel {
        private JComboBox<String> comboNomorMejaAktifKafe;
        private JLabel lblInfoPelangganKafe;
        private JPanel panelFnBItemsKafe;                 
        private JComboBox<PackageMenuItem> comboPackageKafe; 
        private JTextArea areaRingkasanPesananKafe;       
        private JLabel lblGrandTotalPesananKafe;          
        private JButton btnTambahPesananKeMeja;            
        private List<JSpinner> fnbQuantitySpinnersKafe = new ArrayList<>(); 
        private List<JCheckBox> fnbCheckBoxesKafe = new ArrayList<>();     
        private String lastSelectedActiveTable = null;
        private TableStatusPanel tableStatusPanelRefInner;

        public OrderKafePanel() { // Konstruktor disederhanakan
            setLayout(new BorderLayout(10, 10));
            setBorder(BorderFactory.createTitledBorder("Tambah Pesanan F&B ke Meja Aktif"));

            JPanel panelInputOrderKafeTop = new JPanel(new GridBagLayout());
            GridBagConstraints gbcTop = new GridBagConstraints();
            gbcTop.fill = GridBagConstraints.HORIZONTAL;
            gbcTop.insets = new Insets(5, 5, 5, 5);
            gbcTop.anchor = GridBagConstraints.WEST;
            int yOrderKafe = 0;

            gbcTop.gridx = 0; gbcTop.gridy = yOrderKafe; panelInputOrderKafeTop.add(new JLabel("Pilih Meja Aktif:"), gbcTop);
            comboNomorMejaAktifKafe = new JComboBox<>(); 
            gbcTop.gridx = 1; gbcTop.gridwidth = 2; panelInputOrderKafeTop.add(comboNomorMejaAktifKafe, gbcTop); yOrderKafe++;

            lblInfoPelangganKafe = new JLabel("Pelanggan: - | Mulai: -");
            lblInfoPelangganKafe.setFont(new Font("Arial", Font.ITALIC, 12));
            gbcTop.gridx = 0; gbcTop.gridy = yOrderKafe; gbcTop.gridwidth = 3; panelInputOrderKafeTop.add(lblInfoPelangganKafe, gbcTop); yOrderKafe++;
            
            add(panelInputOrderKafeTop, BorderLayout.NORTH);

            JPanel panelSelectionItemsKafe = new JPanel();
            panelSelectionItemsKafe.setLayout(new BoxLayout(panelSelectionItemsKafe, BoxLayout.Y_AXIS)); 

            panelFnBItemsKafe = new JPanel();
            int fnbCols = menuItemsList.size() > 4 ? 2 : 1; 
            int fnbRows = (int) Math.ceil((double) menuItemsList.size() / fnbCols);
            if (fnbRows == 0) fnbRows = 1;
            panelFnBItemsKafe.setLayout(new GridLayout(fnbRows, fnbCols * 2, 10, 5)); 
            panelFnBItemsKafe.setBorder(BorderFactory.createTitledBorder("Pilih Makanan & Minuman Individual"));
            for (MenuItem item : menuItemsList) {
                JCheckBox chkItem = new JCheckBox(item.name); 
                fnbCheckBoxesKafe.add(chkItem);
                panelFnBItemsKafe.add(chkItem);
                SpinnerModel spinnerModel = new SpinnerNumberModel(1, 1, 100, 1);
                JSpinner spnQuantity = new JSpinner(spinnerModel);
                spnQuantity.setEnabled(false); 
                ((JSpinner.DefaultEditor) spnQuantity.getEditor()).getTextField().setColumns(3);
                fnbQuantitySpinnersKafe.add(spnQuantity);
                panelFnBItemsKafe.add(spnQuantity);
                chkItem.addActionListener(e -> {
                    spnQuantity.setEnabled(chkItem.isSelected());
                    if (!chkItem.isSelected()) spnQuantity.setValue(1); 
                    updateKafeOrderSummary();
                });
                spnQuantity.addChangeListener(e -> updateKafeOrderSummary());
            }
            panelSelectionItemsKafe.add(new JScrollPane(panelFnBItemsKafe));

            JPanel panelPackageKafe = new JPanel(new FlowLayout(FlowLayout.LEFT));
            panelPackageKafe.setBorder(BorderFactory.createTitledBorder("Pilih Paket F&B Tambahan (Opsional)"));
            PackageMenuItem noPackageKafeOption = new PackageMenuItem("-- Tidak Ada Paket Tambahan --", 0, "Tidak memilih paket tambahan", 0, false);
            List<PackageMenuItem> displayPackageKafeList = new ArrayList<>();
            displayPackageKafeList.add(noPackageKafeOption);
            packageMenuItemsList.stream().filter(p -> !p.isTimeBasedRentalPackage).forEach(displayPackageKafeList::add);
            comboPackageKafe = new JComboBox<>(displayPackageKafeList.toArray(new PackageMenuItem[0]));
            comboPackageKafe.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof PackageMenuItem) {
                        PackageMenuItem item = (PackageMenuItem) value;
                        setText(item.toString()); 
                        setToolTipText(item.description);
                    }
                    return this;
                }
            });
            panelPackageKafe.add(comboPackageKafe);
            panelSelectionItemsKafe.add(panelPackageKafe);
            
            JPanel panelCenterOrderKafe = new JPanel(new BorderLayout(10,10));
            panelCenterOrderKafe.add(panelSelectionItemsKafe, BorderLayout.NORTH); 

            areaRingkasanPesananKafe = new JTextArea(8, 30); 
            areaRingkasanPesananKafe.setEditable(false);
            areaRingkasanPesananKafe.setFont(new Font("Monospaced", Font.PLAIN, 12));
            JScrollPane scrollRingkasanKafe = new JScrollPane(areaRingkasanPesananKafe);
            scrollRingkasanKafe.setBorder(BorderFactory.createTitledBorder("Ringkasan Pesanan Tambahan"));
            panelCenterOrderKafe.add(scrollRingkasanKafe, BorderLayout.CENTER);
            
            lblGrandTotalPesananKafe = new JLabel("Total Tambahan: " + BilliardKasirApp.currencyFormatter.format(0));
            lblGrandTotalPesananKafe.setFont(new Font("Arial", Font.BOLD, 16)); 
            JPanel panelTotalPesananKafe = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            panelTotalPesananKafe.add(lblGrandTotalPesananKafe);
            panelCenterOrderKafe.add(panelTotalPesananKafe, BorderLayout.SOUTH);

            add(panelCenterOrderKafe, BorderLayout.CENTER);

            btnTambahPesananKeMeja = new JButton("Tambah Pesanan ke Meja");
            JPanel panelTombolOrderKafe = new JPanel(new FlowLayout(FlowLayout.CENTER));
            panelTombolOrderKafe.add(btnTambahPesananKeMeja);
            add(panelTombolOrderKafe, BorderLayout.SOUTH);

            btnTambahPesananKeMeja.addActionListener(e -> prosesTambahPesananKeMeja());
            comboPackageKafe.addActionListener(e -> updateKafeOrderSummary());
            
            comboNomorMejaAktifKafe.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String selectedTable = (String) comboNomorMejaAktifKafe.getSelectedItem();
                    if (selectedTable != null) {
                        TableInfo tableInfo = tableStatusMap.get(selectedTable);
                        if (tableInfo != null && tableInfo.isOccupied()) {
                            lblInfoPelangganKafe.setText("Pelanggan: " + tableInfo.getCustomerName() + " | Mulai: " + tableInfo.getStartTime().format(BilliardKasirApp.timeFormatter));
                            enableKafeOrderFields(true);
                            lastSelectedActiveTable = selectedTable;
                        } else { 
                            lblInfoPelangganKafe.setText("Pelanggan: - | Mulai: -");
                            enableKafeOrderFields(false);
                            lastSelectedActiveTable = null;
                        }
                        resetKafeOrderSelections(); 
                    } else {
                         lblInfoPelangganKafe.setText("Pelanggan: - | Mulai: -");
                         enableKafeOrderFields(false);
                         lastSelectedActiveTable = null;
                         resetKafeOrderSelections();
                    }
                }
            });
            enableKafeOrderFields(false); 
        }
        
        private void enableKafeOrderFields(boolean enable) {
            for (JCheckBox chk : fnbCheckBoxesKafe) chk.setEnabled(enable);
            for (JSpinner spn : fnbQuantitySpinnersKafe) spn.setEnabled(false); 
            comboPackageKafe.setEnabled(enable);
            btnTambahPesananKeMeja.setEnabled(enable);
        }
        
        public void setTableStatusPanelRef(TableStatusPanel tspRef) { this.tableStatusPanelRefInner = tspRef; }
        public String getLastSelectedActiveTable() { return lastSelectedActiveTable; }

        public void refreshActiveTableComboBox() { 
            String currentSelection = (String) comboNomorMejaAktifKafe.getSelectedItem();
            comboNomorMejaAktifKafe.removeAllItems(); 
            List<String> activeTables = tableStatusMap.entrySet().stream()
                                        .filter(entry -> entry.getValue().isOccupied())
                                        .map(Map.Entry::getKey)
                                        .sorted(Comparator.naturalOrder())
                                        .collect(Collectors.toList());
            for (String tableName : activeTables) {
                comboNomorMejaAktifKafe.addItem(tableName);
            }

            if (currentSelection != null && activeTables.contains(currentSelection)) {
                comboNomorMejaAktifKafe.setSelectedItem(currentSelection);
            } else if (!activeTables.isEmpty()) {
                comboNomorMejaAktifKafe.setSelectedIndex(0); 
            } else { 
                lblInfoPelangganKafe.setText("Pelanggan: - | Mulai: -");
                enableKafeOrderFields(false);
            }
            if (comboNomorMejaAktifKafe.getSelectedItem() != null && comboNomorMejaAktifKafe.getItemListeners().length > 0) {
                comboNomorMejaAktifKafe.getItemListeners()[0].itemStateChanged(
                    new ItemEvent(comboNomorMejaAktifKafe, ItemEvent.ITEM_STATE_CHANGED, 
                                  comboNomorMejaAktifKafe.getSelectedItem(), ItemEvent.SELECTED)
                );
            }
            resetKafeOrderSelections();
        }

        private void resetKafeOrderSelections() {
            for(JCheckBox chk : fnbCheckBoxesKafe) chk.setSelected(false);
            for(JSpinner spn : fnbQuantitySpinnersKafe) { spn.setValue(1); spn.setEnabled(false); }
            if (comboPackageKafe.getItemCount() > 0) comboPackageKafe.setSelectedIndex(0); 
            updateKafeOrderSummary();
        }

        private void updateKafeOrderSummary() {
            StringBuilder summary = new StringBuilder();
            double currentIndividualFnbTotal = 0;
            double currentPackagesTotal = 0;

            summary.append("--- F&B Individual Tambahan ---\n");
            boolean fnbSelected = false;
            for (int i = 0; i < menuItemsList.size(); i++) {
                if (fnbCheckBoxesKafe.get(i).isSelected()) {
                    MenuItem menuItem = menuItemsList.get(i);
                    int quantity = (int) fnbQuantitySpinnersKafe.get(i).getValue();
                    if (quantity > 0) {
                        OrderItem orderItem = new OrderItem(menuItem, quantity);
                        summary.append(orderItem.toString().split(" \\(")[0]) 
                               .append(" = ")
                               .append(BilliardKasirApp.currencyFormatter.format(orderItem.getTotalPrice()))
                               .append("\n");
                        currentIndividualFnbTotal += orderItem.getTotalPrice();
                        fnbSelected = true;
                    }
                }
            }
            if (!fnbSelected) summary.append("(Tidak ada)\n");

            summary.append("\n--- Paket F&B Tambahan ---\n");
            PackageMenuItem selectedPackage = (PackageMenuItem) comboPackageKafe.getSelectedItem();
            if (selectedPackage != null && selectedPackage.price > 0) { 
                summary.append(selectedPackage.name)
                       .append(" = ")
                       .append(BilliardKasirApp.currencyFormatter.format(selectedPackage.price))
                       .append("\n");
                currentPackagesTotal = selectedPackage.price;
            } else {
                summary.append("(Tidak ada)\n");
            }
            
            areaRingkasanPesananKafe.setText(summary.toString());
            lblGrandTotalPesananKafe.setText("Total Tambahan: " + BilliardKasirApp.currencyFormatter.format(currentIndividualFnbTotal + currentPackagesTotal));
        }

        private void prosesTambahPesananKeMeja() {
            String selectedTable = (String) comboNomorMejaAktifKafe.getSelectedItem();
            if (selectedTable == null) {
                JOptionPane.showMessageDialog(this, "Silakan pilih meja aktif.", "Error", JOptionPane.WARNING_MESSAGE); return;
            }
            TableInfo currentTable = tableStatusMap.get(selectedTable);
            if (!currentTable.isOccupied()) { 
                JOptionPane.showMessageDialog(this, "Meja " + selectedTable + " tidak sedang digunakan!", "Error", JOptionPane.ERROR_MESSAGE); return;
            }
                
            List<OrderItem> newIndividualItemsForTable = new ArrayList<>();
            double newIndividualFnbTotal = 0;
            for (int i = 0; i < menuItemsList.size(); i++) {
                if (fnbCheckBoxesKafe.get(i).isSelected()) {
                    MenuItem menuItem = menuItemsList.get(i);
                    int quantity = (int) fnbQuantitySpinnersKafe.get(i).getValue();
                    if (quantity > 0) {
                        OrderItem orderItem = new OrderItem(menuItem, quantity);
                        newIndividualItemsForTable.add(orderItem);
                        newIndividualFnbTotal += orderItem.getTotalPrice();
                    }
                }
            }

            List<PackageMenuItem> newPackagesForTable = new ArrayList<>();
            double newPackagesTotal = 0;
            PackageMenuItem selectedPackage = (PackageMenuItem) comboPackageKafe.getSelectedItem();
            if (selectedPackage != null && selectedPackage.price > 0) { 
                newPackagesForTable.add(selectedPackage);
                newPackagesTotal = selectedPackage.price;
            }
            
            if (newIndividualItemsForTable.isEmpty() && newPackagesForTable.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tidak ada item tambahan yang dipilih.", "Informasi", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            currentTable.addCafeOrder(newIndividualItemsForTable, newPackagesForTable);
            if (tableStatusPanelRefInner != null) tableStatusPanelRefInner.refreshStatusDisplay();
            BilliardKasirApp.this.populateKasirForPayment(selectedTable); 

            JOptionPane.showMessageDialog(this, "Pesanan tambahan untuk meja " + selectedTable + " berhasil ditambahkan.", "Pesanan Ditambahkan", JOptionPane.INFORMATION_MESSAGE);
            
            resetKafeOrderSelections(); 
        }
    }

    // Inner class untuk Panel Status Meja
    class TableStatusPanel extends JPanel {
        private JPanel gridPanel;
        private Map<String, JTextArea> tableStatusAreas = new HashMap<>();
        private Map<String, JPanel> tableCellPanelsMap = new HashMap<>();
        private OrderMejaPanel orderMejaPanelRefInner; 
        private OrderKafePanel orderKafePanelRefInner;

        public TableStatusPanel() { // Konstruktor disederhanakan
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createTitledBorder("Status Meja Billiard"));

            int numberOfTables = tableStatusMap.size();
            int cols = 3; 
            int rows = (int) Math.ceil((double) numberOfTables / cols);
            if (rows == 0) rows = 1;

            gridPanel = new JPanel(new GridLayout(rows, cols, 15, 15)); 
            gridPanel.setBorder(new EmptyBorder(15,15,15,15)); 

            initializeTableCells();
            add(new JScrollPane(gridPanel), BorderLayout.CENTER); 
            refreshStatusDisplay(); 
        }
        
        public void setOrderPanelRefs(OrderMejaPanel omRef, OrderKafePanel okRef){
            this.orderMejaPanelRefInner = omRef;
            this.orderKafePanelRefInner = okRef;
        }

        private void initializeTableCells() {
            gridPanel.removeAll();
            tableStatusAreas.clear();
            tableCellPanelsMap.clear();

            List<String> sortedTableNames = tableStatusMap.keySet().stream()
                                            .sorted(Comparator.naturalOrder())
                                            .collect(Collectors.toList());

            for (String tableName : sortedTableNames) {
                TableInfo tableInfo = tableStatusMap.get(tableName);
                JPanel cellPanel = new JPanel(new BorderLayout(5,5));
                cellPanel.setBorder(BorderFactory.createEtchedBorder());
                cellPanel.setPreferredSize(new Dimension(230, 160)); 

                JLabel nameLabel = new JLabel(tableInfo.getTableName(), SwingConstants.CENTER);
                nameLabel.setFont(new Font("Arial", Font.BOLD, 20)); 

                JTextArea statusArea = new JTextArea(6, 20); 
                statusArea.setEditable(false);
                statusArea.setLineWrap(true);
                statusArea.setWrapStyleWord(true);
                statusArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
                statusArea.setOpaque(false); 
                statusArea.setBorder(new EmptyBorder(5,8,5,8)); 

                cellPanel.add(nameLabel, BorderLayout.NORTH);
                cellPanel.add(new JScrollPane(statusArea), BorderLayout.CENTER); 

                gridPanel.add(cellPanel);
                tableStatusAreas.put(tableInfo.getTableName(), statusArea);
                tableCellPanelsMap.put(tableInfo.getTableName(), cellPanel);
            }
            
            int totalCells = gridPanel.getComponentCount();
            int desiredCells = ((GridLayout)gridPanel.getLayout()).getRows() * ((GridLayout)gridPanel.getLayout()).getColumns();
            for(int i = totalCells; i < desiredCells; i++) {
                JPanel placeholder = new JPanel();
                placeholder.setPreferredSize(new Dimension(230,160)); 
                placeholder.setOpaque(false); 
                gridPanel.add(placeholder);
            }
            gridPanel.revalidate(); 
            gridPanel.repaint();
        }

        public void refreshStatusDisplay() {
            for (Map.Entry<String, TableInfo> entry : tableStatusMap.entrySet()) {
                String tableName = entry.getKey();
                TableInfo tableInfo = entry.getValue();
                JTextArea statusArea = tableStatusAreas.get(tableName);
                JPanel cellPanel = tableCellPanelsMap.get(tableName);

                if (statusArea != null && cellPanel != null) {
                    if (tableInfo.isOccupied()) {
                        cellPanel.setBackground(new Color(255, 200, 200)); 
                        StringBuilder details = new StringBuilder();
                        details.append(String.format("Status: DIPAKAI\nOleh: %s\nMulai: %s",
                                tableInfo.getCustomerName() != null ? tableInfo.getCustomerName() : "-",
                                tableInfo.getStartTime() != null ? tableInfo.getStartTime().format(BilliardKasirApp.timeFormatter) : "-"));
                        double totalOrderValue = tableInfo.getIndividualFnbTotal() + tableInfo.getPackagesTotal();
                        if (totalOrderValue > 0) {
                            details.append("\nTotal Pesanan: ").append(BilliardKasirApp.currencyFormatter.format(totalOrderValue));
                        }
                        statusArea.setText(details.toString());
                    } else {
                        cellPanel.setBackground(new Color(200, 255, 200)); 
                        statusArea.setText("Status: TERSEDIA");
                    }
                }
            }
            gridPanel.revalidate();
            gridPanel.repaint();
            if (orderMejaPanelRefInner != null) { 
                orderMejaPanelRefInner.refreshTableComboBox();
            }
            if (orderKafePanelRefInner != null) {
                orderKafePanelRefInner.refreshActiveTableComboBox();
            }
        }
    }

    // Inner class untuk Panel Riwayat Pemesanan (Laporan)
    class HistoriPemesananPanel extends JPanel {
        private JTable historyTable;
        private DefaultTableModel tableModel;
        private JButton btnRefreshHistory, btnViewSelectedReceipts, btnSaveSelectedReceiptsText, btnPrintSelectedReceiptsToPrinter; 
        private JLabel lblDailyRevenueToday, lblWeeklyRevenue, lblMonthlyRevenue, lblSelectedDateRevenue;
        private JTextField txtSelectedDateForReport;
        private JButton btnShowDailyReportActions;


        public HistoriPemesananPanel() {
            setLayout(new BorderLayout(10,10));
            setBorder(BorderFactory.createTitledBorder("Laporan Transaksi"));

            // Panel Atas: Pendapatan & Refresh
            JPanel topActionPanel = new JPanel(new BorderLayout(10,5));
            JPanel revenueDisplayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
            lblDailyRevenueToday = new JLabel("Pendapatan Hari Ini: Rp 0");
            lblWeeklyRevenue = new JLabel("Pendapatan Minggu Ini: Rp 0");
            lblMonthlyRevenue = new JLabel("Pendapatan Bulan Ini: Rp 0");
            Font revenueFont = new Font("Arial", Font.BOLD, 13);
            lblDailyRevenueToday.setFont(revenueFont);
            lblWeeklyRevenue.setFont(revenueFont);
            lblMonthlyRevenue.setFont(revenueFont);
            revenueDisplayPanel.add(lblDailyRevenueToday);
            revenueDisplayPanel.add(new JSeparator(SwingConstants.VERTICAL));
            revenueDisplayPanel.add(lblWeeklyRevenue);
            revenueDisplayPanel.add(new JSeparator(SwingConstants.VERTICAL));
            revenueDisplayPanel.add(lblMonthlyRevenue);
            topActionPanel.add(revenueDisplayPanel, BorderLayout.CENTER);

            btnRefreshHistory = new JButton("Segarkan Semua Data");
            btnRefreshHistory.addActionListener(e -> loadHistory()); 
            JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            refreshPanel.add(btnRefreshHistory);
            topActionPanel.add(refreshPanel, BorderLayout.EAST);
            
            add(topActionPanel, BorderLayout.NORTH);


            // Panel Tengah: Tabel Riwayat
            String[] columnNames = {"ID Transaksi", "Meja", "Pelanggan", "Waktu Transaksi", "Total Tagihan", "Metode Bayar"};
            tableModel = new DefaultTableModel(columnNames, 0){
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false; 
                }
            };
            historyTable = new JTable(tableModel);
            historyTable.setFillsViewportHeight(true);
            historyTable.setAutoCreateRowSorter(true); 
            historyTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION); 
            
            historyTable.getColumnModel().getColumn(0).setPreferredWidth(80); 
            historyTable.getColumnModel().getColumn(1).setPreferredWidth(80); 
            historyTable.getColumnModel().getColumn(2).setPreferredWidth(150); 
            historyTable.getColumnModel().getColumn(3).setPreferredWidth(150); 
            historyTable.getColumnModel().getColumn(4).setPreferredWidth(120); 
            historyTable.getColumnModel().getColumn(5).setPreferredWidth(100); 

            JScrollPane scrollPane = new JScrollPane(historyTable);
            add(scrollPane, BorderLayout.CENTER);

            // Panel Bawah: Aksi untuk tabel dan laporan harian kustom
            JPanel bottomControlsPanel = new JPanel(new BorderLayout(20,5));
            
            JPanel dailyReportPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            dailyReportPanel.add(new JLabel("Laporan Harian Tgl (YYYY-MM-DD):"));
            txtSelectedDateForReport = new JTextField(10);
            txtSelectedDateForReport.setText(LocalDate.now().format(dateDbFormatter)); 
            btnShowDailyReportActions = new JButton("Proses Laporan Harian");
            lblSelectedDateRevenue = new JLabel("Total Hari Terpilih: Rp 0");
            lblSelectedDateRevenue.setFont(revenueFont);
            dailyReportPanel.add(txtSelectedDateForReport);
            dailyReportPanel.add(btnShowDailyReportActions);
            dailyReportPanel.add(new JSeparator(SwingConstants.VERTICAL));
            dailyReportPanel.add(lblSelectedDateRevenue);
            bottomControlsPanel.add(dailyReportPanel, BorderLayout.WEST);


            JPanel selectedActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            btnViewSelectedReceipts = new JButton("Lihat Struk Terpilih"); 
            btnSaveSelectedReceiptsText = new JButton("Simpan Struk Terpilih (TXT)");
            btnPrintSelectedReceiptsToPrinter = new JButton("Cetak Struk Terpilih ke Printer");
            
            btnViewSelectedReceipts.addActionListener(e -> processSelectedTransactions(false, false));
            btnSaveSelectedReceiptsText.addActionListener(e -> processSelectedTransactions(true, false));
            btnPrintSelectedReceiptsToPrinter.addActionListener(e -> processSelectedTransactions(false, true));
            btnShowDailyReportActions.addActionListener(e -> processDailyReportActions());

            selectedActionsPanel.add(btnViewSelectedReceipts);
            selectedActionsPanel.add(btnSaveSelectedReceiptsText);
            selectedActionsPanel.add(btnPrintSelectedReceiptsToPrinter);
            bottomControlsPanel.add(selectedActionsPanel, BorderLayout.EAST);

            add(bottomControlsPanel, BorderLayout.SOUTH);

            loadHistory(); 
        }

        public void loadHistory() {
            tableModel.setRowCount(0); 
            List<TransactionHistoryRecord> history = loadTransactionHistoryFromDB();
            for (TransactionHistoryRecord record : history) {
                tableModel.addRow(new Object[]{
                        record.transactionId,
                        record.tableName,
                        record.customerName,
                        record.transactionTime.format(BilliardKasirApp.dateTimeDisplayFormatter),
                        BilliardKasirApp.currencyFormatter.format(record.totalBill),
                        record.paymentMethod
                });
            }
            updateRevenueDisplay(); 
        }

        private void updateRevenueDisplay() {
            lblDailyRevenueToday.setText("Pendapatan Hari Ini: " + currencyFormatter.format(fetchRevenueForDate(LocalDate.now())));
            lblWeeklyRevenue.setText("Pendapatan Minggu Ini: " + currencyFormatter.format(fetchWeeklyRevenue()));
            lblMonthlyRevenue.setText("Pendapatan Bulan Ini: " + currencyFormatter.format(fetchMonthlyRevenue()));
        }

        private double fetchRevenueForDate(LocalDate date) {
            double dailyRevenue = 0;
            String sql = "SELECT SUM(total_bill) FROM transactions WHERE DATE(transaction_timestamp) = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDate(1, Date.valueOf(date));
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    dailyRevenue = rs.getDouble(1);
                }
                 DBUtil.close(null, null, rs);
            } catch (SQLException e) {
                e.printStackTrace(); 
            }
            return dailyRevenue;
        }


        private double fetchWeeklyRevenue() {
            double weeklyRevenue = 0;
            String sql = "SELECT SUM(total_bill) FROM transactions WHERE YEARWEEK(transaction_timestamp, 1) = YEARWEEK(CURDATE(), 1)";
            try (Connection conn = DBUtil.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    weeklyRevenue = rs.getDouble(1);
                }
            } catch (SQLException e) {
                e.printStackTrace(); 
            }
            return weeklyRevenue;
        }

        private double fetchMonthlyRevenue() {
            double monthlyRevenue = 0;
            String sql = "SELECT SUM(total_bill) FROM transactions WHERE YEAR(transaction_timestamp) = YEAR(CURDATE()) AND MONTH(transaction_timestamp) = MONTH(CURDATE())";
            try (Connection conn = DBUtil.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    monthlyRevenue = rs.getDouble(1);
                }
            } catch (SQLException e) {
                e.printStackTrace(); 
            }
            return monthlyRevenue;
        }

        private String generateReceiptContent(TransactionHistoryRecord record) {
            if (record == null) return "Data transaksi tidak ditemukan.";

            StringBuilder strukText = new StringBuilder();
            // Maksimal karakter per baris (kurang lebih untuk 58mm)
            int lineLength = 32; 
            String separator = "--------------------------------\n"; // Separator 32 karakter

            strukText.append("================================\n"); // Sesuaikan
            strukText.append(centerString("STRUK PEMBAYARAN", lineLength)).append("\n");
            strukText.append(centerString(BilliardKasirApp.NAMA_TEMPAT_BILLIARD, lineLength)).append("\n");
            strukText.append("================================\n");
            strukText.append(String.format("%-" + (lineLength - 15) + "s: %s\n", "No. Transaksi", record.transactionId));
            strukText.append(String.format("%-" + (lineLength - 15) + "s: %s\n", "Waktu", record.transactionTime.format(BilliardKasirApp.dateTimeDisplayFormatter)));
            strukText.append(String.format("%-" + (lineLength - 15) + "s: %s\n", "Meja", record.tableName));
            strukText.append(String.format("%-" + (lineLength - 15) + "s: %s\n", "Pelanggan", record.customerName != null ? record.customerName : "-"));

            if (record.startTime != null && record.endTime != null) {
                strukText.append(String.format("%-" + (lineLength - 15) + "s: %s\n", "Mulai", record.startTime.format(BilliardKasirApp.timeFormatter)));
                strukText.append(String.format("%-" + (lineLength - 15) + "s: %s\n", "Selesai", record.endTime.format(BilliardKasirApp.timeFormatter)));
                strukText.append(String.format("%-" + (lineLength - 15) + "s: %d m\n", "Durasi", record.totalPlayDurationMinutes));
            }
            strukText.append(separator);

            strukText.append(String.format("%-" + (lineLength - 15) + "s: %s\n", "Sewa Meja", BilliardKasirApp.currencyFormatter.format(record.rentalCost)));

            if (record.items != null && !record.items.isEmpty()) {
                strukText.append("Pesanan Tambahan:\n");
                for (Map<String, Object> item : record.items) {
                    String itemName = (String) item.get("name");
                    String itemType = (String) item.get("type");
                    int qty = (int) item.get("qty");
                    double totalPrice = (double) item.get("total_price");
                    String formattedItem;
                    if ("FNB".equals(itemType)) {
                        formattedItem = String.format(" %d x %-12.12s: %s", qty, itemName, BilliardKasirApp.currencyFormatter.format(totalPrice));
                    } else { // PAKET
                        formattedItem = String.format(" Paket: %-10.10s: %s", itemName, BilliardKasirApp.currencyFormatter.format(totalPrice));
                    }
                    strukText.append(wrapText(formattedItem, lineLength)).append("\n");
                }
            }
            strukText.append(String.format("%-" + (lineLength-18) + "s: %s\n", "Total F&B Indv.", BilliardKasirApp.currencyFormatter.format(record.individualFnbCost)));
            strukText.append(String.format("%-" + (lineLength-18) + "s: %s\n", "Total Paket", BilliardKasirApp.currencyFormatter.format(record.packageCost)));
            strukText.append(String.format("%-" + (lineLength-18) + "s: %s\n", "Biaya Lain", BilliardKasirApp.currencyFormatter.format(record.additionalCost)));
            strukText.append(separator);
            double subTotalStruk = record.rentalCost + record.individualFnbCost + record.packageCost + record.additionalCost;
            strukText.append(String.format("%-" + (lineLength-10) + "s: %s\n", "Subtotal", BilliardKasirApp.currencyFormatter.format(subTotalStruk)));
            
            if (record.memberDiscountAmount > 0) strukText.append(String.format("%-" + (lineLength-18) + "s: %s\n", "Diskon Member", BilliardKasirApp.currencyFormatter.format(record.memberDiscountAmount)));
            if (record.manualDiscountAmount > 0) strukText.append(String.format("%-" + (lineLength-18) + "s: %s\n", "Diskon Manual", BilliardKasirApp.currencyFormatter.format(record.manualDiscountAmount)));
            double totalDiskonStruk = record.memberDiscountAmount + record.manualDiscountAmount;
            if (totalDiskonStruk > 0) strukText.append(String.format("%-" + (lineLength-18) + "s: %s\n", "Total Diskon", BilliardKasirApp.currencyFormatter.format(totalDiskonStruk)));
            
            strukText.append("================================\n"); // Sesuaikan
            strukText.append(String.format("%-" + (lineLength-18) + "s: %s\n", "TOTAL TAGIHAN", BilliardKasirApp.currencyFormatter.format(record.totalBill)));
            strukText.append("--------------------------------\n");
            strukText.append(String.format("%-" + (lineLength-18) + "s: %s\n", "Metode Bayar", record.paymentMethod));
            strukText.append(String.format("%-" + (lineLength-18) + "s: %s\n", "Dibayar", BilliardKasirApp.currencyFormatter.format(record.amountPaid)));
            strukText.append(String.format("%-" + (lineLength-18) + "s: %s\n", "Kembalian", BilliardKasirApp.currencyFormatter.format(record.changeGiven)));
            strukText.append("================================\n"); // Sesuaikan
            strukText.append(centerString("Terima Kasih!", lineLength)).append("\n");
            strukText.append(centerString(NAMA_TEMPAT_BILLIARD, lineLength)).append("\n");
            return strukText.toString();
        }

        // Helper untuk memusatkan teks
        private String centerString(String text, int lineWidth) {
            if (text.length() >= lineWidth) {
                return text;
            }
            int padding = (lineWidth - text.length()) / 2;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < padding; i++) sb.append(" ");
            sb.append(text);
            while (sb.length() < lineWidth) sb.append(" ");
            return sb.toString();
        }
        
        // Helper untuk wrap teks jika melebihi lebar
        private String wrapText(String text, int lineWidth) {
            if (text.length() <= lineWidth) {
                return text;
            }
            StringBuilder wrappedText = new StringBuilder();
            int lastWrap = 0;
            while (lastWrap < text.length()) {
                int MaintTo = Math.min(lastWrap + lineWidth, text.length());
                wrappedText.append(text.substring(lastWrap, MaintTo));
                if (MaintTo < text.length()) {
                    wrappedText.append("\n");
                }
                lastWrap = MaintTo;
            }
            return wrappedText.toString();
        }
        
        // Metode yang digabungkan untuk melihat, menyimpan, atau mencetak struk terpilih
        private void processSelectedTransactions(boolean saveToFile, boolean printToPhysicalPrinter) {
            int[] selectedRows = historyTable.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(this, "Pilih satu atau lebih transaksi dari tabel.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            StringBuilder combinedReceiptContent = new StringBuilder();
            List<TransactionHistoryRecord> recordsToProcess = new ArrayList<>();

            for (int row : selectedRows) {
                int modelRow = historyTable.convertRowIndexToModel(row);
                int transactionId = (int) tableModel.getValueAt(modelRow, 0);
                TransactionHistoryRecord record = fetchTransactionDetailsFromDB(transactionId);
                if (record != null) {
                    recordsToProcess.add(record);
                    combinedReceiptContent.append(generateReceiptContent(record)).append("\n\n--- Struk Berikutnya ---\n\n");
                }
            }

            if (recordsToProcess.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Gagal memuat detail untuk transaksi terpilih.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Hapus pemisah terakhir jika ada
            if (combinedReceiptContent.length() > 28) { // Panjang dari "\n\n--- Struk Berikutnya ---\n\n"
                combinedReceiptContent.setLength(combinedReceiptContent.length() - "\n\n--- Struk Berikutnya ---\n\n".length());
            }


            if (saveToFile) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Simpan Struk Terpilih sebagai File Teks (.txt)");
                String defaultFileName = "struk_terpilih_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".txt";
                fileChooser.setSelectedFile(new File(defaultFileName));
                int userSelection = fileChooser.showSaveDialog(this);

                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File fileToSave = fileChooser.getSelectedFile();
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSave))) {
                        writer.write(combinedReceiptContent.toString());
                        JOptionPane.showMessageDialog(this, "Struk berhasil disimpan ke:\n" + fileToSave.getAbsolutePath() + 
                                                        "\n\nUntuk PDF, Anda bisa menggunakan printer virtual 'Print to PDF' dari aplikasi teks editor.", 
                                                        "Simpan Berhasil", JOptionPane.INFORMATION_MESSAGE);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(this, "Gagal menyimpan struk: " + ex.getMessage(), "Error Simpan File", JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    }
                }
            } else if (printToPhysicalPrinter) {
                JTextArea printArea = new JTextArea(combinedReceiptContent.toString());
                printArea.setFont(new Font("Monospaced", Font.PLAIN, 8)); // Font sangat kecil untuk thermal
                try {
                    // Menambahkan header dan footer untuk setiap halaman jika diperlukan (lebih kompleks)
                    // Untuk sekarang, kita cetak apa adanya.
                    boolean complete = printArea.print();
                    if (complete) {
                        JOptionPane.showMessageDialog(this, "Pencetakan struk dikirim ke printer.", "Info Cetak", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, "Pencetakan struk dibatalkan.", "Info Cetak", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (PrinterException ex) {
                    JOptionPane.showMessageDialog(this, "Gagal mencetak struk: " + ex.getMessage() + "\nPastikan printer terkonfigurasi dengan benar.", "Error Cetak", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            } else { // View
                JTextArea receiptArea = new JTextArea(combinedReceiptContent.toString());
                receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 10)); 
                receiptArea.setEditable(false);
                JScrollPane receiptScrollPane = new JScrollPane(receiptArea);
                receiptScrollPane.setPreferredSize(new Dimension(380, 500)); // Lebar disesuaikan untuk struk 58mm
                JOptionPane.showMessageDialog(this, receiptScrollPane, "Struk Transaksi Terpilih", JOptionPane.PLAIN_MESSAGE);
            }
        }
        
        private void processDailyReportActions() {
            String dateStr = txtSelectedDateForReport.getText().trim();
            if (dateStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Masukkan tanggal untuk laporan harian.", "Input Tanggal", JOptionPane.WARNING_MESSAGE);
                return;
            }
            LocalDate selectedDate;
            try {
                selectedDate = LocalDate.parse(dateStr, dateDbFormatter);
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this, "Format tanggal salah (YYYY-MM-DD).", "Error Format Tanggal", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double dailyTotal = fetchRevenueForDate(selectedDate);
            lblSelectedDateRevenue.setText("Total " + selectedDate.format(dateDisplayFormatter) + ": " + currencyFormatter.format(dailyTotal));

            List<TransactionHistoryRecord> dailyTransactions = fetchFullTransactionsForDateFromDB(selectedDate);
            if (dailyTransactions.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tidak ada transaksi pada tanggal " + selectedDate.format(dateDisplayFormatter) + ".", "Info Laporan", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            Object[] options = {"Simpan ke File (TXT)", "Cetak ke Printer", "Batal"};
            int choice = JOptionPane.showOptionDialog(this,
                    "Total pendapatan pada " + selectedDate.format(dateDisplayFormatter) + " adalah " + currencyFormatter.format(dailyTotal) + ".\n" +
                    "Jumlah transaksi: " + dailyTransactions.size() + ".\n\nApa yang ingin Anda lakukan dengan detail transaksi harian?",
                    "Laporan Harian " + selectedDate.format(dateDisplayFormatter),
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null, options, options[0]);

            if (choice == JOptionPane.YES_OPTION || choice == JOptionPane.NO_OPTION) { // Simpan atau Cetak
                StringBuilder dailyReportContent = new StringBuilder();
                dailyReportContent.append("LAPORAN TRANSAKSI HARIAN\n");
                dailyReportContent.append("Tanggal: ").append(selectedDate.format(dateDisplayFormatter)).append("\n");
                dailyReportContent.append("Total Pendapatan Hari Ini: ").append(currencyFormatter.format(dailyTotal)).append("\n");
                dailyReportContent.append("========================================\n\n");

                for (TransactionHistoryRecord record : dailyTransactions) {
                    dailyReportContent.append(generateReceiptContent(record)).append("\n\n");
                }

                if (choice == JOptionPane.YES_OPTION) { // Simpan
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("Simpan Laporan Harian sebagai File Teks (.txt)");
                    fileChooser.setSelectedFile(new File("laporan_harian_" + selectedDate.format(dateDbFormatter) + ".txt"));
                    if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileChooser.getSelectedFile()))) {
                            writer.write(dailyReportContent.toString());
                            JOptionPane.showMessageDialog(this, "Laporan harian berhasil disimpan.", "Simpan Berhasil", JOptionPane.INFORMATION_MESSAGE);
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(this, "Gagal menyimpan laporan: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } else { // Cetak
                    JTextArea printArea = new JTextArea(dailyReportContent.toString());
                    printArea.setFont(new Font("Monospaced", Font.PLAIN, 8)); // Font kecil untuk laporan panjang
                    try {
                        if (printArea.print()) {
                            JOptionPane.showMessageDialog(this, "Laporan harian dikirim ke printer.", "Info Cetak", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(this, "Pencetakan dibatalkan.", "Info Cetak", JOptionPane.WARNING_MESSAGE);
                        }
                    } catch (PrinterException ex) {
                         JOptionPane.showMessageDialog(this, "Gagal mencetak laporan: " + ex.getMessage(), "Error Cetak", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }


        private TransactionHistoryRecord fetchTransactionDetailsFromDB(int transactionId) {
            TransactionHistoryRecord record = null;
            String sqlTransaction = "SELECT * FROM transactions WHERE transaction_id = ?";
            String sqlItems = "SELECT item_name, item_type, quantity, price_per_item, total_price FROM transaction_items WHERE transaction_id = ?";

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement pstmtTransaction = conn.prepareStatement(sqlTransaction);
                 PreparedStatement pstmtItems = conn.prepareStatement(sqlItems)) {

                pstmtTransaction.setInt(1, transactionId);
                ResultSet rsTransaction = pstmtTransaction.executeQuery();

                if (rsTransaction.next()) {
                    record = new TransactionHistoryRecord(
                        rsTransaction.getInt("transaction_id"),
                        rsTransaction.getString("table_name"),
                        rsTransaction.getString("customer_name"),
                        rsTransaction.getTimestamp("transaction_timestamp"),
                        rsTransaction.getDouble("total_bill"),
                        rsTransaction.getString("payment_method"),
                        rsTransaction.getTime("start_time"),
                        rsTransaction.getTime("end_time"),
                        rsTransaction.getInt("total_play_duration_minutes"),
                        rsTransaction.getDouble("rental_cost"),
                        rsTransaction.getDouble("individual_fnb_cost"),
                        rsTransaction.getDouble("package_cost"),
                        rsTransaction.getDouble("additional_cost"),
                        rsTransaction.getDouble("member_discount_amount"),
                        rsTransaction.getDouble("manual_discount_amount"),
                        rsTransaction.getDouble("amount_paid"),
                        rsTransaction.getDouble("change_given")
                    );

                    pstmtItems.setInt(1, transactionId);
                    ResultSet rsItems = pstmtItems.executeQuery();
                    while (rsItems.next()) {
                        Map<String, Object> itemDetail = new HashMap<>();
                        itemDetail.put("name", rsItems.getString("item_name"));
                        itemDetail.put("type", rsItems.getString("item_type"));
                        itemDetail.put("qty", rsItems.getInt("quantity"));
                        itemDetail.put("price_per", rsItems.getDouble("price_per_item"));
                        itemDetail.put("total_price", rsItems.getDouble("total_price"));
                        record.items.add(itemDetail);
                    }
                    DBUtil.close(null, null, rsItems);
                }
                DBUtil.close(null, null, rsTransaction);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error mengambil detail transaksi: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
            return record;
        }

        private List<TransactionHistoryRecord> loadTransactionHistoryFromDB() {
            List<TransactionHistoryRecord> historyList = new ArrayList<>();
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            String sql = "SELECT transaction_id, table_name, customer_name, transaction_timestamp, total_bill, payment_method " +
                         "FROM transactions ORDER BY transaction_timestamp DESC"; 
            try {
                conn = DBUtil.getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    historyList.add(new TransactionHistoryRecord(
                            rs.getInt("transaction_id"),
                            rs.getString("table_name"),
                            rs.getString("customer_name"),
                            rs.getTimestamp("transaction_timestamp").toLocalDateTime(),
                            rs.getDouble("total_bill"),
                            rs.getString("payment_method")
                    ));
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error memuat riwayat transaksi: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            } finally {
                DBUtil.close(conn, stmt, rs);
            }
            return historyList;
        }

        private List<TransactionHistoryRecord> fetchFullTransactionsForDateFromDB(LocalDate date) {
            List<TransactionHistoryRecord> dailyRecords = new ArrayList<>();
            String sql = "SELECT * FROM transactions WHERE DATE(transaction_timestamp) = ? ORDER BY transaction_timestamp ASC";
             try (Connection conn = DBUtil.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDate(1, Date.valueOf(date));
                ResultSet rs = pstmt.executeQuery();
                while(rs.next()){
                    TransactionHistoryRecord record = new TransactionHistoryRecord(
                        rs.getInt("transaction_id"),
                        rs.getString("table_name"),
                        rs.getString("customer_name"),
                        rs.getTimestamp("transaction_timestamp"),
                        rs.getDouble("total_bill"),
                        rs.getString("payment_method"),
                        rs.getTime("start_time"),
                        rs.getTime("end_time"),
                        rs.getInt("total_play_duration_minutes"),
                        rs.getDouble("rental_cost"),
                        rs.getDouble("individual_fnb_cost"),
                        rs.getDouble("package_cost"),
                        rs.getDouble("additional_cost"),
                        rs.getDouble("member_discount_amount"),
                        rs.getDouble("manual_discount_amount"),
                        rs.getDouble("amount_paid"),
                        rs.getDouble("change_given")
                    );
                    // Ambil juga item-itemnya
                     try (PreparedStatement pstmtItems = conn.prepareStatement("SELECT item_name, item_type, quantity, price_per_item, total_price FROM transaction_items WHERE transaction_id = ?")) {
                        pstmtItems.setInt(1, record.transactionId);
                        ResultSet rsItems = pstmtItems.executeQuery();
                        while (rsItems.next()) {
                            Map<String, Object> itemDetail = new HashMap<>();
                            itemDetail.put("name", rsItems.getString("item_name"));
                            itemDetail.put("type", rsItems.getString("item_type"));
                            itemDetail.put("qty", rsItems.getInt("quantity"));
                            itemDetail.put("price_per", rsItems.getDouble("price_per_item"));
                            itemDetail.put("total_price", rsItems.getDouble("total_price"));
                            record.items.add(itemDetail);
                        }
                        DBUtil.close(null, null, rsItems);
                    }
                    dailyRecords.add(record);
                }
                DBUtil.close(null, null, rs);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error memuat transaksi harian: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
            return dailyRecords;
        }
    }

    // Inner class untuk Panel Manajemen Member (Sama seperti sebelumnya, tidak ada perubahan di sini)
    class MemberPanel extends JPanel {
        private JTextField txtMemberId, txtMemberName, txtPhoneNumber, txtEmail, txtRegistrationDate, txtExpiryDate;
        private JCheckBox chkIsActiveMember;
        private JComboBox<String> comboMemberCategory; 
        private JButton btnAddMember, btnSearchMember, btnUpdateMember, btnDeleteMember, btnResetMemberForm;
        private JTable memberTable;
        private DefaultTableModel memberTableModel;

        public MemberPanel() {
            setLayout(new BorderLayout(10, 10));
            setBorder(BorderFactory.createTitledBorder("Manajemen Member"));

            JPanel formPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5,5,5,5);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            int yForm = 0;

            gbc.gridx = 0; gbc.gridy = yForm; formPanel.add(new JLabel("ID Member:"), gbc);
            txtMemberId = new JTextField(15);
            gbc.gridx = 1; gbc.gridy = yForm; formPanel.add(txtMemberId, gbc); yForm++;

            gbc.gridx = 0; gbc.gridy = yForm; formPanel.add(new JLabel("Nama Member:"), gbc);
            txtMemberName = new JTextField(25);
            gbc.gridx = 1; gbc.gridy = yForm; formPanel.add(txtMemberName, gbc); yForm++;
            
            gbc.gridx = 0; gbc.gridy = yForm; formPanel.add(new JLabel("No. Telepon:"), gbc);
            txtPhoneNumber = new JTextField(15);
            gbc.gridx = 1; gbc.gridy = yForm; formPanel.add(txtPhoneNumber, gbc); yForm++;

            gbc.gridx = 0; gbc.gridy = yForm; formPanel.add(new JLabel("Email:"), gbc);
            txtEmail = new JTextField(25);
            gbc.gridx = 1; gbc.gridy = yForm; formPanel.add(txtEmail, gbc); yForm++;

            gbc.gridx = 0; gbc.gridy = yForm; formPanel.add(new JLabel("Kategori Member:"), gbc);
            String[] categories = {"Reguler", "Gold", "Atlet"};
            comboMemberCategory = new JComboBox<>(categories);
            gbc.gridx = 1; gbc.gridy = yForm; formPanel.add(comboMemberCategory, gbc); yForm++;


            gbc.gridx = 0; gbc.gridy = yForm; formPanel.add(new JLabel("Tgl. Daftar (YYYY-MM-DD):"), gbc);
            txtRegistrationDate = new JTextField(10);
            gbc.gridx = 1; gbc.gridy = yForm; formPanel.add(txtRegistrationDate, gbc); yForm++;
            
            gbc.gridx = 0; gbc.gridy = yForm; formPanel.add(new JLabel("Tgl. Kadaluarsa (YYYY-MM-DD):"), gbc);
            txtExpiryDate = new JTextField(10);
            gbc.gridx = 1; gbc.gridy = yForm; formPanel.add(txtExpiryDate, gbc); yForm++;

            gbc.gridx = 0; gbc.gridy = yForm; formPanel.add(new JLabel("Status Aktif:"), gbc);
            chkIsActiveMember = new JCheckBox("Aktif"); chkIsActiveMember.setSelected(true);
            gbc.gridx = 1; gbc.gridy = yForm; formPanel.add(chkIsActiveMember, gbc); yForm++;

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            btnAddMember = new JButton("Tambah Member");
            btnSearchMember = new JButton("Cari Member (by ID)");
            btnUpdateMember = new JButton("Simpan Perubahan");
            btnDeleteMember = new JButton("Hapus Member");
            btnResetMemberForm = new JButton("Reset Form");
            buttonPanel.add(btnAddMember);
            buttonPanel.add(btnSearchMember);
            buttonPanel.add(btnUpdateMember);
            buttonPanel.add(btnDeleteMember);
            buttonPanel.add(btnResetMemberForm);
            gbc.gridx = 0; gbc.gridy = yForm; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
            formPanel.add(buttonPanel, gbc); yForm++;

            String[] memberColumnNames = {"ID Member", "Nama", "No. Telepon", "Email", "Kategori", "Tgl. Daftar", "Tgl. Kadaluarsa", "Aktif"};
            memberTableModel = new DefaultTableModel(memberColumnNames, 0) {
                 @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            memberTable = new JTable(memberTableModel);
            memberTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            memberTable.setFillsViewportHeight(true);
            memberTable.setAutoCreateRowSorter(true);

            JScrollPane tableScrollPane = new JScrollPane(memberTable);
            
            add(formPanel, BorderLayout.NORTH);
            add(tableScrollPane, BorderLayout.CENTER);

            btnAddMember.addActionListener(e -> addMember());
            btnSearchMember.addActionListener(e -> searchMember());
            btnUpdateMember.addActionListener(e -> updateMember());
            btnDeleteMember.addActionListener(e -> deleteMember());
            btnResetMemberForm.addActionListener(e -> resetMemberForm());

            memberTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent event) {
                    if (!event.getValueIsAdjusting() && memberTable.getSelectedRow() != -1) {
                        populateFormFromSelectedTableRow();
                    }
                }
            });
            loadMembers(); 
        }

        private void populateFormFromSelectedTableRow(){
            int selectedRow = memberTable.getSelectedRow();
            if(selectedRow >= 0){
                txtMemberId.setText(memberTableModel.getValueAt(selectedRow, 0).toString());
                txtMemberName.setText(memberTableModel.getValueAt(selectedRow, 1).toString());
                txtPhoneNumber.setText(memberTableModel.getValueAt(selectedRow, 2) != null ? memberTableModel.getValueAt(selectedRow, 2).toString() : "");
                txtEmail.setText(memberTableModel.getValueAt(selectedRow, 3) != null ? memberTableModel.getValueAt(selectedRow, 3).toString() : "");
                comboMemberCategory.setSelectedItem(memberTableModel.getValueAt(selectedRow, 4).toString());
                txtRegistrationDate.setText(memberTableModel.getValueAt(selectedRow, 5) != null ? memberTableModel.getValueAt(selectedRow, 5).toString() : "");
                txtExpiryDate.setText(memberTableModel.getValueAt(selectedRow, 6) != null ? memberTableModel.getValueAt(selectedRow, 6).toString() : "");
                chkIsActiveMember.setSelected((Boolean)memberTableModel.getValueAt(selectedRow, 7));
                txtMemberId.setEditable(false); 
            }
        }

        private void addMember() {
            String memberId = txtMemberId.getText().trim();
            String name = txtMemberName.getText().trim();
            String phone = txtPhoneNumber.getText().trim();
            String email = txtEmail.getText().trim();
            String category = (String) comboMemberCategory.getSelectedItem();
            String regDateStr = txtRegistrationDate.getText().trim();
            String expDateStr = txtExpiryDate.getText().trim();
            boolean isActive = chkIsActiveMember.isSelected();

            if (memberId.isEmpty() || name.isEmpty() || phone.isEmpty()) {
                JOptionPane.showMessageDialog(this, "ID Member, Nama, dan No. Telepon wajib diisi!", "Input Kurang", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            Date registrationDate = null;
            Date expiryDate = null;
            try {
                if (!regDateStr.isEmpty()) registrationDate = Date.valueOf(LocalDate.parse(regDateStr, BilliardKasirApp.dateDbFormatter));
                if (!expDateStr.isEmpty()) expiryDate = Date.valueOf(LocalDate.parse(expDateStr, BilliardKasirApp.dateDbFormatter));
            } catch (DateTimeParseException e) {
                JOptionPane.showMessageDialog(this, "Format tanggal salah (YYYY-MM-DD).", "Error Format Tanggal", JOptionPane.ERROR_MESSAGE);
                return;
            }


            String sql = "INSERT INTO members (member_id, member_name, phone_number, email, member_category, registration_date, expiry_date, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, memberId);
                pstmt.setString(2, name);
                pstmt.setString(3, phone);
                pstmt.setString(4, email.isEmpty() ? null : email);
                pstmt.setString(5, category);
                pstmt.setDate(6, registrationDate);
                pstmt.setDate(7, expiryDate);
                pstmt.setBoolean(8, isActive);
                
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    JOptionPane.showMessageDialog(this, "Member berhasil ditambahkan!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                    loadMembers();
                    resetMemberForm();
                }
            } catch (SQLException e) {
                 if(e.getErrorCode() == 1062){ 
                    JOptionPane.showMessageDialog(this, "Gagal menambah member: ID Member atau No. Telepon sudah ada.", "Error Duplikat", JOptionPane.ERROR_MESSAGE);
                 } else {
                    JOptionPane.showMessageDialog(this, "Error menambah member: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                 }
                e.printStackTrace();
            }
        }
        
        private void searchMember() {
            String memberIdToSearch = JOptionPane.showInputDialog(this, "Masukkan ID Member yang dicari:");
            if (memberIdToSearch == null || memberIdToSearch.trim().isEmpty()) return;

            String sql = "SELECT member_name, phone_number, email, member_category, registration_date, expiry_date, is_active FROM members WHERE member_id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, memberIdToSearch.trim());
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    txtMemberId.setText(memberIdToSearch.trim());
                    txtMemberName.setText(rs.getString("member_name"));
                    txtPhoneNumber.setText(rs.getString("phone_number"));
                    txtEmail.setText(rs.getString("email"));
                    comboMemberCategory.setSelectedItem(rs.getString("member_category"));
                    Date regDate = rs.getDate("registration_date");
                    txtRegistrationDate.setText(regDate != null ? regDate.toLocalDate().format(BilliardKasirApp.dateDbFormatter) : "");
                    Date expDate = rs.getDate("expiry_date");
                    txtExpiryDate.setText(expDate != null ? expDate.toLocalDate().format(BilliardKasirApp.dateDbFormatter) : "");
                    chkIsActiveMember.setSelected(rs.getBoolean("is_active"));
                    txtMemberId.setEditable(false); 
                    JOptionPane.showMessageDialog(this, "Data member ditemukan.", "Info", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Member dengan ID " + memberIdToSearch + " tidak ditemukan.", "Info", JOptionPane.INFORMATION_MESSAGE);
                    resetMemberForm();
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error mencari member: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }

        private void updateMember() {
            String memberId = txtMemberId.getText().trim();
            if (memberId.isEmpty() || txtMemberId.isEditable()) { 
                JOptionPane.showMessageDialog(this, "Silakan cari member terlebih dahulu atau pilih dari tabel.", "Info", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String name = txtMemberName.getText().trim();
            String phone = txtPhoneNumber.getText().trim();
            String email = txtEmail.getText().trim();
            String category = (String) comboMemberCategory.getSelectedItem();
            String regDateStr = txtRegistrationDate.getText().trim();
            String expDateStr = txtExpiryDate.getText().trim();
            boolean isActive = chkIsActiveMember.isSelected();

            if (name.isEmpty() || phone.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nama dan No. Telepon wajib diisi!", "Input Kurang", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Date registrationDate = null;
            Date expiryDate = null;
            try {
                if (!regDateStr.isEmpty()) registrationDate = Date.valueOf(LocalDate.parse(regDateStr, BilliardKasirApp.dateDbFormatter));
                if (!expDateStr.isEmpty()) expiryDate = Date.valueOf(LocalDate.parse(expDateStr, BilliardKasirApp.dateDbFormatter));
            } catch (DateTimeParseException e) {
                JOptionPane.showMessageDialog(this, "Format tanggal salah (YYYY-MM-DD).", "Error Format Tanggal", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String sql = "UPDATE members SET member_name = ?, phone_number = ?, email = ?, member_category = ?, registration_date = ?, expiry_date = ?, is_active = ? WHERE member_id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, name);
                pstmt.setString(2, phone);
                pstmt.setString(3, email.isEmpty() ? null : email);
                pstmt.setString(4, category);
                pstmt.setDate(5, registrationDate);
                pstmt.setDate(6, expiryDate);
                pstmt.setBoolean(7, isActive);
                pstmt.setString(8, memberId);
                
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    JOptionPane.showMessageDialog(this, "Data member berhasil diperbarui!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                    loadMembers();
                    resetMemberForm();
                } else {
                    JOptionPane.showMessageDialog(this, "Gagal memperbarui data member (ID tidak ditemukan).", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException e) {
                 if(e.getErrorCode() == 1062){ 
                    JOptionPane.showMessageDialog(this, "Gagal memperbarui: No. Telepon atau Email sudah digunakan member lain.", "Error Duplikat", JOptionPane.ERROR_MESSAGE);
                 } else {
                    JOptionPane.showMessageDialog(this, "Error memperbarui member: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                 }
                e.printStackTrace();
            }
        }

        private void deleteMember() {
            String memberId = txtMemberId.getText().trim();
            if (memberId.isEmpty() || txtMemberId.isEditable()) {
                 JOptionPane.showMessageDialog(this, "Silakan cari member yang akan dihapus atau pilih dari tabel.", "Info", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this, "Apakah Anda yakin ingin menghapus member dengan ID: " + memberId + "?", "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                String sql = "DELETE FROM members WHERE member_id = ?";
                try (Connection conn = DBUtil.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, memberId);
                    int affectedRows = pstmt.executeUpdate();
                    if (affectedRows > 0) {
                        JOptionPane.showMessageDialog(this, "Member berhasil dihapus!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                        loadMembers();
                        resetMemberForm();
                    } else {
                        JOptionPane.showMessageDialog(this, "Gagal menghapus member (ID tidak ditemukan).", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Error menghapus member: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }
        
        private void resetMemberForm() {
            txtMemberId.setText("");
            txtMemberId.setEditable(true);
            txtMemberName.setText("");
            txtPhoneNumber.setText("");
            txtEmail.setText("");
            comboMemberCategory.setSelectedIndex(0); // Kembali ke Reguler
            txtRegistrationDate.setText("");
            txtExpiryDate.setText("");
            chkIsActiveMember.setSelected(true);
            memberTable.clearSelection();
            txtMemberId.requestFocus();
        }

        public void loadMembers() {
            memberTableModel.setRowCount(0);
            String sql = "SELECT member_id, member_name, phone_number, email, member_category, registration_date, expiry_date, is_active FROM members ORDER BY member_name";
            try (Connection conn = DBUtil.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    memberTableModel.addRow(new Object[]{
                        rs.getString("member_id"),
                        rs.getString("member_name"),
                        rs.getString("phone_number"),
                        rs.getString("email"),
                        rs.getString("member_category"), // Tampilkan kategori
                        rs.getDate("registration_date") != null ? rs.getDate("registration_date").toLocalDate().format(BilliardKasirApp.dateDbFormatter) : "",
                        rs.getDate("expiry_date") != null ? rs.getDate("expiry_date").toLocalDate().format(BilliardKasirApp.dateDbFormatter) : "",
                        rs.getBoolean("is_active")
                    });
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error memuat data member: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    // --- INNER CLASSES END HERE ---

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) { e.printStackTrace(); }

            LoginDialog loginDialog = new LoginDialog(null);
            loginDialog.setVisible(true);

            if (loginDialog.isLoggedIn()) {
                new BilliardKasirApp().setVisible(true);
            } else {
                System.out.println("Login dibatalkan atau gagal. Aplikasi keluar.");
                System.exit(0);
            }
        });
    }
}
