-- ============================================================
-- Mini Mart Management System — Database Schema
-- Database: MySQL 8.0+
-- Encoding: UTF-8
-- ============================================================

DROP DATABASE IF EXISTS minimart_db;
CREATE DATABASE minimart_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE minimart_db;

-- ============================================================
-- TABLE: users
-- Stores system users (Admin and Employee roles).
-- Passwords are stored as BCrypt hashes (never plain text).
-- ============================================================
CREATE TABLE users (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL COMMENT 'BCrypt hashed password',
    full_name   VARCHAR(100) NOT NULL,
    role        ENUM('ADMIN', 'EMPLOYEE') NOT NULL DEFAULT 'EMPLOYEE',
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_users_username (username),
    INDEX idx_users_role     (role)
) ENGINE=InnoDB COMMENT='System users with role-based access control';

-- ============================================================
-- TABLE: products
-- Inventory catalog. product_code is unique (e.g., SP001).
-- ============================================================
CREATE TABLE products (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    product_code    VARCHAR(20)    NOT NULL UNIQUE COMMENT 'e.g. SP001',
    name            VARCHAR(150)   NOT NULL,
    category        VARCHAR(50)    NOT NULL,
    description     TEXT,
    unit            VARCHAR(30)    NOT NULL DEFAULT 'Cái' COMMENT 'Unit of measure',
    price           DECIMAL(15,2)  NOT NULL CHECK (price >= 0),
    stock_quantity  INT            NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    status          ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    image_path      VARCHAR(255)   DEFAULT NULL,
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_products_code   (product_code),
    INDEX idx_products_name   (name),
    INDEX idx_products_status (status)
) ENGINE=InnoDB COMMENT='Product catalog with stock tracking';

-- ============================================================
-- TABLE: customers
-- Customer registry. phone_number is AES-encrypted.
-- ============================================================
CREATE TABLE customers (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    customer_name   VARCHAR(100) NOT NULL,
    phone_number    VARCHAR(255) NOT NULL COMMENT 'AES-256 encrypted phone number',
    email           VARCHAR(150),
    address         TEXT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_customers_name (customer_name)
) ENGINE=InnoDB COMMENT='Customer records with encrypted contact info';

-- ============================================================
-- TABLE: orders
-- An order ties a user (cashier) to a customer (buyer).
-- total_amount is stored redundantly for fast reporting.
-- ============================================================
CREATE TABLE orders (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    order_date      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_amount    DECIMAL(15,2)  NOT NULL DEFAULT 0.00,
    note            VARCHAR(255),
    user_id         INT            NOT NULL COMMENT 'Cashier who processed the order',
    customer_id     INT            COMMENT 'NULL allowed for walk-in customers',
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_orders_user     FOREIGN KEY (user_id)     REFERENCES users(id)     ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON UPDATE CASCADE ON DELETE SET NULL,

    INDEX idx_orders_user_id     (user_id),
    INDEX idx_orders_customer_id (customer_id),
    INDEX idx_orders_order_date  (order_date)
) ENGINE=InnoDB COMMENT='Sales orders header';

-- ============================================================
-- TABLE: order_items
-- Each row is one product line in an order.
-- price_at_sale captures the price at the moment of purchase.
-- ============================================================
CREATE TABLE order_items (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    order_id        INT            NOT NULL,
    product_id      INT            NOT NULL,
    quantity        INT            NOT NULL CHECK (quantity > 0),
    price_at_sale   DECIMAL(15,2)  NOT NULL COMMENT 'Snapshot of product price at purchase time',
    subtotal        DECIMAL(15,2)  GENERATED ALWAYS AS (quantity * price_at_sale) STORED,

    CONSTRAINT fk_order_items_order   FOREIGN KEY (order_id)   REFERENCES orders(id)   ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id) ON UPDATE CASCADE ON DELETE RESTRICT,

    INDEX idx_order_items_order_id   (order_id),
    INDEX idx_order_items_product_id (product_id)
) ENGINE=InnoDB COMMENT='Sales order line items';

