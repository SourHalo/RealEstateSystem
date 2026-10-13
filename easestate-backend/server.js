const express = require('express');
const cors = require('cors');
const pool = require('./db');
require('dotenv').config();

const app = express();
app.use(cors());
app.use(express.json());

// -------------------------------------------------------------
// 1. BASE PROPERTIES & FILTERING
// -------------------------------------------------------------
app.get('/api/properties', async (req, res) => {
  try {
    const { type, status } = req.query;
    let query = `
      SELECT 
        p.Property_ID, p.Address, p.Property_Type, p.Status, 
        o.Owner_Name, o.Phone, o.Email
      FROM Property p
      INNER JOIN Owner o ON p.Owner_ID = o.Owner_ID
      WHERE 1=1
    `;
    const params = [];
    if (type && type !== 'All') {
      query += ' AND p.Property_Type = ?';
      params.push(type);
    }
    if (status && status !== 'All') {
      query += ' AND p.Status = ?';
      params.push(status);
    }
    query += ' ORDER BY p.Property_ID DESC';

    const [rows] = await pool.query(query, params);
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// -------------------------------------------------------------
// 2. ACTIVE RENTALS (VIEW) & CONTRACTORS
// -------------------------------------------------------------
app.get('/api/agreements/active', async (req, res) => {
  try {
    const [rows] = await pool.query('SELECT * FROM Active_Rental_View');
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/contractors', async (req, res) => {
  try {
    const [rows] = await pool.query('SELECT Contractor_ID, Contractor_Name, Specialty, Phone FROM Contractor ORDER BY Contractor_Name');
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// -------------------------------------------------------------
// 3. ALL STORED PROCEDURES (From your SQL script)
// -------------------------------------------------------------

// Proc 1: property_details()
app.get('/api/procedures/property-details', async (req, res) => {
  try {
    const [result] = await pool.query('CALL property_details()');
    res.json(Array.isArray(result[0]) ? result[0] : result);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Proc 2: tenant_rental_details(name)
app.get('/api/procedures/tenant/:name', async (req, res) => {
  try {
    const [result] = await pool.query('CALL tenant_rental_details(?)', [req.params.name]);
    res.json(Array.isArray(result[0]) ? result[0] : result);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Proc 3: payment_details(agreement_id)
app.get('/api/procedures/payment/:agreementId', async (req, res) => {
  try {
    const [result] = await pool.query('CALL payment_details(?)', [req.params.agreementId]);
    res.json(Array.isArray(result[0]) ? result[0] : result);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Proc 4: pending_maintenance()
app.get('/api/procedures/pending-maintenance', async (req, res) => {
  try {
    const [result] = await pool.query('CALL pending_maintenance()');
    res.json(Array.isArray(result[0]) ? result[0] : result);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Proc 5: contractor_work_details(contractor_id)
app.get('/api/procedures/contractor/:contractorId', async (req, res) => {
  try {
    const [result] = await pool.query('CALL contractor_work_details(?)', [req.params.contractorId]);
    res.json(Array.isArray(result[0]) ? result[0] : result);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// -------------------------------------------------------------
// 4. DATABASE STORED FUNCTIONS & AGGREGATE CALCULATIONS
// -------------------------------------------------------------
app.get('/api/functions/metrics', async (req, res) => {
  try {
    // Queries total revenue, pending repair counts, and active agreements
    const [rev] = await pool.query('SELECT IFNULL(SUM(Amount), 0) AS totalRevenue FROM Payment');
    const [rentSum] = await pool.query('SELECT IFNULL(SUM(Monthly_Rent), 0) AS monthlyCashflow FROM Rental_Agreement WHERE Status = "Active"');
    const [pendingCount] = await pool.query('SELECT COUNT(*) AS totalPending FROM Maintenance_Request WHERE Status != "Completed"');
    const [availableCount] = await pool.query('SELECT COUNT(*) AS totalAvailable FROM Property WHERE Status = "Available"');

    res.json({
      totalRevenue: rev[0].totalRevenue,
      monthlyCashflow: rentSum[0].monthlyCashflow,
      totalPending: pendingCount[0].totalPending,
      totalAvailable: availableCount[0].totalAvailable
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// -------------------------------------------------------------
// 5. ACTIONS THAT TRIGGER DATABASE TRIGGERS
// -------------------------------------------------------------

// Action A (Fires Maintenance Insert / Log Trigger)
// -------------------------------------------------------------
// TRIGGER ACTION 1: Update Property Status (Fires update_property_status)
// -------------------------------------------------------------
app.post('/api/triggers/toggle-property-status', async (req, res) => {
  const { property_id, new_status } = req.body;
  try {
    const [result] = await pool.query(
      'UPDATE Property SET Status = ? WHERE Property_ID = ?',
      [new_status, property_id]
    );

    if (result.changedRows === 0) {
      return res.json({ 
        message: `Property #${property_id} was already '${new_status}'. Status did not change, so no log was generated.` 
      });
    }

    res.json({ message: `Property #${property_id} updated to '${new_status}'. Trigger update_property_status executed!` });
  } catch (err) {
    console.error('Trigger error:', err);
    res.status(500).json({ error: err.message });
  }
});

// -------------------------------------------------------------
// TRIGGER ACTION 2: Insert Payment (Fires after_payment_insert)
// -------------------------------------------------------------
app.post('/api/triggers/record-payment', async (req, res) => {
  const { agreement_id, amount, payment_status } = req.body;
  try {
    const today = new Date().toISOString().split('T')[0];
    const [result] = await pool.query(
      'INSERT INTO Payment (Agreement_ID, Payment_Date, Amount, Payment_Status) VALUES (?, ?, ?, ?)',
      [agreement_id, amount, today, payment_status || 'Paid']
    );

    res.json({ 
      message: `Payment of ₹${amount} inserted! Trigger after_payment_insert executed!`, 
      paymentId: result.insertId 
    });
  } catch (err) {
    console.error('Payment Trigger Error:', err);
    res.status(500).json({ error: err.message });
  }
});

// -------------------------------------------------------------
// FETCH ALL 4 TRIGGER LOG TABLES
// -------------------------------------------------------------
app.get('/api/logs', async (req, res) => {
  try {
    const [propertyLogs] = await pool.query('SELECT * FROM Property_Log ORDER BY Changed_At DESC LIMIT 6');
    const [paymentLogs] = await pool.query('SELECT * FROM Payment_Log ORDER BY Created_At DESC LIMIT 6');
    const [maintenanceLogs] = await pool.query('SELECT * FROM Maintenance_Log ORDER BY Changed_At DESC LIMIT 6');
    const [workOrderLogs] = await pool.query('SELECT * FROM Work_Order_Log ORDER BY Changed_At DESC LIMIT 6');

    res.json({ propertyLogs, paymentLogs, maintenanceLogs, workOrderLogs });
  } catch (err) {
    console.error('Log fetch error:', err);
    res.status(500).json({ error: err.message });
  }
});

const PORT = process.env.PORT || 5000;
app.listen(PORT, () => {
  console.log(`EstateEase API listening on http://localhost:${PORT}`);
});