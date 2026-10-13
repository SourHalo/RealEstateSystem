CREATE DATABASE IF NOT EXISTS EstateEase;

USE EstateEase;


-- ============================================
-- EstateEase: Real Estate Management System
-- Database and Table Creation
-- DBMS: MySQL
-- ============================================


-- 1. OWNER
CREATE TABLE Owner (
    Owner_ID INT AUTO_INCREMENT PRIMARY KEY,
    Owner_Name VARCHAR(100) NOT NULL,
    Phone VARCHAR(15),
    Email VARCHAR(100) UNIQUE,
    Address VARCHAR(255)
);

-- 2. PROPERTY
CREATE TABLE Property (
    Property_ID INT AUTO_INCREMENT PRIMARY KEY,
    Owner_ID INT NOT NULL,
    Address VARCHAR(255) NOT NULL,
    Property_Type VARCHAR(50),
    Status VARCHAR(30) DEFAULT 'Available',

    FOREIGN KEY (Owner_ID)
        REFERENCES Owner(Owner_ID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- 3. TENANT
CREATE TABLE Tenant (
    Tenant_ID INT AUTO_INCREMENT PRIMARY KEY,
    Tenant_Name VARCHAR(100) NOT NULL,
    Phone VARCHAR(15),
    Email VARCHAR(100) UNIQUE,
    Address VARCHAR(255)
);

-- 4. RENTAL AGREEMENT
CREATE TABLE Rental_Agreement (
    Agreement_ID INT AUTO_INCREMENT PRIMARY KEY,
    Property_ID INT NOT NULL,
    Tenant_ID INT NOT NULL,
    Start_Date DATE NOT NULL,
    End_Date DATE NOT NULL,
    Monthly_Rent DECIMAL(10,2) NOT NULL,
    Status VARCHAR(30) DEFAULT 'Active',

    FOREIGN KEY (Property_ID)
        REFERENCES Property(Property_ID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    FOREIGN KEY (Tenant_ID)
        REFERENCES Tenant(Tenant_ID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CHECK (Monthly_Rent >= 0),
    CHECK (End_Date >= Start_Date)
);

-- 5. PAYMENT
CREATE TABLE Payment (
    Payment_ID INT AUTO_INCREMENT PRIMARY KEY,
    Agreement_ID INT NOT NULL,
    Payment_Date DATE NOT NULL,
    Amount DECIMAL(10,2) NOT NULL,
    Payment_Status VARCHAR(30) DEFAULT 'Pending',

    FOREIGN KEY (Agreement_ID)
        REFERENCES Rental_Agreement(Agreement_ID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CHECK (Amount >= 0)
);

-- 6. MAINTENANCE REQUEST
CREATE TABLE Maintenance_Request (
    Request_ID INT AUTO_INCREMENT PRIMARY KEY,
    Property_ID INT NOT NULL,
    Description VARCHAR(500) NOT NULL,
    Request_Date DATE NOT NULL,
    Status VARCHAR(30) DEFAULT 'Pending',

    FOREIGN KEY (Property_ID)
        REFERENCES Property(Property_ID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- 7. CONTRACTOR
CREATE TABLE Contractor (
    Contractor_ID INT AUTO_INCREMENT PRIMARY KEY,
    Contractor_Name VARCHAR(100) NOT NULL,
    Phone VARCHAR(15),
    Email VARCHAR(100) UNIQUE,
    Specialization VARCHAR(100)
);

-- 8. WORK ORDER
CREATE TABLE Work_Order (
    WorkOrder_ID INT AUTO_INCREMENT PRIMARY KEY,
    Request_ID INT NOT NULL,
    Assignment_Date DATE NOT NULL,
    Status VARCHAR(30) DEFAULT 'Assigned',

    FOREIGN KEY (Request_ID)
        REFERENCES Maintenance_Request(Request_ID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- 9. WORK ORDER CONTRACTOR
-- Junction table for the M:N relationship
-- between Work_Order and Contractor.
CREATE TABLE Work_Order_Contractor (
    WorkOrder_ID INT NOT NULL,
    Contractor_ID INT NOT NULL,

    PRIMARY KEY (WorkOrder_ID, Contractor_ID),

    FOREIGN KEY (WorkOrder_ID)
        REFERENCES Work_Order(WorkOrder_ID)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    FOREIGN KEY (Contractor_ID)
        REFERENCES Contractor(Contractor_ID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- 10. ROLE
CREATE TABLE Role (
    Role_ID INT AUTO_INCREMENT PRIMARY KEY,
    Role_Name VARCHAR(50) NOT NULL UNIQUE,
    Description VARCHAR(255),
    Status VARCHAR(30) DEFAULT 'Active'
);

-- 11. USER
CREATE TABLE `User` (
    User_ID INT AUTO_INCREMENT PRIMARY KEY,
    Role_ID INT NOT NULL,
    Username VARCHAR(50) NOT NULL UNIQUE,
    Password_Hash VARCHAR(255) NOT NULL,
    Account_Status VARCHAR(30) DEFAULT 'Active',

    FOREIGN KEY (Role_ID)
        REFERENCES Role(Role_ID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- ============================================
-- Verify that all tables have been created
-- ============================================

SHOW TABLES;

-- View the structure of individual tables
DESCRIBE Owner;
DESCRIBE Property;
DESCRIBE Tenant;
DESCRIBE Rental_Agreement;
DESCRIBE Payment;
DESCRIBE Maintenance_Request;
DESCRIBE Contractor;
DESCRIBE Work_Order;
DESCRIBE Work_Order_Contractor;
DESCRIBE Role;
DESCRIBE `User`;




USE EstateEase;

-- ============================================
-- 1. OWNER: 10 records
-- ============================================
INSERT INTO Owner
(Owner_ID, Owner_Name, Phone, Email, Address)
VALUES
(1, 'Aarav Sharma', '9876500001', 'aarav@gmail.com', 'Kothrud, Pune'),
(2, 'Priya Patel', '9876500002', 'priya@gmail.com', 'Baner, Pune'),
(3, 'Rahul Mehta', '9876500003', 'rahul@gmail.com', 'Wakad, Pune'),
(4, 'Sneha Desai', '9876500004', 'sneha@gmail.com', 'Hinjewadi, Pune'),
(5, 'Vikram Shah', '9876500005', 'vikram@gmail.com', 'Viman Nagar, Pune'),
(6, 'Ananya Joshi', '9876500006', 'ananya@gmail.com', 'Aundh, Pune'),
(7, 'Rohan Kulkarni', '9876500007', 'rohan@gmail.com', 'Pimple Saudagar, Pune'),
(8, 'Neha Verma', '9876500008', 'neha@gmail.com', 'Hadapsar, Pune'),
(9, 'Karan Malhotra', '9876500009', 'karan@gmail.com', 'Kharadi, Pune'),
(10, 'Isha Kapoor', '9876500010', 'isha@gmail.com', 'Shivajinagar, Pune');


-- ============================================
-- 2. PROPERTY: 10 records
-- ============================================
INSERT INTO Property
(Property_ID, Owner_ID, Address, Property_Type, Status)
VALUES
(101, 1, 'Flat 101, Green Heights, Kothrud', 'Apartment', 'Rented'),
(102, 2, 'Flat 202, Blue Residency, Baner', 'Apartment', 'Rented'),
(103, 3, 'Row House 12, Wakad', 'House', 'Rented'),
(104, 4, 'Flat 304, Sunrise Towers, Hinjewadi', 'Apartment', 'Rented'),
(105, 5, 'Shop 5, Viman Nagar Market', 'Commercial', 'Rented'),
(106, 6, 'Flat 406, Aundh Residency', 'Apartment', 'Available'),
(107, 7, 'Row House 18, Pimple Saudagar', 'House', 'Rented'),
(108, 8, 'Flat 508, Hadapsar Heights', 'Apartment', 'Under Maintenance'),
(109, 9, 'Office 301, Kharadi Business Park', 'Commercial', 'Available'),
(110, 10, 'Flat 610, Shivajinagar Towers', 'Apartment', 'Rented');


-- ============================================
-- 3. TENANT: 10 records
-- ============================================
INSERT INTO Tenant
(Tenant_ID, Tenant_Name, Phone, Email, Address)
VALUES
(201, 'Aditya Rao', '9811100001', 'aditya@gmail.com', 'Kothrud, Pune'),
(202, 'Meera Nair', '9811100002', 'meera@gmail.com', 'Baner, Pune'),
(203, 'Siddharth Jain', '9811100003', 'siddharth@gmail.com', 'Wakad, Pune'),
(204, 'Kavya Singh', '9811100004', 'kavya@gmail.com', 'Hinjewadi, Pune'),
(205, 'Arjun Reddy', '9811100005', 'arjun@gmail.com', 'Viman Nagar, Pune'),
(206, 'Diya Iyer', '9811100006', 'diya@gmail.com', 'Aundh, Pune'),
(207, 'Manav Gupta', '9811100007', 'manav@gmail.com', 'Pimple Saudagar, Pune'),
(208, 'Riya Das', '9811100008', 'riya@gmail.com', 'Hadapsar, Pune'),
(209, 'Kabir Sethi', '9811100009', 'kabir@gmail.com', 'Kharadi, Pune'),
(210, 'Tara Menon', '9811100010', 'tara@gmail.com', 'Shivajinagar, Pune');


-- ============================================
-- 4. RENTAL_AGREEMENT: 10 records
-- ============================================
INSERT INTO Rental_Agreement
(Agreement_ID, Property_ID, Tenant_ID,
 Start_Date, End_Date, Monthly_Rent, Status)
VALUES
(301, 101, 201, '2026-01-01', '2026-12-31', 18000.00, 'Active'),
(302, 102, 202, '2026-02-01', '2027-01-31', 22000.00, 'Active'),
(303, 103, 203, '2026-01-15', '2027-01-14', 25000.00, 'Active'),
(304, 104, 204, '2026-03-01', '2027-02-28', 20000.00, 'Active'),
(305, 105, 205, '2026-01-01', '2026-12-31', 30000.00, 'Active'),
(306, 107, 207, '2026-04-01', '2027-03-31', 28000.00, 'Active'),
(307, 108, 208, '2026-02-15', '2027-02-14', 16000.00, 'Active'),
(308, 110, 210, '2026-05-01', '2027-04-30', 24000.00, 'Active'),
(309, 101, 206, '2024-01-01', '2024-12-31', 15000.00, 'Expired'),
(310, 102, 209, '2024-06-01', '2025-05-31', 19000.00, 'Expired');


-- ============================================
-- 5. PAYMENT: 10 records
-- ============================================
INSERT INTO Payment
(Payment_ID, Agreement_ID, Payment_Date, Amount, Payment_Status)
VALUES
(401, 301, '2026-01-05', 18000.00, 'Paid'),
(402, 302, '2026-02-05', 22000.00, 'Paid'),
(403, 303, '2026-01-16', 25000.00, 'Paid'),
(404, 304, '2026-03-05', 20000.00, 'Paid'),
(405, 305, '2026-01-05', 30000.00, 'Paid'),
(406, 306, '2026-04-05', 28000.00, 'Pending'),
(407, 307, '2026-02-20', 16000.00, 'Paid'),
(408, 308, '2026-05-05', 24000.00, 'Pending'),
(409, 301, '2026-02-05', 18000.00, 'Paid'),
(410, 302, '2026-03-05', 22000.00, 'Paid');


-- ============================================
-- 6. MAINTENANCE_REQUEST: 10 records
-- ============================================
INSERT INTO Maintenance_Request
(Request_ID, Property_ID, Description, Request_Date, Status)
VALUES
(501, 101, 'Kitchen tap is leaking', '2026-01-10', 'Completed'),
(502, 102, 'Air conditioner not cooling', '2026-02-12', 'In Progress'),
(503, 103, 'Bathroom pipe leakage', '2026-01-20', 'Completed'),
(504, 104, 'Electrical switch needs repair', '2026-03-15', 'Pending'),
(505, 105, 'Shop shutter is stuck', '2026-01-18', 'Completed'),
(506, 106, 'Wall requires repainting', '2026-04-12', 'Pending'),
(507, 107, 'Water heater not working', '2026-04-20', 'In Progress'),
(508, 108, 'Ceiling leakage during rain', '2026-05-02', 'In Progress'),
(509, 109, 'Office lighting repair', '2026-05-10', 'Pending'),
(510, 110, 'Door lock replacement', '2026-05-15', 'Completed');


-- ============================================
-- 7. CONTRACTOR: 10 records
-- ============================================
INSERT INTO Contractor
(Contractor_ID, Contractor_Name, Phone, Email, Specialization)
VALUES
(601, 'Amit Plumbing Services', '9822200001', 'amit.plumbing@gmail.com', 'Plumbing'),
(602, 'CoolCare Solutions', '9822200002', 'coolcare@gmail.com', 'Air Conditioning'),
(603, 'Ramesh Pipe Works', '9822200003', 'ramesh.pipe@gmail.com', 'Plumbing'),
(604, 'Spark Electricals', '9822200004', 'spark.electrical@gmail.com', 'Electrical'),
(605, 'ShutterFix Services', '9822200005', 'shutterfix@gmail.com', 'Shutter Repair'),
(606, 'ColorCraft Painters', '9822200006', 'colorcraft@gmail.com', 'Painting'),
(607, 'HeatPro Repairs', '9822200007', 'heatpro@gmail.com', 'Water Heaters'),
(608, 'RoofCare Services', '9822200008', 'roofcare@gmail.com', 'Waterproofing'),
(609, 'BrightLine Electricals', '9822200009', 'brightline@gmail.com', 'Electrical'),
(610, 'SecureLock Services', '9822200010', 'securelock@gmail.com', 'Lock Repair');


-- ============================================
-- 8. WORK_ORDER: 10 records
-- ============================================
INSERT INTO Work_Order
(WorkOrder_ID, Request_ID, Assignment_Date, Status)
VALUES
(701, 501, '2026-01-11', 'Completed'),
(702, 502, '2026-02-13', 'In Progress'),
(703, 503, '2026-01-21', 'Completed'),
(704, 504, '2026-03-16', 'Assigned'),
(705, 505, '2026-01-19', 'Completed'),
(706, 506, '2026-04-13', 'Assigned'),
(707, 507, '2026-04-21', 'In Progress'),
(708, 508, '2026-05-03', 'In Progress'),
(709, 509, '2026-05-11', 'Assigned'),
(710, 510, '2026-05-16', 'Completed');


-- ============================================
-- 9. WORK_ORDER_CONTRACTOR: 10 records
-- ============================================
INSERT INTO Work_Order_Contractor
(WorkOrder_ID, Contractor_ID)
VALUES
(701, 601),
(702, 602),
(703, 603),
(704, 604),
(705, 605),
(706, 606),
(707, 607),
(708, 608),
(709, 609),
(710, 610);


-- ============================================
-- 10. ROLE: 10 records
-- ============================================
INSERT INTO Role
(Role_ID, Role_Name, Description, Status)
VALUES
(801, 'Admin', 'Manages the complete system', 'Active'),
(802, 'Property Manager', 'Manages properties and agreements', 'Active'),
(803, 'Accountant', 'Manages rent and payment records', 'Active'),
(804, 'Maintenance Manager', 'Coordinates maintenance work', 'Active'),
(805, 'Support Staff', 'Handles support requests', 'Active'),
(806, 'Owner', 'Views owned properties and reports', 'Active'),
(807, 'Tenant', 'Views rental and payment information', 'Active'),
(808, 'Auditor', 'Reviews records and reports', 'Active'),
(809, 'Receptionist', 'Handles initial property enquiries', 'Active'),
(810, 'System Operator', 'Performs routine system operations', 'Active');


-- ============================================
-- 11. USER: 10 records
-- ============================================
-- Password_Hash values below are demonstration
-- placeholders, NOT real password hashes.
INSERT INTO `User`
(User_ID, Role_ID, Username, Password_Hash, Account_Status)
VALUES
(901, 801, 'admin01', 'DEMO_HASH_901', 'Active'),
(902, 802, 'manager01', 'DEMO_HASH_902', 'Active'),
(903, 803, 'accountant01', 'DEMO_HASH_903', 'Active'),
(904, 804, 'maintenance01', 'DEMO_HASH_904', 'Active'),
(905, 805, 'support01', 'DEMO_HASH_905', 'Active'),
(906, 806, 'owner01', 'DEMO_HASH_906', 'Active'),
(907, 807, 'tenant01', 'DEMO_HASH_907', 'Active'),
(908, 808, 'auditor01', 'DEMO_HASH_908', 'Active'),
(909, 809, 'reception01', 'DEMO_HASH_909', 'Active'),
(910, 810, 'operator01', 'DEMO_HASH_910', 'Active');


-- ============================================
-- VERIFY RECORD COUNTS
-- ============================================
SELECT 'Owner' AS Table_Name, COUNT(*) AS Total_Records FROM Owner
UNION ALL
SELECT 'Property', COUNT(*) FROM Property
UNION ALL
SELECT 'Tenant', COUNT(*) FROM Tenant
UNION ALL
SELECT 'Rental_Agreement', COUNT(*) FROM Rental_Agreement
UNION ALL
SELECT 'Payment', COUNT(*) FROM Payment
UNION ALL
SELECT 'Maintenance_Request', COUNT(*) FROM Maintenance_Request
UNION ALL
SELECT 'Contractor', COUNT(*) FROM Contractor
UNION ALL
SELECT 'Work_Order', COUNT(*) FROM Work_Order
UNION ALL
SELECT 'Work_Order_Contractor', COUNT(*) FROM Work_Order_Contractor
UNION ALL
SELECT 'Role', COUNT(*) FROM Role
UNION ALL
SELECT 'User', COUNT(*) FROM `User`;

/*
FUNCTIONS
*/
SELECT total_rent_paid(201);

SELECT remaining_rent(301);

SELECT count_owner_properties(1);

SELECT count_pending_requests(106);

SELECT count_contractor_orders(601);

/*
procedure
*/

CALL property_details();

CALL tenant_rental_details('Aditya Rao');

CALL payment_details(301);

CALL pending_maintenance();

CALL contractor_work_details(601);


/*
triggers
*/


USE EstateEase;

-- ============================================
-- TRIGGER 1: Log property status updates
-- ============================================

CREATE TABLE Property_Log (
    Log_ID INT AUTO_INCREMENT PRIMARY KEY,
    Property_ID INT,
    Old_Status VARCHAR(30),
    New_Status VARCHAR(30),
    Changed_At TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DELIMITER //

CREATE TRIGGER update_property_status
AFTER UPDATE ON Property
FOR EACH ROW
BEGIN
    IF NOT (OLD.Status <=> NEW.Status) THEN
        INSERT INTO Property_Log
            (Property_ID, Old_Status, New_Status)
        VALUES
            (OLD.Property_ID, OLD.Status, NEW.Status);
    END IF;
END //

DELIMITER ;

-- Test Trigger 1
UPDATE Property
SET Status = 'Available'
WHERE Property_ID = 101;

SELECT * FROM Property_Log;


-- ============================================
-- TRIGGER 2: Log new payment records
-- ============================================

CREATE TABLE Payment_Log (
    Log_ID INT AUTO_INCREMENT PRIMARY KEY,
    Payment_ID INT,
    Agreement_ID INT,
    Amount DECIMAL(10,2),
    Payment_Status VARCHAR(30),
    Created_At TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DELIMITER //

CREATE TRIGGER after_payment_insert
AFTER INSERT ON Payment
FOR EACH ROW
BEGIN
    INSERT INTO Payment_Log
        (Payment_ID, Agreement_ID, Amount, Payment_Status)
    VALUES
        (NEW.Payment_ID, NEW.Agreement_ID,
         NEW.Amount, NEW.Payment_Status);
END //

DELIMITER ;

-- Test Trigger 2
INSERT INTO Payment
    (Agreement_ID, Payment_Date, Amount, Payment_Status)
VALUES
    (303, '2026-06-05', 25000.00, 'Paid');

SELECT * FROM Payment_Log;


-- ============================================
-- TRIGGER 3: Log maintenance status updates
-- ============================================

CREATE TABLE Maintenance_Log (
    Log_ID INT AUTO_INCREMENT PRIMARY KEY,
    Request_ID INT,
    Old_Status VARCHAR(30),
    New_Status VARCHAR(30),
    Changed_At TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DELIMITER //

CREATE TRIGGER update_maintenance_status
AFTER UPDATE ON Maintenance_Request
FOR EACH ROW
BEGIN
    IF NOT (OLD.Status <=> NEW.Status) THEN
        INSERT INTO Maintenance_Log
            (Request_ID, Old_Status, New_Status)
        VALUES
            (OLD.Request_ID, OLD.Status, NEW.Status);
    END IF;
END //

DELIMITER ;

-- Test Trigger 3
UPDATE Maintenance_Request
SET Status = 'Completed'
WHERE Request_ID = 504;

SELECT * FROM Maintenance_Log;


-- ============================================
-- TRIGGER 4: Log work order status updates
-- ============================================

CREATE TABLE Work_Order_Log (
    Log_ID INT AUTO_INCREMENT PRIMARY KEY,
    WorkOrder_ID INT,
    Old_Status VARCHAR(30),
    New_Status VARCHAR(30),
    Changed_At TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DELIMITER //

CREATE TRIGGER update_work_order_status
AFTER UPDATE ON Work_Order
FOR EACH ROW
BEGIN
    IF NOT (OLD.Status <=> NEW.Status) THEN
        INSERT INTO Work_Order_Log
            (WorkOrder_ID, Old_Status, New_Status)
        VALUES
            (OLD.WorkOrder_ID, OLD.Status, NEW.Status);
    END IF;
END //

DELIMITER ;

-- Test Trigger 4
UPDATE Work_Order
SET Status = 'Completed'
WHERE WorkOrder_ID = 704;

SELECT * FROM Work_Order_Log;



-- Check all logs
SELECT * FROM Property_Log;
SELECT * FROM Payment_Log;
SELECT * FROM Maintenance_Log;
SELECT * FROM Work_Order_Log;


/*
10 sql queries
*/

-- Display all properties with their owner's details

SELECT
    p.Property_ID,
    p.Address,
    p.Property_Type,
    p.Status,
    o.Owner_Name,
    o.Phone
FROM Property p
INNER JOIN Owner o
    ON p.Owner_ID = o.Owner_ID;
    
-- Find the average monthly rent of all rental agreements

SELECT
    AVG(Monthly_Rent) AS Average_Monthly_Rent
FROM Rental_Agreement;

-- Count the properties owned by each owner

SELECT
    o.Owner_ID,
    o.Owner_Name,
    COUNT(p.Property_ID) AS Total_Properties
FROM Owner o
LEFT JOIN Property p
    ON o.Owner_ID = p.Owner_ID
GROUP BY o.Owner_ID, o.Owner_Name;

-- Display tenants with their rental agreement details

SELECT
    t.Tenant_Name,
    p.Address,
    ra.Agreement_ID,
    ra.Monthly_Rent,
    ra.Start_Date,
    ra.End_Date
FROM Tenant t
INNER JOIN Rental_Agreement ra
    ON t.Tenant_ID = ra.Tenant_ID
INNER JOIN Property p
    ON ra.Property_ID = p.Property_ID;
    
-- Calculate the total successful payments for each rental agreement

SELECT
    ra.Agreement_ID,
    ra.Monthly_Rent,
    COALESCE(SUM(p.Amount), 0) AS Total_Paid
FROM Rental_Agreement ra
LEFT JOIN Payment p
    ON ra.Agreement_ID = p.Agreement_ID
    AND p.Payment_Status = 'Paid'
GROUP BY ra.Agreement_ID, ra.Monthly_Rent;


-- Display all pending maintenance requests

SELECT
    mr.Request_ID,
    p.Address,
    mr.Description,
    mr.Request_Date,
    mr.Status
FROM Maintenance_Request mr
INNER JOIN Property p
    ON mr.Property_ID = p.Property_ID
WHERE mr.Status = 'Pending'
ORDER BY mr.Request_Date;

-- Find contractors and the number of work orders assigned to them

SELECT
    c.Contractor_ID,
    c.Contractor_Name,
    c.Specialization,
    COUNT(wc.WorkOrder_ID) AS Total_Work_Orders
FROM Contractor c
LEFT JOIN Work_Order_Contractor wc
    ON c.Contractor_ID = wc.Contractor_ID
GROUP BY
    c.Contractor_ID,
    c.Contractor_Name,
    c.Specialization;
    
-- Find properties whose monthly rent is greater than the average rent

SELECT
    Agreement_ID,
    Property_ID,
    Tenant_ID,
    Monthly_Rent
FROM Rental_Agreement
WHERE Monthly_Rent > (
    SELECT AVG(Monthly_Rent)
    FROM Rental_Agreement
);

CREATE OR REPLACE VIEW Active_Rental_View AS
SELECT
    ra.Agreement_ID,
    t.Tenant_Name,
    p.Address,
    ra.Monthly_Rent,
    ra.Start_Date,
    ra.End_Date
FROM Rental_Agreement ra
INNER JOIN Tenant t
    ON ra.Tenant_ID = t.Tenant_ID
INNER JOIN Property p
    ON ra.Property_ID = p.Property_ID
WHERE ra.Status = 'Active';

-- Create a view for active rental agreements

SELECT * FROM Active_Rental_View;


-- Display work orders with contractor and maintenance details

SELECT
    wo.WorkOrder_ID,
    mr.Description,
    p.Address,
    c.Contractor_Name,
    c.Specialization,
    wo.Assignment_Date,
    wo.Status
FROM Work_Order wo
INNER JOIN Maintenance_Request mr
    ON wo.Request_ID = mr.Request_ID
INNER JOIN Property p
    ON mr.Property_ID = p.Property_ID
INNER JOIN Work_Order_Contractor wc
    ON wo.WorkOrder_ID = wc.WorkOrder_ID
INNER JOIN Contractor c
    ON wc.Contractor_ID = c.Contractor_ID;










