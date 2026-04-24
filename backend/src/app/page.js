"use client";
import React, { useState, useEffect } from 'react';
import './globals.css';

export default function AdminDashboard() {
  const [kycList, setKycList] = useState([]);
  const [selectedKyc, setSelectedKyc] = useState(null);
  const [feedback, setFeedback] = useState('');
  const [editMode, setEditMode] = useState(false);
  const [editData, setEditData] = useState({});
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState('ALL');
  const [rejectedFields, setRejectedFields] = useState([]);
  const [isMounted, setIsMounted] = useState(false);

  useEffect(() => {
    setIsMounted(true);
  }, []);

  useEffect(() => {
    fetch('/api/kyc')
      .then(res => res.json())
      .then(data => setKycList(data))
      .catch(err => console.error(err));
  }, []);

  const handleSelect = (kyc) => {
    setSelectedKyc(kyc);
    setEditData(kyc);
    setFeedback(kyc.feedback || '');
    setRejectedFields(kyc.rejectedFields || []);
    setEditMode(false);
  };

  const handleUpdateStatus = async (status) => {
    try {
      const res = await fetch(`/api/kyc/${selectedKyc.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          ...editData,
          status,
          feedback,
          rejectedFields: status === 'DECLINED' ? rejectedFields : []
        })
      });
      if (res.ok) {
        const updated = await res.json();
        setKycList(prev => prev.map(item => item.id === selectedKyc.id ? updated.data : item));
        setSelectedKyc(updated.data);
        setEditMode(false);
        alert(`Application ${status}`);
      }
    } catch (err) {
      alert('Failed to update');
    }
  };

  const handleDelete = async () => {
    if (!confirm('Are you sure you want to permanently delete this submission? This cannot be undone.')) return;

    try {
      const res = await fetch(`/api/kyc/${selectedKyc.id}`, { method: 'DELETE' });
      if (res.ok) {
        setKycList(prev => prev.filter(item => item.id !== selectedKyc.id));
        setSelectedKyc(null);
        alert('Application deleted successfully');
      }
    } catch (err) {
      alert('Failed to delete');
    }
  };

  const handleEditChange = (e) => {
    setEditData({ ...editData, [e.target.name]: e.target.value });
  };

  const toggleRejectedField = (field) => {
    setRejectedFields(prev =>
      prev.includes(field) ? prev.filter(f => f !== field) : [...prev, field]
    );
  };

  const filteredList = kycList.filter(kyc => {
    const matchesSearch = (kyc.fullName || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
      (kyc.nin || '').toLowerCase().includes(searchTerm.toLowerCase());
    const matchesFilter = filterStatus === 'ALL' || kyc.status === filterStatus;
    return matchesSearch && matchesFilter;
  });

  return (
    <div className="container">
      <header className="header">
        <h1> KYC Admin Panel</h1>
      </header>

      <div className="main-content">
        {/* Sidebar List */}
        <div className="sidebar">
          <h2>Submissions</h2>

          <div className="filters" style={{ display: 'flex', gap: '10px', marginBottom: '15px', padding: '0 20px' }}>
            <input
              type="text"
              placeholder="Search Name or ID..."
              value={searchTerm}
              onChange={e => setSearchTerm(e.target.value)}
              style={{ flex: 1, padding: '8px 12px', borderRadius: '6px', border: '1px solid #374151', backgroundColor: '#1F2937', color: '#FFF' }}
            />
            <select
              value={filterStatus}
              onChange={e => setFilterStatus(e.target.value)}
              style={{ padding: '8px', borderRadius: '6px', border: '1px solid #374151', backgroundColor: '#1F2937', color: '#FFF', cursor: 'pointer' }}
            >
              <option value="ALL">All Status</option>
              <option value="PENDING">Pending</option>
              <option value="APPROVED">Approved</option>
              <option value="DECLINED">Declined</option>
            </select>
          </div>

          <div className="list">
            {filteredList.map(kyc => (
              <div
                key={kyc.id}
                className={`list-item ${selectedKyc?.id === kyc.id ? 'active' : ''}`}
                onClick={() => handleSelect(kyc)}
              >
                <div className="item-header">
                  <strong>{kyc.fullName || 'Unknown'}</strong>
                  <span className={`badge ${kyc.status.toLowerCase()}`}>{kyc.status}</span>
                </div>
                <div className="item-sub">ID: {kyc.nin || 'N/A'}</div>
                <div className="item-date">{isMounted ? new Date(kyc.submittedAt).toLocaleString() : ''}</div>
              </div>
            ))}
            {filteredList.length === 0 && <p className="empty">No submissions found.</p>}
          </div>
        </div>

        {/* Detail View */}
        <div className="detail-view">
          {selectedKyc ? (
            <div className="detail-card">
              <div className="detail-header">
                <h2>Applicant Details</h2>
                <div className="action-buttons">
                  <button className="btn outline" onClick={() => setEditMode(!editMode)}>
                    {editMode ? 'Cancel Edit' : 'Edit Data'}
                  </button>
                  <button className="btn danger" onClick={handleDelete} style={{ marginLeft: '10px' }}>
                    Delete
                  </button>
                </div>
              </div>

              <div className="images-gallery">
                <div className="image-box">
                  <h3>Front ID</h3>
                  {selectedKyc.frontImage ? <img src={selectedKyc.frontImage} alt="Front ID" /> : <div className="no-img">No Image</div>}
                </div>
                <div className="image-box">
                  <h3>Back ID</h3>
                  {selectedKyc.backImage ? <img src={selectedKyc.backImage} alt="Back ID" /> : <div className="no-img">No Image</div>}
                </div>
                <div className="image-box">
                  <h3>Selfie</h3>
                  {selectedKyc.selfieImage ? <img src={selectedKyc.selfieImage} alt="Selfie" /> : <div className="no-img">No Image</div>}
                </div>
              </div>

              {selectedKyc.history && selectedKyc.history.length > 0 && (
                <div className="history-section" style={{ marginTop: '20px', padding: '15px', backgroundColor: 'rgba(239, 68, 68, 0.1)', borderRadius: '8px', borderLeft: '4px solid #EF4444' }}>
                  <h3 style={{ marginTop: 0, color: '#EF4444' }}>Previous Declines</h3>
                  <ul style={{ margin: 0, paddingLeft: '20px' }}>
                    {selectedKyc.history.map((h, idx) => (
                      <li key={idx} style={{ marginBottom: '8px' }}>
                        <span style={{ fontSize: '0.85em', color: '#9CA3AF' }}>{isMounted ? new Date(h.timestamp).toLocaleString() : ''}</span><br />
                        <strong>Feedback:</strong> {h.feedback}
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              <div className="data-grid">
                {['fullName', 'dob', 'nin', 'sex', 'nationality', 'address'].map(field => (
                  <div className="data-field" key={field}>
                    <label>{field.toUpperCase()}</label>
                    {editMode ? (
                      <input
                        type="text"
                        name={field}
                        value={editData[field] || ''}
                        onChange={handleEditChange}
                      />
                    ) : (
                      <div className="value">{selectedKyc[field] || '—'}</div>
                    )}
                  </div>
                ))}
              </div>

              <div className="action-section">
                <h3>Admin Action</h3>
                <label>Feedback / Reason (Optional)</label>
                <textarea
                  value={feedback}
                  onChange={e => setFeedback(e.target.value)}
                  placeholder="Enter reason if declining, or notes if approving..."
                  rows="3"
                />

                <div className="rejection-options" style={{ marginTop: '15px' }}>
                  <label style={{ fontWeight: 'bold', display: 'block', marginBottom: '8px' }}>Select Rejected Items:</label>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                    {[
                      { id: 'frontImage', label: 'ID Front Side' },
                      { id: 'backImage', label: 'ID Back Side' },
                      { id: 'selfieImage', label: 'Selfie Photo' },
                      { id: 'details', label: 'Personal Details' }
                    ].map(field => (
                      <label key={field.id} style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}>
                        <input
                          type="checkbox"
                          checked={rejectedFields.includes(field.id)}
                          onChange={() => toggleRejectedField(field.id)}
                        />
                        {field.label}
                      </label>
                    ))}
                  </div>
                </div>

                <div className="action-buttons">
                  <button className="btn success" onClick={() => handleUpdateStatus('APPROVED')}>
                    Approve KYC
                  </button>
                  <button className="btn danger" onClick={() => handleUpdateStatus('DECLINED')}>
                    Decline KYC
                  </button>
                  {editMode && (
                    <button className="btn primary" onClick={() => handleUpdateStatus(selectedKyc.status)}>
                      Save Edits Only
                    </button>
                  )}
                </div>
              </div>
            </div>
          ) : (
            <div className="placeholder">
              Select an application from the list to review it.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
