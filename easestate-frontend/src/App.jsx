import { useState, useEffect } from 'react';
import axios from 'axios';
import PropertyCanvas from './PropertyCanvas';
import './App.css';

const LUXURY_IMAGES = [
  'https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1600566753376-12c8ab7fb75b?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1613977257363-707ba9348227?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1600585154526-990dced4db0d?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1580587771525-78b9dba3b914?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1600573472592-401b489a3cdc?auto=format&fit=crop&w=800&q=80'
];

export default function App() {
  const [activePage, setActivePage] = useState('properties');
  const [theme, setTheme] = useState('dark');
  const [properties, setProperties] = useState([]);
  const [selected3D, setSelected3D] = useState(null);

  // Filters
  const [filterType, setFilterType] = useState('All');
  const [filterStatus, setFilterStatus] = useState('All');

  // Management Portal Data
  const [agreements, setAgreements] = useState([]);
  const [pendingMaintenanceList, setPendingMaintenanceList] = useState([]);
  const [contractors, setContractors] = useState([]);
  const [selectedContractors, setSelectedContractors] = useState({});
  const [triggerLogs, setTriggerLogs] = useState({ propertyLogs: [], paymentLogs: [], maintenanceLogs: [] });
  const [metrics, setMetrics] = useState({ totalRevenue: 0, monthlyCashflow: 0, totalPending: 0, totalAvailable: 0 });

  // Stored Procedure Demo States
  const [selectedProcedure, setSelectedProcedure] = useState('tenant');
  const [procedureParam, setProcedureParam] = useState('Aditya Rao');
  const [procedureResult, setProcedureResult] = useState(null);

  // Trigger Action Demo States
  const [targetPropId, setTargetPropId] = useState('101');
  const [targetStatus, setTargetStatus] = useState('Under Maintenance');
  const [paymentAgreementId, setPaymentAgreementId] = useState('301');
  const [paymentAmount, setPaymentAmount] = useState('18000');
  const [triggerBanner, setTriggerBanner] = useState('');

  // 3D Modal Maintenance Form
  const [maintenanceDesc, setMaintenanceDesc] = useState('');
  const [maintenanceSuccess, setMaintenanceSuccess] = useState('');

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
  }, [theme]);

  // Fetch properties on filter change
  useEffect(() => {
    fetchFilteredProperties();
  }, [filterType, filterStatus]);

  const fetchFilteredProperties = async () => {
    try {
      const res = await axios.get('http://localhost:5000/api/properties', {
        params: { type: filterType, status: filterStatus }
      });
      setProperties(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  // Load management portal data
  const loadManagementData = async () => {
    try {
      const [agreementsRes, ticketsRes, contractorsRes, logsRes, metricsRes] = await Promise.all([
        axios.get('http://localhost:5000/api/agreements/active'),
        axios.get('http://localhost:5000/api/procedures/pending-maintenance'),
        axios.get('http://localhost:5000/api/contractors'),
        axios.get('http://localhost:5000/api/logs'),
        axios.get('http://localhost:5000/api/functions/metrics')
      ]);
      setAgreements(agreementsRes.data);
      setPendingMaintenanceList(ticketsRes.data);
      setContractors(contractorsRes.data);
      setTriggerLogs(logsRes.data);
      setMetrics(metricsRes.data);
    } catch (err) {
      console.error('Portal load error:', err);
    }
  };

  useEffect(() => {
    if (activePage === 'management') {
      loadManagementData();
    }
  }, [activePage]);

  // Execute Any Stored Procedure
  const handleExecuteProcedure = async () => {
    try {
      let endpoint = '';
      if (selectedProcedure === 'property_details') endpoint = '/api/procedures/property-details';
      else if (selectedProcedure === 'tenant') endpoint = `/api/procedures/tenant/${encodeURIComponent(procedureParam)}`;
      else if (selectedProcedure === 'payment') endpoint = `/api/procedures/payment/${procedureParam}`;
      else if (selectedProcedure === 'pending_maintenance') endpoint = '/api/procedures/pending-maintenance';
      else if (selectedProcedure === 'contractor') endpoint = `/api/procedures/contractor/${procedureParam}`;

      const res = await axios.get(`http://localhost:5000${endpoint}`);
      setProcedureResult(res.data);
    } catch (err) {
      console.error(err);
      alert('Error calling stored procedure. Check console.');
    }
  };

  // Action: Fire Property Trigger
  const handleTriggerPropertyStatus = async () => {
    try {
      const res = await axios.post('http://localhost:5000/api/triggers/toggle-property-status', {
        property_id: targetPropId,
        new_status: targetStatus
      });
      setTriggerBanner(res.data.message);
      loadManagementData();
      fetchFilteredProperties();
      setTimeout(() => setTriggerBanner(''), 5000);
    } catch (err) {
      console.error(err);
    }
  };

  // Action: Fire Payment Trigger
  const handleTriggerPayment = async () => {
    try {
      const res = await axios.post('http://localhost:5000/api/triggers/record-payment', {
        agreement_id: paymentAgreementId,
        amount: paymentAmount,
        payment_mode: 'Online Bank Transfer'
      });
      setTriggerBanner(res.data.message);
      loadManagementData();
      setTimeout(() => setTriggerBanner(''), 5000);
    } catch (err) {
      console.error(err);
    }
  };

  // Assign Contractor
  const handleAssignContractor = async (requestId) => {
    const contractorId = selectedContractors[requestId];
    if (!contractorId) return alert('Select a contractor from dropdown.');
    try {
      const res = await axios.post('http://localhost:5000/api/assign-contractor', {
        request_id: requestId,
        contractor_id: contractorId
      });
      setTriggerBanner(res.data.message);
      loadManagementData();
      setTimeout(() => setTriggerBanner(''), 5000);
    } catch (err) {
      console.error(err);
    }
  };

  // 3D Repair Submission
  const handleMaintenanceSubmit = async (e) => {
    e.preventDefault();
    if (!maintenanceDesc || !selected3D) return;
    try {
      await axios.post('http://localhost:5000/api/maintenance', {
        property_id: selected3D.Property_ID,
        description: maintenanceDesc
      });
      setMaintenanceSuccess('Ticket logged & committed in MySQL! Trigger fired.');
      setMaintenanceDesc('');
      setTimeout(() => setMaintenanceSuccess(''), 4000);
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="luxury-container">
      {/* 1. Header Navigation */}
      <nav className="navbar">
        <div className="brand-title" onClick={() => setActivePage('properties')}>
          LUXURY ESTATES
        </div>

        <div className="nav-right">
          <div className="nav-links">
            <span
              className={`nav-link ${activePage === 'properties' ? 'active' : ''}`}
              onClick={() => setActivePage('properties')}
            >
              Properties
            </span>
            <span
              className={`nav-link ${activePage === 'management' ? 'active' : ''}`}
              onClick={() => setActivePage('management')}
            >
              Management Portal
            </span>
            <span
              className={`nav-link ${activePage === 'about' ? 'active' : ''}`}
              onClick={() => setActivePage('about')}
            >
              About
            </span>
            <span
              className={`nav-link ${activePage === 'contact' ? 'active' : ''}`}
              onClick={() => setActivePage('contact')}
            >
              Contact
            </span>
          </div>

          <button className="theme-toggle-btn" onClick={() => setTheme(prev => prev === 'dark' ? 'light' : 'dark')}>
            {theme === 'dark' ? '☀️ Light' : '🌙 Dark'}
          </button>
        </div>
      </nav>

      {/* 2. PROPERTIES VIEW */}
      {activePage === 'properties' && (
        <>
          <div className="hero-wrapper">
            <div className="hero-image-box">
              <img
                src="https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=1600&q=85"
                alt="Cliffside Luxury Villa"
                className="hero-img"
              />
              <div className="hero-overlay">
                <h1>MODERN ARCHITECTURAL MARVELS</h1>
                <p style={{ color: '#cbd5e1', letterSpacing: '0.05em' }}>
                  Handcrafted residences backed by EstateEase MySQL Database
                </p>
              </div>
            </div>
          </div>

          <section className="properties-section">
            <div className="filter-bar">
              <div className="filter-group">
                <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Property Type:</span>
                <select className="filter-select" value={filterType} onChange={(e) => setFilterType(e.target.value)}>
                  <option value="All">All Types</option>
                  <option value="Apartment">Apartment</option>
                  <option value="House">House</option>
                  <option value="Commercial">Commercial</option>
                </select>
              </div>

              <div className="filter-group">
                <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Status:</span>
                <select className="filter-select" value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
                  <option value="All">All Statuses</option>
                  <option value="Available">Available</option>
                  <option value="Rented">Rented</option>
                  <option value="Under Maintenance">Under Maintenance</option>
                </select>
              </div>

              <span style={{ fontSize: '0.85rem', color: 'var(--gold-accent)', fontWeight: 600 }}>
                {properties.length} Results Found in Database
              </span>
            </div>

            <div className="property-grid">
              {properties.map((item, index) => {
                const img = LUXURY_IMAGES[index % LUXURY_IMAGES.length];
                const statusClass = item.Status === 'Available' ? 'available' : item.Status === 'Rented' ? 'rented' : 'maintenance';

                return (
                  <div key={item.Property_ID} className="prop-card">
                    <div className="card-img-container">
                      <img src={img} alt={item.Address} className="card-img" />
                      <div style={{ position: 'absolute', top: 12, right: 12 }}>
                        <span className={`badge-tag ${statusClass}`}>{item.Status}</span>
                      </div>
                    </div>
                    <div className="card-body">
                      <div className="card-price">$25,000,000</div>
                      <div className="card-location">{item.Address}</div>
                      <div className="card-features">
                        <span>🛏️ 2 Bed</span>
                        <span>🛁 1 Bath</span>
                        <span>📐 180 m²</span>
                        <span>🏛️ {item.Property_Type}</span>
                      </div>
                      <button className="gold-btn" onClick={() => setSelected3D(item)}>
                        VIEW 3D & DETAILS
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </section>
        </>
      )}

      {/* 3. COMPLETE MANAGEMENT PORTAL: PROCEDURES, FUNCTIONS & TRIGGERS */}
      {activePage === 'management' && (
        <section className="admin-section">
          <div className="admin-header">
            <span className="section-tag">Backoffice Control Engine</span>
            <h2 className="section-title">EstateEase MySQL Management Portal</h2>
          </div>

          {triggerBanner && (
            <div style={{
              background: 'rgba(34, 197, 94, 0.2)',
              border: '1px solid #4ade80',
              color: '#4ade80',
              padding: '1rem',
              borderRadius: '6px',
              marginBottom: '2rem',
              fontWeight: 600,
              textAlign: 'center'
            }}>
              ✓ {triggerBanner}
            </div>
          )}

          {/* Section A: MySQL Stored Functions & Metrics */}
          <div className="about-stats" style={{ marginBottom: '2.5rem', marginTop: 0 }}>
            <div className="stat-box">
              <div className="stat-num">₹{Number(metrics.totalRevenue).toLocaleString()}</div>
              <div className="stat-label">Total Revenue (SUM Function)</div>
            </div>
            <div className="stat-box">
              <div className="stat-num">₹{Number(metrics.monthlyCashflow).toLocaleString()}</div>
              <div className="stat-label">Monthly Active Rent (Function)</div>
            </div>
            <div className="stat-box">
              <div className="stat-num">{metrics.totalPending}</div>
              <div className="stat-label">Pending Maintenance (COUNT)</div>
            </div>
            <div className="stat-box">
              <div className="stat-num">{metrics.totalAvailable}</div>
              <div className="stat-label">Available Units</div>
            </div>
          </div>

          {/* Section B: All 5 Stored Procedures Interactivity */}
          <div className="admin-card" style={{ marginBottom: '2.5rem' }}>
            <h3 style={{ fontSize: '1.25rem', color: 'var(--gold-accent)' }}>
              MySQL Stored Procedures Console
            </h3>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginTop: '0.2rem', marginBottom: '1.2rem' }}>
              Select and execute compiled stored procedures defined in your SQL script.
            </p>

            <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', alignItems: 'center' }}>
              <select
                className="filter-select"
                value={selectedProcedure}
                onChange={(e) => {
                  setSelectedProcedure(e.target.value);
                  if (e.target.value === 'tenant') setProcedureParam('Aditya Rao');
                  else if (e.target.value === 'payment') setProcedureParam('301');
                  else if (e.target.value === 'contractor') setProcedureParam('601');
                  else setProcedureParam('');
                }}
              >
                <option value="tenant">CALL tenant_rental_details(name)</option>
                <option value="payment">CALL payment_details(agreement_id)</option>
                <option value="property_details">CALL property_details()</option>
                <option value="pending_maintenance">CALL pending_maintenance()</option>
                <option value="contractor">CALL contractor_work_details(contractor_id)</option>
              </select>

              {(selectedProcedure === 'tenant' || selectedProcedure === 'payment' || selectedProcedure === 'contractor') && (
                <input
                  type="text"
                  placeholder={selectedProcedure === 'tenant' ? 'e.g. Aditya Rao' : selectedProcedure === 'payment' ? 'Agreement ID (301)' : 'Contractor ID (601)'}
                  value={procedureParam}
                  onChange={(e) => setProcedureParam(e.target.value)}
                  style={{
                    padding: '0.6rem 1rem',
                    background: 'var(--bg-primary)',
                    border: '1px solid var(--border-color)',
                    color: 'var(--text-primary)',
                    borderRadius: '6px'
                  }}
                />
              )}

              <button className="gold-btn" style={{ width: 'auto', padding: '0.65rem 1.8rem' }} onClick={handleExecuteProcedure}>
                Run Procedure
              </button>
            </div>

            {procedureResult && (
              <div style={{ marginTop: '1.5rem', background: 'var(--bg-primary)', padding: '1rem', borderRadius: '6px', overflowX: 'auto' }}>
                <p style={{ color: 'var(--gold-accent)', fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.5rem' }}>
                  PROCEDURE RESULT SET ({Array.isArray(procedureResult) ? procedureResult.length : 1} records):
                </p>
                <pre style={{ color: '#4ade80', fontSize: '0.8rem', maxHeight: '200px', overflowY: 'auto' }}>
                  {JSON.stringify(procedureResult, null, 2)}
                </pre>
              </div>
            )}
          </div>

          {/* Section C: Fire Triggers Live (Buttons) */}
          <div className="admin-grid">
            {/* Trigger Fire Action 1 */}
            <div className="admin-card">
              <h3 style={{ fontSize: '1.15rem', color: 'var(--gold-accent)' }}>Trigger Action: Property Status Update</h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem', marginTop: '0.2rem', marginBottom: '1rem' }}>
                Fires trigger on <code>Property</code> to insert an audit record into <code>Property_Log</code>.
              </p>
              <div style={{ display: 'flex', gap: '0.8rem', alignItems: 'center' }}>
                <input
                  type="number"
                  placeholder="Property ID"
                  value={targetPropId}
                  onChange={(e) => setTargetPropId(e.target.value)}
                  style={{ width: '100px', padding: '0.5rem', background: 'var(--bg-primary)', border: '1px solid var(--border-color)', color: '#fff', borderRadius: '4px' }}
                />
                <select
                  className="filter-select"
                  value={targetStatus}
                  onChange={(e) => setTargetStatus(e.target.value)}
                >
                  <option value="Available">Available</option>
                  <option value="Rented">Rented</option>
                  <option value="Under Maintenance">Under Maintenance</option>
                </select>
                <button className="gold-btn" style={{ width: 'auto', padding: '0.5rem 1rem' }} onClick={handleTriggerPropertyStatus}>
                  Fire Trigger
                </button>
              </div>
            </div>

            {/* Trigger Fire Action 2 */}
            <div className="admin-card">
              <h3 style={{ fontSize: '1.15rem', color: 'var(--gold-accent)' }}>Trigger Action: Insert Payment</h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem', marginTop: '0.2rem', marginBottom: '1rem' }}>
                Fires trigger on <code>Payment</code> to log transactions into <code>Payment_Log</code>.
              </p>
              <div style={{ display: 'flex', gap: '0.8rem', alignItems: 'center' }}>
                <input
                  type="number"
                  placeholder="Agreement ID"
                  value={paymentAgreementId}
                  onChange={(e) => setPaymentAgreementId(e.target.value)}
                  style={{ width: '120px', padding: '0.5rem', background: 'var(--bg-primary)', border: '1px solid var(--border-color)', color: '#fff', borderRadius: '4px' }}
                />
                <input
                  type="number"
                  placeholder="Amount"
                  value={paymentAmount}
                  onChange={(e) => setPaymentAmount(e.target.value)}
                  style={{ width: '110px', padding: '0.5rem', background: 'var(--bg-primary)', border: '1px solid var(--border-color)', color: '#fff', borderRadius: '4px' }}
                />
                <button className="gold-btn" style={{ width: 'auto', padding: '0.5rem 1rem' }} onClick={handleTriggerPayment}>
                  Fire Trigger
                </button>
              </div>
            </div>
          </div>

          {/* Section D: Live Trigger Audit Trail Table */}
          {/* Live Trigger Audit Trail Table */}
          <div className="admin-card" style={{ marginBottom: '2.5rem' }}>
            <h3 style={{ fontSize: '1.25rem', color: 'var(--gold-accent)' }}>Live Trigger Audit Trails (Auto-Generated)</h3>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginTop: '0.2rem' }}>
              Rows populated purely by MySQL Triggers: <code>Property_Log</code>, <code>Payment_Log</code>, <code>Maintenance_Log</code>, and <code>Work_Order_Log</code>.
            </p>
            <table className="admin-table">
              <thead>
                <tr>
                  <th>Audit Table</th>
                  <th>Record Ref</th>
                  <th>Old Status / Details</th>
                  <th>New Status / State</th>
                  <th>Timestamp</th>
                </tr>
              </thead>
              <tbody>
                {triggerLogs.propertyLogs?.map((log) => (
                  <tr key={`p-${log.Log_ID}`}>
                    <td><code>Property_Log</code></td>
                    <td>Property #{log.Property_ID}</td>
                    <td><span className="badge-tag maintenance">{log.Old_Status || 'None'}</span></td>
                    <td><span className="badge-tag available">{log.New_Status}</span></td>
                    <td>{new Date(log.Changed_At).toLocaleTimeString()}</td>
                  </tr>
                ))}
                {triggerLogs.paymentLogs?.map((log) => (
                  <tr key={`pay-${log.Log_ID}`}>
                    <td><code>Payment_Log</code></td>
                    <td>Payment #{log.Payment_ID} (Agr #{log.Agreement_ID})</td>
                    <td>₹{Number(log.Amount).toLocaleString()}</td>
                    <td><span className="badge-tag rented">{log.Payment_Status}</span></td>
                    <td>{new Date(log.Created_At).toLocaleTimeString()}</td>
                  </tr>
                ))}
                {triggerLogs.maintenanceLogs?.map((log) => (
                  <tr key={`m-${log.Log_ID}`}>
                    <td><code>Maintenance_Log</code></td>
                    <td>Request #{log.Request_ID}</td>
                    <td><span className="badge-tag maintenance">{log.Old_Status || 'None'}</span></td>
                    <td><span className="badge-tag available">{log.New_Status}</span></td>
                    <td>{new Date(log.Changed_At).toLocaleTimeString()}</td>
                  </tr>
                ))}
                {triggerLogs.workOrderLogs?.map((log) => (
                  <tr key={`wo-${log.Log_ID}`}>
                    <td><code>Work_Order_Log</code></td>
                    <td>Order #{log.WorkOrder_ID}</td>
                    <td><span className="badge-tag maintenance">{log.Old_Status || 'None'}</span></td>
                    <td><span className="badge-tag available">{log.New_Status}</span></td>
                    <td>{new Date(log.Changed_At).toLocaleTimeString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Section E: Active Leases & Contractor Dispatch Table */}
          <div className="admin-grid">
            <div className="admin-card">
              <h3 style={{ fontSize: '1.2rem', color: 'var(--gold-accent)' }}>Active Rental Leases</h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginTop: '0.3rem' }}>
                Queried from SQL View: <code>Active_Rental_View</code>
              </p>
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>Tenant</th>
                    <th>Property</th>
                    <th>Monthly Rent</th>
                  </tr>
                </thead>
                <tbody>
                  {agreements.map((a) => (
                    <tr key={a.Agreement_ID}>
                      <td><strong>{a.Tenant_Name}</strong></td>
                      <td>{a.Address}</td>
                      <td style={{ color: '#4ade80', fontWeight: 600 }}>₹{a.Monthly_Rent}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="admin-card">
              <h3 style={{ fontSize: '1.2rem', color: 'var(--gold-accent)' }}>Contractor Dispatch &amp; Work Orders</h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginTop: '0.3rem' }}>
                Populated by <code>pending_maintenance()</code> procedure
              </p>
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Property</th>
                    <th>Assign Contractor</th>
                  </tr>
                </thead>
                <tbody>
                  {pendingMaintenanceList.map((m) => (
                    <tr key={m.Request_ID}>
                      <td>#{m.Request_ID}</td>
                      <td>{m.Address}</td>
                      <td>
                        <div style={{ display: 'flex', gap: '0.4rem' }}>
                          <select
                            className="filter-select"
                            style={{ padding: '0.3rem 0.5rem', fontSize: '0.75rem' }}
                            onChange={(e) => setSelectedContractors({
                              ...selectedContractors,
                              [m.Request_ID]: e.target.value
                            })}
                          >
                            <option value="">Select Contractor...</option>
                            {contractors.map(c => (
                              <option key={c.Contractor_ID} value={c.Contractor_ID}>
                                {c.Contractor_Name} ({c.Specialty})
                              </option>
                            ))}
                          </select>
                          <button
                            className="gold-btn"
                            style={{ padding: '0.3rem 0.6rem', fontSize: '0.7rem', width: 'auto' }}
                            onClick={() => handleAssignContractor(m.Request_ID)}
                          >
                            Assign
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </section>
      )}

      {/* 4. ABOUT SECTION */}
      {activePage === 'about' && (
        <section className="about-section">
          <div className="about-header">
            <span className="section-tag">Legacy & Vision</span>
            <h2 className="section-title">Redefining Architectural Luxury</h2>
          </div>
          <div className="about-grid">
            <div>
              <p style={{ color: 'var(--text-muted)', lineHeight: '1.8', marginBottom: '1.2rem' }}>
                EstateEase represents the pinnacle of premium residential management. From clifftop villas to contemporary penthouse residences, our mission is to deliver exceptional architectural spaces with seamless, transparent management.
              </p>
              <div className="about-stats">
                <div className="stat-box">
                  <div className="stat-num">{properties.length}+</div>
                  <div className="stat-label">Prime Estates</div>
                </div>
                <div className="stat-box">
                  <div className="stat-num">$250M</div>
                  <div className="stat-label">Portfolio Value</div>
                </div>
                <div className="stat-box">
                  <div className="stat-num">100%</div>
                  <div className="stat-label">Verified Owners</div>
                </div>
              </div>
            </div>
            <div style={{ borderRadius: '8px', overflow: 'hidden', height: '380px' }}>
              <img
                src="https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?auto=format&fit=crop&w=800&q=80"
                alt="Architecture"
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
              />
            </div>
          </div>
        </section>
      )}

      {/* 5. CONTACT SECTION */}
      {activePage === 'contact' && (
        <section className="contact-section">
          <div className="about-header">
            <span className="section-tag">Private Inquiries</span>
            <h2 className="section-title">Connect with Our Private Office</h2>
          </div>
          <div className="contact-grid">
            <div className="contact-info-card">
              <div>
                <h3 style={{ fontSize: '1.1rem', color: 'var(--gold-accent)' }}>Headquarters</h3>
                <p style={{ color: 'var(--text-muted)', marginTop: '0.2rem' }}>Senapati Bapat Road, Shivajinagar, Pune 411016</p>
              </div>
              <div>
                <h3 style={{ fontSize: '1.1rem', color: 'var(--gold-accent)' }}>Concierge Desk</h3>
                <p style={{ color: 'var(--text-muted)', marginTop: '0.2rem' }}>concierge@estateease.com</p>
              </div>
            </div>
            <form className="contact-form" onSubmit={(e) => { e.preventDefault(); alert('Message dispatched.'); }}>
              <input type="text" placeholder="Your Full Name" required />
              <input type="email" placeholder="Email Address" required />
              <textarea rows="4" placeholder="Your message..." required></textarea>
              <button type="submit" className="gold-btn">Submit Inquiry</button>
            </form>
          </div>
        </section>
      )}

      {/* 6. Three.js 3D Modal + Maintenance Request Action */}
      {selected3D && (
        <div className="modal-backdrop" onClick={() => setSelected3D(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <button className="modal-close" onClick={() => setSelected3D(null)}>×</button>

            <h2 style={{ fontFamily: 'Cinzel, serif', color: 'var(--gold-accent)', marginBottom: '0.3rem' }}>
              3D Architectural Model
            </h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '1.2rem' }}>
              {selected3D.Address} &bull; <strong>{selected3D.Property_Type}</strong>
            </p>

            <PropertyCanvas propertyType={selected3D.Property_Type} theme={theme} />

            <div style={{
              marginTop: '1.2rem',
              display: 'flex',
              justifyContent: 'space-between',
              color: 'var(--text-muted)',
              fontSize: '0.9rem',
              borderTop: '1px solid var(--border-color)',
              paddingTop: '1rem'
            }}>
              <span>Owner: <strong style={{ color: 'var(--text-primary)' }}>{selected3D.Owner_Name}</strong></span>
              <span>Phone: <strong style={{ color: 'var(--text-primary)' }}>{selected3D.Phone}</strong></span>
              <span>Status: <strong style={{ color: 'var(--gold-accent)' }}>{selected3D.Status}</strong></span>
            </div>

            {/* Book Maintenance Form inside Modal */}
            <form onSubmit={handleMaintenanceSubmit} style={{ marginTop: '1.2rem', display: 'flex', gap: '0.8rem' }}>
              <input
                type="text"
                placeholder="Log a maintenance issue for this property..."
                value={maintenanceDesc}
                onChange={(e) => setMaintenanceDesc(e.target.value)}
                style={{
                  flex: 1,
                  padding: '0.65rem 1rem',
                  background: 'var(--bg-primary)',
                  border: '1px solid var(--border-color)',
                  color: 'var(--text-primary)',
                  borderRadius: '4px'
                }}
                required
              />
              <button type="submit" className="gold-btn" style={{ width: 'auto', padding: '0 1.5rem' }}>
                Request Repair
              </button>
            </form>
            {maintenanceSuccess && (
              <p style={{ color: '#4ade80', fontSize: '0.85rem', marginTop: '0.5rem' }}>{maintenanceSuccess}</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}