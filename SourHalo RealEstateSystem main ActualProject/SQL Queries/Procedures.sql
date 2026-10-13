USE EstateEase;

DELIMITER //

DROP PROCEDURE IF EXISTS sp_book_property //
DROP PROCEDURE IF EXISTS sp_assign_work_order //
DROP PROCEDURE IF EXISTS sp_complete_work_order //
DROP PROCEDURE IF EXISTS sp_terminate_agreement //
DROP PROCEDURE IF EXISTS sp_record_payment //
DROP PROCEDURE IF EXISTS sp_expire_agreements //

-- Procedure 1: Book a property (agreement + status change, atomically)
-- p_rent may be NULL, in which case the property's Monthly_Rent is used.
CREATE PROCEDURE sp_book_property(
    IN  p_property_id INT,
    IN  p_tenant_id   INT,
    IN  p_start_date  DATE,
    IN  p_end_date    DATE,
    IN  p_rent        DECIMAL(10,2),
    OUT p_agreement_id INT
)
BEGIN
    DECLARE v_status VARCHAR(20)  DEFAULT NULL;
    DECLARE v_rent   DECIMAL(10,2) DEFAULT NULL;
    DECLARE v_tenant_count INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    -- Input validation
    IF p_property_id IS NULL OR p_tenant_id IS NULL
       OR p_start_date IS NULL OR p_end_date IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Property, tenant, start date and end date are required';
    END IF;

    IF p_end_date <= p_start_date THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'End date must be after start date';
    END IF;

    IF p_rent IS NOT NULL AND p_rent <= 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Rent must be greater than zero';
    END IF;

    START TRANSACTION;

    -- Lock the property row so two bookings cannot race
    SELECT Status, Monthly_Rent INTO v_status, v_rent
    FROM Property
    WHERE Property_ID = p_property_id
    FOR UPDATE;

    IF v_status IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Property does not exist';
    END IF;

    IF v_status <> 'Available' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Property is not available for booking';
    END IF;

    SELECT COUNT(*) INTO v_tenant_count FROM Tenant WHERE Tenant_ID = p_tenant_id;
    IF v_tenant_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Tenant does not exist';
    END IF;

    INSERT INTO Rental_Agreement
        (Property_ID, Tenant_ID, Start_Date, End_Date, Rent_Amount, Agreement_Status)
    VALUES
        (p_property_id, p_tenant_id, p_start_date, p_end_date, COALESCE(p_rent, v_rent), 'Active');

    SET p_agreement_id = LAST_INSERT_ID();

    UPDATE Property SET Status = 'Rented' WHERE Property_ID = p_property_id;

    COMMIT;
END //

-- Procedure 2: Assign a work order
-- p_estimated_cost may be NULL, in which case the contractor's Charges_Per_Job is used.
-- Property status becomes 'Under Maintenance' only if it was 'Available';
-- a rented property stays 'Rented'.
CREATE PROCEDURE sp_assign_work_order(
    IN  p_property_id    INT,
    IN  p_contractor_id  INT,
    IN  p_issue          VARCHAR(255),
    IN  p_date           DATE,
    IN  p_estimated_cost DECIMAL(10,2),
    OUT p_work_order_id  INT
)
BEGIN
    DECLARE v_status  VARCHAR(20)  DEFAULT NULL;
    DECLARE v_charges DECIMAL(10,2) DEFAULT NULL;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF p_property_id IS NULL OR p_contractor_id IS NULL OR p_date IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Property, contractor and date are required';
    END IF;

    IF p_issue IS NULL OR TRIM(p_issue) = '' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Issue description cannot be empty';
    END IF;

    IF p_estimated_cost IS NOT NULL AND p_estimated_cost < 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cost cannot be negative';
    END IF;

    START TRANSACTION;

    SELECT Status INTO v_status
    FROM Property
    WHERE Property_ID = p_property_id
    FOR UPDATE;

    IF v_status IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Property does not exist';
    END IF;

    SELECT Charges_Per_Job INTO v_charges
    FROM Contractor
    WHERE Contractor_ID = p_contractor_id;

    IF v_charges IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Contractor does not exist';
    END IF;

    INSERT INTO Maintenance_Work_Order
        (Property_ID, Contractor_ID, Issue_Description, Request_Date, Cost, Status)
    VALUES
        (p_property_id, p_contractor_id, TRIM(p_issue), p_date, COALESCE(p_estimated_cost, v_charges), 'Assigned');

    SET p_work_order_id = LAST_INSERT_ID();

    IF v_status = 'Available' THEN
        UPDATE Property SET Status = 'Under Maintenance' WHERE Property_ID = p_property_id;
    END IF;

    COMMIT;