-- ============================================================
-- TABLE: system_logs
-- Structured audit trail written by the server for all
-- significant events (LOGIN, CRUD, ERRORS, etc.).
-- ============================================================
CREATE TABLE system_logs (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    log_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    level       ENUM('INFO','WARN','ERROR') NOT NULL DEFAULT 'INFO',
    action      VARCHAR(50)  NOT NULL COMMENT 'Protocol action name',
    username    VARCHAR(50)  COMMENT 'Actor username (NULL for system)',
    message     TEXT         NOT NULL,
    ip_address  VARCHAR(45),

    INDEX idx_logs_log_time (log_time),
    INDEX idx_logs_level    (level),
    INDEX idx_logs_action   (action)
) ENGINE=InnoDB COMMENT='Server-side structured audit log';


-- ============================================================
-- SAMPLE DATA
-- ============================================================

-- ── Users ──────────────────────────────────────────────────
-- BCrypt hash of 'Admin@123'  (cost 12)
-- BCrypt hash of 'Emp@123'    (cost 12)
INSERT INTO users (username, password, full_name, role) VALUES
('admin',    '$2a$12$V0DbYqNhh94vEsbTLmbZ4Onz84rd4BNkcVQaqkPuWvR521sYiad3e', 'Nguyễn Quản Trị', 'ADMIN'),
('nhanvien1','$2a$12$A2hG0kecyxExMyapgxgMwenXQM8bz5r//SsQ8FCTRssmHg3fsFMLi', 'Trần Nhân Viên', 'EMPLOYEE'),
('nhanvien2','$2a$12$A2hG0kecyxExMyapgxgMwenXQM8bz5r//SsQ8FCTRssmHg3fsFMLi', 'Lê Thị Hoa',     'EMPLOYEE');

