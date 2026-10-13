USE EstateEase;

INSERT INTO Property_Type (Type_Name) VALUES 
('1 BHK Apartment'), 
('2 BHK Apartment'), 
('Row House'), 
('Commercial Office');

INSERT INTO Owner (Full_Name, Phone, Email, City) VALUES
('Aarav Sharma', '9876500001', 'aarav@gmail.com', 'Pune'),
('Priya Patel', '9876500002', 'priya@gmail.com', 'Pune');

INSERT INTO Tenant (Full_Name, Phone, Email, City) VALUES
('Aditya Rao', '9811100001', 'aditya@gmail.com', 'Pune'),
('Meera Nair', '9811100002', 'meera@gmail.com', 'Pune');

INSERT INTO Property (Owner_ID, Type_ID, Address, Area, City, Monthly_Rent, Status) VALUES
(1, 2, 'Flat 101, Green Heights', 'Kothrud', 'Pune', 22000.00, 'Available'),
(2, 3, 'Row House 12, Blue Valley', 'Wakad', 'Pune', 30000.00, 'Available');

INSERT INTO Contractor (Contractor_Name, Specialization, Phone, Charges_Per_Job) VALUES
('Amit Plumbing Works', 'Plumbing', '9822200001', 800.00),
('Spark Electricals', 'Electrical', '9822200004', 600.00);