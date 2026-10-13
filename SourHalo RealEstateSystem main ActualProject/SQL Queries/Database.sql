DROP DATABASE IF EXISTS EstateEase;
CREATE DATABASE EstateEase CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE EstateEase;

-- 1. Property Categories (Lookup)
CREATE TABLE Property_Type (
    Type_ID INT AUTO_INCREMENT PRIMARY KEY,
    Type_Name VARCHAR(50) NOT NULL UNIQUE,
    CONSTRAINT chk_type_name CHECK (TRIM(Type_Name) <> '')
);

-- 2. Owners
CREATE TABLE Owner (
    Owner_ID INT AUTO_INCREMENT PRIMARY KEY,
    Full_Name VARCHAR(100) NOT NULL,
    Phone VARCHAR(15) NOT NULL,
    Email VARCHAR(100) UNIQUE,
    City VARCHAR(50) DEFAULT 'Pune',
    CONSTRAINT chk_owner_name  CHECK (TRIM(Full_Name) <> ''),
    CONSTRAINT chk_owner_phone CHECK (Phone REGEXP '^[+]?[0-9]{10,14}$'),
    CONSTRAINT chk_owner_email CHECK (Email IS NULL OR Email LIKE '%_@_%._%')
);

-- 3. Tenants
CREATE TABLE Tenant (
    Tenant_ID INT AUTO_INCREMENT PRIMARY KEY,
    Full_Name VARCHAR(100) NOT NULL,
    Phone VARCHAR(15) NOT NULL,
    Email VARCHAR(100) UNIQUE,
    City VARCHAR(50) DEFAULT 'Pune',
    CONSTRAINT chk_tenant_name  CHECK (TRIM(Full_Name) <> ''),
    CONSTRAINT chk_tenant_phone CHECK (Phone REGEXP '^[+]?[0-9]{10,14}$'),
    CONSTRAINT chk_tenant_email CHECK (Email IS NULL OR Email LIKE '%_@_%._%')
);

-- 4. Properties
CREATE TABLE Property (
    Property_ID INT AUTO_INCREMENT PRIMARY KEY,
    Owner_ID INT NOT NULL,
    Type_ID INT NOT NULL,
    Address VARCHAR(255) NOT NULL,
    Area VARCHAR(50) NOT NULL,
    City VARCHAR(50) DEFAULT 'Pune',
    Monthly_Rent DECIMAL(10,2) NOT NULL,
    Status VARCHAR(20) NOT NULL DEFAULT 'Available',
    CONSTRAINT fk_property_owner FOREIGN KEY (Owner_ID) REFERENCES Owner(Owner_ID) ON DELETE RESTRICT,
    CONSTRAINT fk_property_type  FOREIGN KEY (Type_ID)  REFERENCES Property_Type(Type_ID),
    CONSTRAINT chk_property_rent   CHECK (Monthly_Rent > 0),
    CONSTRAINT chk_property_status CHECK (Status IN ('Available', 'Rented', 'Under Maintenance')),
    INDEX idx_property_search (Status, Area)
);

-- 5. Rental Agreements
CREATE TABLE Rental_Agreement (
    Agreement_ID INT AUTO_INCREMENT PRIMARY KEY,
    Property_ID INT NOT NULL,
    Tenant_ID INT NOT NULL,
    Start_Date DATE NOT NULL,
    End_Date DATE NOT NULL,
    Rent_Amount DECIMAL(10,2) NOT NULL,
    Agreement_Status VARCHAR(20) NOT NULL DEFAULT 'Active',
    CONSTRAINT fk_agreement_property FOREIGN KEY (Property_ID) REFERENCES Property(Property_ID),
    CONSTRAINT fk_agreement_tenant   FOREIGN KEY (Tenant_ID)   REFERENCES Tenant(Tenant_ID),
    CONSTRAINT chk_agreement_dates  CHECK (End_Date > Start_Date),
    CONSTRAINT chk_agreement_rent   CHECK (Rent_Amount > 0),
    CONSTRAINT chk_agreement_status CHECK (Agreement_Status IN ('Active', 'Terminated', 'Expired')),
    INDEX idx_agreement_property_status (Property_ID, Agreement_Status)
);

-- 6. Payments
CREATE TABLE Payment (
    Payment_ID INT AUTO_INCREMENT PRIMARY KEY,
    Agreement_ID INT NOT NULL,
    Payment_Date DATE NOT NULL,
    Amount DECIMAL(10,2) NOT NULL,
    Payment_Mode VARCHAR(20) NOT NULL DEFAULT 'UPI',
    Payment_Status VARCHAR(20) NOT NULL DEFAULT 'Paid',
    CONSTRAINT fk_payment_agreement FOREIGN KEY (Agreement_ID) REFERENCES Rental_Agreement(Agreement_ID),
    CONSTRAINT chk_payment_amount CHECK (Amount > 0),
    CONSTRAINT chk_payment_mode   CHECK (Payment_Mode IN ('UPI', 'Card', 'Net Banking', 'Cash')),
    CONSTRAINT chk_payment_status CHECK (Payment_Status IN ('Paid', 'Pending', 'Failed'))
);

-- 7. Maintenance Contractors
CREATE TABLE Contractor (
    Contractor_ID INT AUTO_INCREMENT PRIMARY KEY,
    Contractor_Name VARCHAR(100) NOT NULL,
    Specialization VARCHAR(50) NOT NULL,
    Phone VARCHAR(15) NOT NULL,
    Charges_Per_Job DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT chk_contractor_name    CHECK (TRIM(Contractor_Name) <> ''),
    CONSTRAINT chk_contractor_phone   CHECK (Phone REGEXP '^[+]?[0-9]{10,14}$'),
    CONSTRAINT chk_contractor_charges CHECK (Charges_Per_Job >= 0)
);

-- 8. Maintenance Work Orders
CREATE TABLE Maintenance_Work_Order (
    WorkOrder_ID INT AUTO_INCREMENT PRIMARY KEY,
    Property_ID INT NOT NULL,
    Contractor_ID INT NOT NULL,
    Issue_Description VARCHAR(255) NOT NULL,
    Request_Date DATE NOT NULL,
    Cost DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    Status VARCHAR(20) NOT NULL DEFAULT 'Assigned',
    CONSTRAINT fk_wo_property   FOREIGN KEY (Property_ID)   REFERENCES Property(Property_ID),
    CONSTRAINT fk_wo_contractor FOREIGN KEY (Contractor_ID) REFERENCES Contractor(Contractor_ID),
    CONSTRAINT chk_wo_issue  CHECK (TRIM(Issue_Description) <> ''),
    CONSTRAINT chk_wo_cost   CHECK (Cost >= 0),
    CONSTRAINT chk_wo_status CHECK (Status IN ('Assigned', 'In Progress', 'Completed')),
    INDEX idx_wo_property_status (Property_ID, Status)
);

-- 9. Audit Log
CREATE TABLE Property_Status_Log (
    Log_ID INT AUTO_INCREMENT PRIMARY KEY,
    Property_ID INT NOT NULL,
    Old_Status VARCHAR(20),
    New_Status VARCHAR(20),
    Changed_At TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_log_property FOREIGN KEY (Property_ID) REFERENCES Property(Property_ID) ON DELETE CASCADE
);