-- ── Products ───────────────────────────────────────────────
INSERT INTO products (product_code, name, category, description, unit, price, stock_quantity, status, image_path) VALUES
('SP001', 'Nước suối Aquafina 500ml', 'Đồ uống', 'Nước tinh khiết Aquafina đóng chai 500ml', 'Chai', 6000.00, 200, 'ACTIVE', NULL),
('SP002', 'Coca-Cola 330ml lon', 'Đồ uống', 'Nước ngọt có gas Coca-Cola lon 330ml', 'Lon', 12000.00, 150, 'ACTIVE', NULL),
('SP003', 'Mì Hảo Hảo tôm chua cay', 'Thực phẩm đóng gói', 'Mì ăn liền gói Hảo Hảo tôm chua cay 75g', 'Gói', 5000.00, 500, 'ACTIVE', NULL),
('SP004', 'Bánh Oreo vị socola', 'Bánh kẹo & Ăn vặt', 'Bánh quy nhân kem Oreo hương Socola 133g', 'Hộp', 25000.00, 80, 'ACTIVE', NULL),
('SP005', 'Sữa tươi TH True Milk 1L', 'Thực phẩm tươi & Sữa', 'Sữa tươi tiệt trùng nguyên chất TH không đường 1L', 'Hộp', 35000.00, 100, 'ACTIVE', NULL),
('SP006', 'Dầu ăn Tường An 1L', 'Thực phẩm đóng gói', 'Dầu thực vật Tường An Cooking Oil chai 1L', 'Chai', 45000.00, 60, 'ACTIVE', NULL),
('SP007', 'Gạo ST25 thơm đặc sản 5kg', 'Thực phẩm đóng gói', 'Gạo ngon nhất thế giới ST25 túi 5kg thơm dẻo', 'Túi', 120000.00, 40, 'ACTIVE', NULL),
('SP008', 'Xà phòng Lifebuoy 90g', 'Hóa mỹ phẩm & Đồ gia dụng', 'Xà bông kháng khuẩn Lifebuoy bảo vệ vượt trội 90g', 'Bánh', 15000.00, 120, 'ACTIVE', NULL),
('SP009', 'Bia Tiger lon 330ml', 'Đồ uống', 'Bia Tiger Premium lon 330ml', 'Lon', 18000.00, 75, 'ACTIVE', NULL),
('SP010', 'Kẹo bạc hà Extra', 'Bánh kẹo & Ăn vặt', 'Kẹo cao su không đường Extra hương bạc hà', 'Gói', 8000.00, 300, 'ACTIVE', NULL),
('SP011', 'Bột giặt Omo 3kg', 'Hóa mỹ phẩm & Đồ gia dụng', 'Bột giặt OMO sạch cực nhanh túi 3kg', 'Túi', 98000.00, 30, 'ACTIVE', NULL),
('SP012', 'Nước mắm Phú Quốc 500ml', 'Thực phẩm đóng gói', 'Nước mắm cá cơm truyền thống Phú Quốc chai 500ml', 'Chai', 32000.00, 50, 'ACTIVE', NULL),
('SP013', 'Snack Poca vị BBQ', 'Bánh kẹo & Ăn vặt', 'Snack khoai tây Poca vị sườn nướng BBQ 75g', 'Gói', 10000.00, 200, 'ACTIVE', NULL),
('SP014', 'Trà đá Lipton túi lọc', 'Đồ uống', 'Trà nhãn vàng Lipton hộp 25 túi lọc thơm ngon', 'Hộp', 22000.00, 90, 'ACTIVE', NULL),
('SP015', 'Sữa đặc Ông Thọ 380g', 'Thực phẩm tươi & Sữa', 'Sữa đặc có đường Ông Thọ Vinamilk lon thiếc 380g', 'Lon', 25000.00, 5, 'ACTIVE', NULL),
('SP016', 'Nước tăng lực Red Bull 250ml', 'Đồ uống', 'Nước tăng lực Red Bull Thái Lan bò húc lon 250ml', 'Lon', 12000.00, 120, 'ACTIVE', NULL),
('SP017', 'Nước ngọt Sting hương dâu 330ml', 'Đồ uống', 'Nước tăng lực Sting dâu đỏ lon 330ml', 'Lon', 11000.00, 180, 'ACTIVE', NULL),
('SP018', 'Nước ngọt Pepsi 330ml lon', 'Đồ uống', 'Nước ngọt có gas Pepsi Cola lon 330ml', 'Lon', 12000.00, 160, 'ACTIVE', NULL),
('SP019', 'Nước ngọt 7Up 330ml lon', 'Đồ uống', 'Nước ngọt có gas hương chanh 7Up lon 330ml', 'Lon', 12000.00, 140, 'ACTIVE', NULL),
('SP020', 'Trà xanh không độ C2 360ml', 'Đồ uống', 'Trà xanh đóng chai C2 hương chanh thanh mát 360ml', 'Chai', 7000.00, 220, 'ACTIVE', NULL),
('SP021', 'Trà Ô Long Tea+ Plus 455ml', 'Đồ uống', 'Trà Ô Long Tea Plus tốt cho sức khỏe chai 455ml', 'Chai', 10000.00, 150, 'ACTIVE', NULL),
('SP022', 'Bia Heineken lon 330ml', 'Đồ uống', 'Bia Heineken Premium cao cấp lon 330ml', 'Lon', 21000.00, 90, 'ACTIVE', NULL),
('SP023', 'Mì Omachi xốt bò hầm', 'Thực phẩm đóng gói', 'Mì khoai tây Omachi hương vị bò hầm gói 80g', 'Gói', 9000.00, 350, 'ACTIVE', NULL),
('SP024', 'Mì Kokomi 90 tôm chua cay', 'Thực phẩm đóng gói', 'Mì ăn liền Kokomi đại gói 90g dai giòn', 'Gói', 4500.00, 400, 'ACTIVE', NULL),
('SP025', 'Cháo ăn liền Gấu Đỏ thịt bằm', 'Thực phẩm đóng gói', 'Cháo ăn liền Gấu Đỏ hương vị thịt bằm gói 50g', 'Gói', 4000.00, 150, 'ACTIVE', NULL),
('SP026', 'Hạt nêm Knorr thịt thăn 900g', 'Thực phẩm đóng gói', 'Hạt nêm Knorr từ thịt thăn, xương ống và tủy gói 900g', 'Túi', 85000.00, 85, 'ACTIVE', NULL),
('SP027', 'Nước mắm Nam Ngư 750ml', 'Thực phẩm đóng gói', 'Nước mắm Nam Ngư Đệ Nhị chai lớn 750ml', 'Chai', 28000.00, 70, 'ACTIVE', NULL),
('SP028', 'Tương ớt Chin-su chai 250g', 'Thực phẩm đóng gói', 'Tương ớt cay nồng Chin-su chai 250g', 'Chai', 14000.00, 180, 'ACTIVE', NULL),
('SP029', 'Snack Lay\'s khoai tây tự nhiên', 'Bánh kẹo & Ăn vặt', 'Snack khoai tây Lay\'s Classic gói lớn 95g', 'Gói', 18000.00, 120, 'ACTIVE', NULL),
('SP030', 'Bánh quy giòn AFC lúa mì', 'Bánh kẹo & Ăn vặt', 'Bánh quy giòn AFC vị lúa mì hộp 172g dinh dưỡng', 'Hộp', 32000.00, 110, 'ACTIVE', NULL),
('SP031', 'Bánh Choco-Pie Orion hộp 6 cái', 'Bánh kẹo & Ăn vặt', 'Bánh socola Chocopie Orion hộp 6 cái 198g', 'Hộp', 35000.00, 95, 'ACTIVE', NULL),
('SP032', 'Bánh quy bơ Cosy Kinh Đô 200g', 'Bánh kẹo & Ăn vặt', 'Bánh quy bơ giòn Cosy Kinh Đô gói 200g', 'Gói', 22000.00, 80, 'ACTIVE', NULL),
('SP033', 'Kẹo dẻo Haribo Goldbears 80g', 'Bánh kẹo & Ăn vặt', 'Kẹo dẻo Haribo hình chú gấu vàng nhập khẩu gói 80g', 'Gói', 24000.00, 150, 'ACTIVE', NULL),
('SP034', 'Socola KitKat 4 thanh 35g', 'Bánh kẹo & Ăn vặt', 'Bánh xốp phủ socola KitKat Nestlé 4 thanh 35g', 'Gói', 16000.00, 130, 'ACTIVE', NULL),
('SP035', 'Dầu gội Sunsilk mềm mượt 320ml', 'Hóa mỹ phẩm & Đồ gia dụng', 'Dầu gội Sunsilk óng mượt rạng ngời chai 320ml', 'Chai', 75000.00, 65, 'ACTIVE', NULL),
('SP036', 'Kem đánh răng P/S bảo vệ 240g', 'Hóa mỹ phẩm & Đồ gia dụng', 'Kem đánh răng P/S bảo vệ 3 tác động tuýp 240g', 'Tuýp', 38000.00, 140, 'ACTIVE', NULL),
('SP037', 'Nước rửa chén Sunlight chanh 1.4L', 'Hóa mỹ phẩm & Đồ gia dụng', 'Nước rửa chén Sunlight hương chanh dạng túi tiết kiệm 1.4L', 'Túi', 48000.00, 90, 'ACTIVE', NULL),
('SP038', 'Nước xả vải Downy huyền bí 1.5L', 'Hóa mỹ phẩm & Đồ gia dụng', 'Nước xả Downy Huyền Bí hương thơm nước hoa túi 1.5L', 'Túi', 105000.00, 70, 'ACTIVE', NULL),
('SP039', 'Khăn giấy lụa Pulppy 2 lớp', 'Hóa mỹ phẩm & Đồ gia dụng', 'Khăn giấy ăn Pulppy lụa cao cấp 2 lớp hộp 180 tờ', 'Hộp', 24000.00, 110, 'ACTIVE', NULL),
('SP040', 'Sữa tắm kháng khuẩn Lifebuoy 850g', 'Hóa mỹ phẩm & Đồ gia dụng', 'Sữa tắm Lifebuoy Bảo Vệ Vượt Trội chai vòi 850g', 'Chai', 155000.00, 45, 'ACTIVE', NULL),
('SP041', 'Bàn chải Colgate lông mềm', 'Hóa mỹ phẩm & Đồ gia dụng', 'Bàn chải đánh răng Colgate lông tơ siêu mềm mịn', 'Cái', 18000.00, 150, 'ACTIVE', NULL),
('SP042', 'Nước lau sàn Sunlight thảo mộc 1kg', 'Hóa mỹ phẩm & Đồ gia dụng', 'Nước lau sàn Sunlight hương hoa lily và thảo mộc chai 1kg', 'Chai', 32000.00, 80, 'ACTIVE', NULL),
('SP043', 'Sữa chua ăn Vinamilk có đường', 'Thực phẩm tươi & Sữa', 'Sữa chua ăn Vinamilk có đường vỉ 4 hộp x 100g', 'Vỉ', 26000.00, 100, 'ACTIVE', NULL),
('SP044', 'Sữa lúa mạch Milo hộp 180ml', 'Thực phẩm tươi & Sữa', 'Thức uống dinh dưỡng Milo Nestlé lốc 4 hộp x 180ml', 'Lốc', 30000.00, 160, 'ACTIVE', NULL),
('SP045', 'Bơ thực vật Tường An Margarine 200g', 'Thực phẩm tươi & Sữa', 'Bơ thực vật Margarine Tường An hũ 200g làm bánh chiên rán', 'Hũ', 19000.00, 60, 'ACTIVE', NULL),
('SP046', 'Trứng gà sạch vỉ 10 quả', 'Thực phẩm tươi & Sữa', 'Hộp vỉ 10 quả trứng gà ta sạch đã kiểm dịch', 'Vỉ', 32000.00, 75, 'ACTIVE', NULL),
('SP047', 'Xúc xích tiệt trùng Vissan bò 100g', 'Thực phẩm tươi & Sữa', 'Xúc xích ăn liền heo bò Vissan gói 4 cây 100g', 'Gói', 12000.00, 140, 'ACTIVE', NULL),
('SP048', 'Phô mai Con Bò Cười hộp 8 miếng', 'Thực phẩm tươi & Sữa', 'Phô mai miếng tam giác Con Bò Cười hộp tròn 8 miếng', 'Hộp', 42000.00, 85, 'ACTIVE', NULL);

