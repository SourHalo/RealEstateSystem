USE EstateEase;

DELIMITER //

DROP TRIGGER IF EXISTS trg_property_status_audit //
DROP TRIGGER IF EXISTS trg_agreement_before_insert //
DROP TRIGGER IF EXISTS trg_agreement_before_update //
DROP TRIGGER IF EXISTS trg_agreement_expiry //

-- Trigger 1: Log every property status change
CREATE TRIGGER trg_property_status_audit
AFTER UPDATE ON Property
FOR EACH ROW
BEGIN
    IF NOT (OLD.Status <=> NEW.Status) THEN
        INSERT INTO Property_Status_Log (Property_ID, Old_Status, New_Status)
        VALUES (OLD.Property_ID, OLD.Status, NEW.Status);
    END IF;
END //

-- Trigger 2: Block overlapping active agreements on the same property
CREATE TRIGGER trg_agreement_before_insert
BEFORE INSERT ON Rental_Agreement
FOR EACH ROW
BEGIN
    IF NEW.Agreement_Status = 'Active' AND EXISTS (
        SELECT 1 FROM Rental_Agreement
        WHERE Property_ID = NEW.Property_ID
          AND Agreement_Status = 'Active'
          AND NEW.Start_Date <= End_Date
          AND NEW.End_Date >= Start_Date
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Property already has an active agreement overlapping these dates';
    END IF;
END //

-- Trigger 3: Terminated/Expired agreements cannot be reactivated
CREATE TRIGGER trg_agreement_before_update
BEFORE UPDATE ON Rental_Agreement
FOR EACH ROW
BEGIN
    IF OLD.Agreement_Status IN ('Terminated', 'Expired')
       AND NEW.Agreement_Status = 'Active' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'A terminated or expired agreement cannot be reactivated';
    END IF;
END //

-- Trigger 4: Free the property when its agreement ends, but only if it is
-- currently Rented and has no other active agreement
CREATE TRIGGER trg_agreement_expiry
AFTER UPDATE ON Rental_Agreement
FOR EACH ROW
BEGIN
    IF OLD.Agreement_Status = 'Active'
       AND NEW.Agreement_Status IN ('Terminated', 'Expired') THEN

        IF NOT EXISTS (
            SELECT 1 FROM Rental_Agreement
            WHERE Property_ID = NEW.Property_ID
              AND Agreement_Status = 'Active'
        ) THEN
            UPDATE Property
            SET Status = 'Available'
            WHERE Property_ID = NEW.Property_ID
              AND Status = 'Rented';
        END IF;
    END IF;
END //

DELIMITER ;