END //

-- Procedure 3: Complete a work order
-- The property is released only if it is 'Under Maintenance' and this was
-- its last open work order.
CREATE PROCEDURE sp_complete_work_order(
    IN p_work_order_id INT,
    IN p_final_cost    DECIMAL(10,2)   -- NULL keeps the existing cost
)
BEGIN
    DECLARE v_prop_id   INT DEFAULT NULL;
    DECLARE v_wo_status VARCHAR(20) DEFAULT NULL;
    DECLARE v_open_count INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF p_work_order_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Work order ID is required';
    END IF;

    IF p_final_cost IS NOT NULL AND p_final_cost < 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cost cannot be negative';
    END IF;

    START TRANSACTION;

    SELECT Property_ID, Status INTO v_prop_id, v_wo_status
    FROM Maintenance_Work_Order
    WHERE WorkOrder_ID = p_work_order_id
    FOR UPDATE;

    IF v_prop_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Work order does not exist';
    END IF;

    IF v_wo_status = 'Completed' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Work order is already completed';
    END IF;

    UPDATE Maintenance_Work_Order
    SET Status = 'Completed',
        Cost   = COALESCE(p_final_cost, Cost)
    WHERE WorkOrder_ID = p_work_order_id;

    SELECT COUNT(*) INTO v_open_count
    FROM Maintenance_Work_Order
    WHERE Property_ID = v_prop_id AND Status <> 'Completed';

    IF v_open_count = 0 THEN
        UPDATE Property
        SET Status = 'Available'
        WHERE Property_ID = v_prop_id AND Status = 'Under Maintenance';
    END IF;

    COMMIT;
END //

-- Procedure 4: Terminate (or expire) an active agreement
-- The trg_agreement_expiry trigger frees the property.
CREATE PROCEDURE sp_terminate_agreement(
    IN p_agreement_id INT,
    IN p_new_status   VARCHAR(20)   -- 'Terminated' or 'Expired'
)
BEGIN
    DECLARE v_status VARCHAR(20) DEFAULT NULL;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF p_agreement_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Agreement ID is required';
    END IF;

    IF p_new_status IS NULL OR p_new_status NOT IN ('Terminated', 'Expired') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'New status must be Terminated or Expired';
    END IF;

    START TRANSACTION;

    SELECT Agreement_Status INTO v_status
    FROM Rental_Agreement
    WHERE Agreement_ID = p_agreement_id
    FOR UPDATE;

    IF v_status IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Agreement does not exist';
    END IF;

    IF v_status <> 'Active' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Only active agreements can be ended';
    END IF;

    UPDATE Rental_Agreement
    SET Agreement_Status = p_new_status
    WHERE Agreement_ID = p_agreement_id;

    COMMIT;
END //

-- Procedure 5: Record a rent payment
CREATE PROCEDURE sp_record_payment(
    IN  p_agreement_id INT,
    IN  p_date         DATE,
    IN  p_amount       DECIMAL(10,2),
    IN  p_mode         VARCHAR(20),
    IN  p_status       VARCHAR(20),   -- 'Paid', 'Pending' or 'Failed'
    OUT p_payment_id   INT
)
BEGIN
    DECLARE v_agr_status VARCHAR(20) DEFAULT NULL;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF p_agreement_id IS NULL OR p_date IS NULL OR p_amount IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Agreement, date and amount are required';
    END IF;

    IF p_amount <= 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Amount must be greater than zero';
    END IF;

    START TRANSACTION;

    SELECT Agreement_Status INTO v_agr_status
    FROM Rental_Agreement
    WHERE Agreement_ID = p_agreement_id
    FOR UPDATE;

    IF v_agr_status IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Agreement does not exist';
    END IF;

    IF v_agr_status <> 'Active' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Payments can only be recorded for active agreements';
    END IF;

    INSERT INTO Payment (Agreement_ID, Payment_Date, Amount, Payment_Mode, Payment_Status)
    VALUES (p_agreement_id, p_date, p_amount,
            COALESCE(p_mode, 'UPI'), COALESCE(p_status, 'Paid'));

    SET p_payment_id = LAST_INSERT_ID();

    COMMIT;
END //

-- Procedure 6: Expire all active agreements whose End_Date has passed.
-- Call this from Java at startup/daily, or schedule it with a MySQL EVENT.
CREATE PROCEDURE sp_expire_agreements(
    OUT p_expired_count INT
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    UPDATE Rental_Agreement
    SET Agreement_Status = 'Expired'
    WHERE Agreement_Status = 'Active' AND End_Date < CURDATE();

    SET p_expired_count = ROW_COUNT();

    COMMIT;
END //

DELIMITER ;