-- ── Customers (phone numbers are AES-encrypted in real app; plain text here for demo) ──
-- NOTE: When the server inserts real customers, it encrypts phone_number with AES.
--       The values below are PLACEHOLDER encrypted blobs (Base64) for sample data.
--       To generate real encrypted values, run the server's AESUtil on the plaintext.
INSERT INTO customers (customer_name, phone_number, email, address) VALUES
('Nguyễn Văn An',   'ENC:qakoU4TvQKTgRszgWnZlTQ==', 'an.nguyen@email.com',   'Q1, TP.HCM'),
('Trần Thị Bích',   'ENC:46UFK7t7zboCqdeO076wSQ==', 'bich.tran@email.com',   'Q3, TP.HCM'),
('Lê Hoàng Cường',  'ENC:OdLC0FnaFrE0DmSpTDiw6Q==', 'cuong.le@email.com',    'Bình Thạnh, TP.HCM'),
('Phạm Ngọc Diệp',  'ENC:9g7DhuC65AuwBMmsO0Zd9g==', 'diep.pham@email.com',   'Q7, TP.HCM'),
('Hoàng Thị Lan',   'ENC:QDJqQTYAuw9IfZsVporuaQ==', 'lan.hoang@email.com',   'Gò Vấp, TP.HCM');

