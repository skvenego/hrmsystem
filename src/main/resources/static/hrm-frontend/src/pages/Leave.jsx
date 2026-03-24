import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Calendar, CheckCircle, XCircle, Clock, Plus, Trash2, Edit } from 'lucide-react';
import Sidebar from '../components/Sidebar';

const Leave = () => {
  const [leaves, setLeaves] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [showBalanceModal, setShowBalanceModal] = useState(false);
  const [selectedLeave, setSelectedLeave] = useState(null);
  const [formData, setFormData] = useState({
    employeeId: '',
    leaveType: 'CASUAL',
    startDate: '',
    endDate: '',
    reason: ''
  });

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const token = localStorage.getItem('token');
      const [leavesRes, employeesRes] = await Promise.all([
        fetch('/api/leaves', {
          headers: { 'Authorization': `Bearer ${token}` }
        }),
        fetch('/api/employees', {
          headers: { 'Authorization': `Bearer ${token}` }
        })
      ]);

      if (leavesRes.ok) {
        const leavesData = await leavesRes.json();
        setLeaves(leavesData);
      }

      if (employeesRes.ok) {
        const employeesData = await employeesRes.json();
        setEmployees(employeesData);
      }

      setLoading(false);
    } catch (error) {
      console.error('Error fetching data:', error);
      alert('Failed to load data. Please login again.');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const token = localStorage.getItem('token');
      const response = await fetch('/api/leaves/apply', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(formData)
      });

      if (response.ok) {
        alert('✓ Leave application submitted successfully');
        setShowModal(false);
        setFormData({ employeeId: '', leaveType: 'CASUAL', startDate: '', endDate: '', reason: '' });
        fetchData();
      } else {
        const error = await response.text();
        alert('Error: ' + error);
      }
    } catch (error) {
      alert('Error submitting leave: ' + error.message);
    }
  };

  const handleApprove = async (leaveId) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`/api/leaves/approve/${leaveId}?approvedBy=Admin`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (response.ok) {
        alert('✓ Leave approved successfully');
        fetchData();
      } else {
        const error = await response.text();
        alert('Error: ' + error);
      }
    } catch (error) {
      alert('Error approving leave: ' + error.message);
    }
  };

  const handleReject = async (leaveId) => {
    const reason = prompt('Enter rejection reason:');
    if (!reason) return;

    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`/api/leaves/reject/${leaveId}?rejectionReason=${encodeURIComponent(reason)}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (response.ok) {
        alert('✓ Leave rejected');
        fetchData();
      } else {
        const error = await response.text();
        alert('Error: ' + error);
      }
    } catch (error) {
      alert('Error rejecting leave: ' + error.message);
    }
  };

  const handleCancel = async (leaveId) => {
    if (!confirm('Are you sure you want to cancel this leave?')) return;

    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`/api/leaves/cancel/${leaveId}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (response.ok) {
        alert('✓ Leave cancelled');
        fetchData();
      } else {
        const error = await response.text();
        alert('Error: ' + error);
      }
    } catch (error) {
      alert('Error cancelling leave: ' + error.message);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'APPROVED': return 'success';
      case 'REJECTED': return 'danger';
      case 'PENDING': return 'warning';
      case 'CANCELLED': return 'secondary';
      default: return 'info';
    }
  };

  const getLeaveTypeColor = (type) => {
    switch (type) {
      case 'SICK': return '#ef4444';
      case 'CASUAL': return '#3b82f6';
      case 'ANNUAL': return '#10b981';
      case 'MATERNITY': return '#f59e0b';
      case 'PATERNITY': return '#8b5cf6';
      default: return '#6b7280';
    }
  };

  // Calculate statistics
  const pendingRequests = leaves.filter(l => l.status === 'PENDING').length;
  const approvedLeaves = leaves.filter(l => l.status === 'APPROVED').length;
  const rejectedLeaves = leaves.filter(l => l.status === 'REJECTED').length;

  const today = new Date().toISOString().split('T')[0];
  const onLeaveToday = leaves.filter(l => 
    l.status === 'APPROVED' && 
    l.startDate <= today && 
    l.endDate >= today
  ).length;

  if (loading) {
    return (
      <div className="dashboard">
        <Sidebar />
        <main className="main-content">
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh' }}>
            <div>Loading...</div>
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="dashboard">
      <Sidebar />
      <main className="main-content">
        <div className="dashboard-header">
          <div>
            <h1 className="dashboard-title">Leave Management</h1>
            <p style={{ color: '#6b7280', marginTop: '0.25rem' }}>Manage employee leave requests</p>
          </div>
          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={() => setShowModal(true)}
            style={{
              padding: '0.75rem 1.5rem',
              background: 'linear-gradient(135deg, #4f46e5, #06b6d4)',
              color: 'white',
              border: 'none',
              borderRadius: '8px',
              fontWeight: 600,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem'
            }}
          >
            <Plus size={18} />
            Request Leave
          </motion.button>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '1rem', marginBottom: '1.5rem' }}>
          {[
            { label: 'Pending Requests', value: pendingRequests, icon: <Clock />, color: '#f59e0b' },
            { label: 'Approved', value: approvedLeaves, icon: <CheckCircle />, color: '#10b981' },
            { label: 'Rejected', value: rejectedLeaves, icon: <XCircle />, color: '#ef4444' },
            { label: 'On Leave Today', value: onLeaveToday, icon: <Calendar />, color: '#6366f1' },
          ].map((stat, index) => (
            <motion.div
              key={index}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.1 }}
              style={{
                background: 'white',
                borderRadius: '12px',
                padding: '1.5rem',
                boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)'
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                <div style={{
                  width: '48px',
                  height: '48px',
                  borderRadius: '12px',
                  background: `${stat.color}15`,
                  color: stat.color,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  {stat.icon}
                </div>
                <div>
                  <p style={{ fontSize: '0.875rem', color: '#6b7280' }}>{stat.label}</p>
                  <h3 style={{ fontSize: '1.5rem', fontWeight: 600 }}>{stat.value}</h3>
                </div>
              </div>
            </motion.div>
          ))}
        </div>

        <motion.div
          className="table-container"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <div className="table-header">
            <h3 className="table-title">Leave Requests</h3>
          </div>
          <table className="data-table">
            <thead>
              <tr>
                <th>Employee</th>
                <th>Type</th>
                <th>From</th>
                <th>To</th>
                <th>Days</th>
                <th>Reason</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {leaves.length === 0 ? (
                <tr>
                  <td colSpan="8" style={{ textAlign: 'center', padding: '2rem', color: '#6b7280' }}>
                    No leave requests found
                  </td>
                </tr>
              ) : (
                leaves.map((leave) => (
                  <tr key={leave.id}>
                    <td>
                      <div>
                        <div style={{ fontWeight: 600 }}>{leave.employeeName}</div>
                      </div>
                    </td>
                    <td>
                      <span style={{
                        padding: '0.25rem 0.75rem',
                        borderRadius: '12px',
                        fontSize: '0.75rem',
                        fontWeight: 500,
                        background: `${getLeaveTypeColor(leave.leaveType)}15`,
                        color: getLeaveTypeColor(leave.leaveType)
                      }}>
                        {leave.leaveType}
                      </span>
                    </td>
                    <td>{new Date(leave.startDate).toLocaleDateString('en-IN')}</td>
                    <td>{new Date(leave.endDate).toLocaleDateString('en-IN')}</td>
                    <td>{leave.totalDays}</td>
                    <td style={{ maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis' }}>{leave.reason || '-'}</td>
                    <td>
                      <span className={`badge badge-${getStatusBadge(leave.status)}`}>
                        {leave.status}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: '0.5rem' }}>
                        {leave.status === 'PENDING' && (
                          <>
                            <button 
                              onClick={() => handleApprove(leave.id)}
                              style={{
                                padding: '0.25rem 0.75rem',
                                background: '#10b981',
                                color: 'white',
                                border: 'none',
                                borderRadius: '4px',
                                cursor: 'pointer',
                                fontSize: '0.75rem'
                              }}
                            >
                              Approve
                            </button>
                            <button 
                              onClick={() => handleReject(leave.id)}
                              style={{
                                padding: '0.25rem 0.75rem',
                                background: '#ef4444',
                                color: 'white',
                                border: 'none',
                                borderRadius: '4px',
                                cursor: 'pointer',
                                fontSize: '0.75rem'
                              }}
                            >
                              Reject
                            </button>
                          </>
                        )}
                        {(leave.status === 'APPROVED' || leave.status === 'PENDING') && (
                          <button 
                            onClick={() => handleCancel(leave.id)}
                            style={{
                              padding: '0.25rem 0.75rem',
                              background: '#6b7280',
                              color: 'white',
                              border: 'none',
                              borderRadius: '4px',
                              cursor: 'pointer',
                              fontSize: '0.75rem'
                            }}
                          >
                            Cancel
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </motion.div>
      </main>

      {/* Apply Leave Modal */}
      {showModal && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0, 0, 0, 0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000
        }}>
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            style={{
              background: 'white',
              borderRadius: '12px',
              padding: '2rem',
              width: '500px',
              maxHeight: '90vh',
              overflow: 'auto'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
              <h2 style={{ fontSize: '1.25rem', fontWeight: 600 }}>Request Leave</h2>
              <button onClick={() => setShowModal(false)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <XCircle size={20} />
              </button>
            </div>

            <form onSubmit={handleSubmit}>
              <div style={{ marginBottom: '1rem' }}>
                <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>Employee</label>
                <select
                  required
                  value={formData.employeeId}
                  onChange={(e) => setFormData({ ...formData, employeeId: e.target.value })}
                  style={{
                    width: '100%',
                    padding: '0.75rem',
                    borderRadius: '8px',
                    border: '1px solid #d1d5db',
                    fontSize: '0.875rem'
                  }}
                >
                  <option value="">Select Employee</option>
                  {employees.map(emp => (
                    <option key={emp.id} value={emp.id}>
                      {emp.firstName} {emp.lastName} (EMP{String(emp.id).padStart(3, '0')})
                    </option>
                  ))}
                </select>
              </div>

              <div style={{ marginBottom: '1rem' }}>
                <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>Leave Type</label>
                <select
                  value={formData.leaveType}
                  onChange={(e) => setFormData({ ...formData, leaveType: e.target.value })}
                  style={{
                    width: '100%',
                    padding: '0.75rem',
                    borderRadius: '8px',
                    border: '1px solid #d1d5db',
                    fontSize: '0.875rem'
                  }}
                >
                  <option value="SICK">Sick Leave</option>
                  <option value="CASUAL">Casual Leave</option>
                  <option value="ANNUAL">Annual Leave</option>
                  <option value="MATERNITY">Maternity Leave</option>
                  <option value="PATERNITY">Paternity Leave</option>
                </select>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>From Date</label>
                  <input
                    type="date"
                    required
                    value={formData.startDate}
                    onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
                    style={{
                      width: '100%',
                      padding: '0.75rem',
                      borderRadius: '8px',
                      border: '1px solid #d1d5db',
                      fontSize: '0.875rem'
                    }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>To Date</label>
                  <input
                    type="date"
                    required
                    value={formData.endDate}
                    onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
                    min={formData.startDate}
                    style={{
                      width: '100%',
                      padding: '0.75rem',
                      borderRadius: '8px',
                      border: '1px solid #d1d5db',
                      fontSize: '0.875rem'
                    }}
                  />
                </div>
              </div>

              <div style={{ marginBottom: '1rem' }}>
                <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>Reason</label>
                <textarea
                  required
                  value={formData.reason}
                  onChange={(e) => setFormData({ ...formData, reason: e.target.value })}
                  rows={3}
                  style={{
                    width: '100%',
                    padding: '0.75rem',
                    borderRadius: '8px',
                    border: '1px solid #d1d5db',
                    fontSize: '0.875rem',
                    resize: 'vertical',
                    position: 'relative',
                    zIndex: 1002,
                    pointerEvents: 'auto',
                    color: '#111827',
                    backgroundColor: 'white'
                  }}
                  placeholder="Enter reason for leave..."
                />
              </div>

              <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end' }}>
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  style={{
                    padding: '0.75rem 1.5rem',
                    background: '#f3f4f6',
                    color: '#374151',
                    border: 'none',
                    borderRadius: '8px',
                    cursor: 'pointer',
                    fontWeight: 500
                  }}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  style={{
                    padding: '0.75rem 1.5rem',
                    background: 'linear-gradient(135deg, #4f46e5, #06b6d4)',
                    color: 'white',
                    border: 'none',
                    borderRadius: '8px',
                    cursor: 'pointer',
                    fontWeight: 500
                  }}
                >
                  Submit Request
                </button>
              </div>
            </form>
          </motion.div>
        </div>
      )}
    </div>
  );
};

export default Leave;