-- ── Orders & Order Items (sample historical data for dashboard chart) ──
-- Order 1: 2026-06-01, by admin, customer 1
INSERT INTO orders (order_date, total_amount, user_id, customer_id, note)
VALUES ('2026-06-01 09:15:00', 72000.00, 1, 1, 'Khách quen');

INSERT INTO order_items (order_id, product_id, quantity, price_at_sale)
VALUES
(1, 1, 4, 6000.00),   -- 4x Aquafina
(1, 2, 3, 12000.00),  -- 3x Coca-Cola
(1, 3, 2, 5000.00);   -- 2x Mì Hảo Hảo

-- Order 2: 2026-06-05, by nhanvien1, walk-in
INSERT INTO orders (order_date, total_amount, user_id, customer_id, note)
VALUES ('2026-06-05 14:30:00', 155000.00, 2, NULL, 'Khách vãng lai');

INSERT INTO order_items (order_id, product_id, quantity, price_at_sale)
VALUES
(2, 5, 2, 35000.00),  -- 2x TH True Milk
(2, 7, 1, 120000.00); -- intentionally keeps sample valid

-- Order 3: 2026-06-10
INSERT INTO orders (order_date, total_amount, user_id, customer_id)
VALUES ('2026-06-10 11:00:00', 255000.00, 1, 2);

INSERT INTO order_items (order_id, product_id, quantity, price_at_sale)
VALUES
(3, 7, 1, 120000.00), -- Gạo ST25
(3, 6, 1, 45000.00),  -- Dầu ăn
(3, 11,1, 98000.00);  -- Bột giặt Omo

-- Order 4: 2026-06-15
INSERT INTO orders (order_date, total_amount, user_id, customer_id)
VALUES ('2026-06-15 16:45:00', 108000.00, 2, 3);

INSERT INTO order_items (order_id, product_id, quantity, price_at_sale)
VALUES
(4, 9, 6, 18000.00); -- 6x Tiger = 108000

-- Order 5: 2026-06-18
INSERT INTO orders (order_date, total_amount, user_id, customer_id)
VALUES ('2026-06-18 10:20:00', 342000.00, 1, 4);

INSERT INTO order_items (order_id, product_id, quantity, price_at_sale)
VALUES
(5, 5,  4, 35000.00),  -- 4x Sữa TH
(5, 11, 1, 98000.00),  -- 1x Omo
(5, 8,  3, 15000.00);  -- 3x Lifebuoy

-- ── Sample System Logs ──────────────────────────────────────
INSERT INTO system_logs (log_time, level, action, username, message, ip_address) VALUES
('2026-06-19 08:00:01', 'INFO',  'SERVER_START', NULL,        'Server started on port 9999',                          '127.0.0.1'),
('2026-06-19 08:05:32', 'INFO',  'LOGIN',        'admin',     'User logged in successfully',                          '127.0.0.1'),
('2026-06-19 08:10:14', 'INFO',  'ADD_PRODUCT',  'admin',     'Product added: SP015 - Sữa đặc Ông Thọ 380g',         '127.0.0.1'),
('2026-06-19 08:22:45', 'INFO',  'LOGIN',        'nhanvien1', 'User logged in successfully',                          '127.0.0.1'),
('2026-06-19 09:01:00', 'INFO',  'CREATE_ORDER', 'nhanvien1', 'Order #5 created. Total: 342000 VND',                  '127.0.0.1'),
('2026-06-19 09:15:00', 'WARN',  'LOW_STOCK',    NULL,        'Product SP015 stock critically low: 5 units remaining','127.0.0.1'),
('2026-06-19 10:00:05', 'ERROR', 'CREATE_ORDER', 'nhanvien2', 'Transaction rolled back: insufficient stock for SP011','127.0.0.1');

-- ============================================================
-- VIEWS (optional helper queries for reporting)
-- ============================================================

-- Revenue per day (used by Dashboard BarChart)
CREATE OR REPLACE VIEW vw_daily_revenue AS
SELECT
    DATE(order_date)          AS sale_date,
    COUNT(id)                 AS order_count,
    SUM(total_amount)         AS total_revenue
FROM orders
GROUP BY DATE(order_date)
ORDER BY sale_date;

-- Products with low stock (threshold = 10)
CREATE OR REPLACE VIEW vw_low_stock_products AS
SELECT id, product_code, name, stock_quantity, unit
FROM   products
WHERE  stock_quantity <= 10 AND status = 'ACTIVE'
ORDER  BY stock_quantity ASC;

-- ============================================================
-- END OF SCHEMA
-- ============================================================
