import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Calendar, Clock, CheckCircle, XCircle, FileText, Plus, Trash2 } from 'lucide-react';
import Sidebar from '../components/Sidebar';

const LeaveRequest = () => {
  const [leaves, setLeaves] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [leaveBalance, setLeaveBalance] = useState({
    casual: 0,
    sick: 0,
    annual: 0
  });
  const [probationInfo, setProbationInfo] = useState({
    isOnProbation: false,
    probationEndDate: null,
    paidLeaveEligible: false,
    remainingDays: 0
  });
  
  const user = JSON.parse(localStorage.getItem('user') || '{}');
  const employeeId = user.employeeId;
  
  const [formData, setFormData] = useState({
    leaveType: 'CASUAL',
    startDate: '',
    endDate: '',
    reason: ''
  });

  useEffect(() => {
    fetchLeaves();
    fetchLeaveBalance();
    fetchProbationInfo();
  }, []);

  const fetchLeaves = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch('/api/leaves', {
        headers: { 
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        const leavesData = await response.json();
        // Filter only this employee's leaves
        const myLeaves = leavesData.filter(leave => leave.employee?.id === parseInt(employeeId));
        setLeaves(myLeaves);
      }

      setLoading(false);
    } catch (error) {
      console.error('Error fetching leaves:', error);
      setLoading(false);
    }
  };

  const fetchLeaveBalance = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`/api/leaves/balance/${employeeId}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (response.ok) {
        const balance = await response.json();
        setLeaveBalance(balance);
      }
    } catch (error) {
      console.error('Error fetching leave balance:', error);
    }
  };

  const fetchProbationInfo = async () => {
    try {
      const token = localStorage.getItem('token');
      // Fetch employee details to get joining date
      const empResponse = await fetch(`/api/employees/${employeeId}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      
      if (empResponse.ok) {
        const employee = await empResponse.json();
        
        // Try to fetch probation period info
        try {
          const probResponse = await fetch(`/api/probation/employee/${employeeId}`, {
            headers: { 'Authorization': `Bearer ${token}` }
          });
          
          if (probResponse.ok) {
            const probation = await probResponse.json();
            const isOnProbation = probation.status === 'PROBATION' || probation.status === 'EXTENDED';
            const remainingDays = isOnProbation ? 
              Math.ceil((new Date(probation.endDate) - new Date()) / (1000 * 60 * 60 * 24)) : 0;
            
            setProbationInfo({
              isOnProbation: isOnProbation,
              probationEndDate: probation.endDate,
              paidLeaveEligible: probation.paidLeaveEligible || false,
              remainingDays: remainingDays
            });
          }
        } catch (err) {
          // If no probation record found, calculate from joining date
          if (employee.joiningDate) {
            const joinDate = new Date(employee.joiningDate);
            const probationEnd = new Date(joinDate.setMonth(joinDate.getMonth() + 6)); // Default 6 months
            const isOnProbation = new Date() < probationEnd;
            const remainingDays = isOnProbation ? 
              Math.ceil((probationEnd - new Date()) / (1000 * 60 * 60 * 24)) : 0;
            
            setProbationInfo({
              isOnProbation: isOnProbation,
              probationEndDate: probationEnd.toISOString().split('T')[0],
              paidLeaveEligible: !isOnProbation,
              remainingDays: remainingDays
            });
          }
        }
      }
    } catch (error) {
      console.error('Error fetching probation info:', error);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Validate dates
    if (new Date(formData.startDate) < new Date(new Date().toISOString().split('T')[0])) {
      alert('Start date cannot be in the past');
      return;
    }
    
    if (new Date(formData.endDate) < new Date(formData.startDate)) {
      alert('End date must be after start date');
      return;
    }

    // Check probation restrictions
    if (probationInfo.isOnProbation && !probationInfo.paidLeaveEligible) {
      const totalDays = Math.ceil((new Date(formData.endDate) - new Date(formData.startDate)) / (1000 * 60 * 60 * 24)) + 1;
      
      if (formData.leaveType === 'CASUAL' || formData.leaveType === 'SICK') {
        const confirmSubmit = window.confirm(
          `⚠️ PROBATION PERIOD WARNING\n\n` +
          `You are currently on probation period.\n` +
          `Probation ends in ${probationInfo.remainingDays} days.\n\n` +
          `During probation:\n` +
          `- Casual and Sick leaves are UNPAID\n` +
          `- You will NOT be eligible for paid leave deduction\n` +
          `- Total days requested: ${totalDays}\n\n` +
          `Do you still want to continue?`
        );
        
        if (!confirmSubmit) return;
      }
    }

    // Check leave balance
    const totalDays = Math.ceil((new Date(formData.endDate) - new Date(formData.startDate)) / (1000 * 60 * 60 * 24)) + 1;
    
    if (formData.leaveType === 'CASUAL' && totalDays > (leaveBalance.casual || 0)) {
      const confirmSubmit = window.confirm(
        `⚠️ INSUFFICIENT LEAVE BALANCE\n\n` +
        `Requested ${formData.leaveType} Leave: ${totalDays} days\n` +
        `Available Balance: ${leaveBalance.casual || 0} days\n\n` +
        `Excess days will be marked as UNPAID.\n\n` +
        `Do you want to continue?`
      );
      
      if (!confirmSubmit) return;
    }
    
    if (formData.leaveType === 'SICK' && totalDays > (leaveBalance.sick || 0)) {
      const confirmSubmit = window.confirm(
        `⚠️ INSUFFICIENT LEAVE BALANCE\n\n` +
        `Requested ${formData.leaveType} Leave: ${totalDays} days\n` +
        `Available Balance: ${leaveBalance.sick || 0} days\n\n` +
        `Excess days will be marked as UNPAID.\n\n` +
        `Do you want to continue?`
      );
      
      if (!confirmSubmit) return;
    }
    
    if (formData.leaveType === 'ANNUAL' && totalDays > (leaveBalance.annual || 0)) {
      const confirmSubmit = window.confirm(
        `⚠️ INSUFFICIENT LEAVE BALANCE\n\n` +
        `Requested ${formData.leaveType} Leave: ${totalDays} days\n` +
        `Available Balance: ${leaveBalance.annual || 0} days\n\n` +
        `Excess days will be marked as UNPAID.\n\n` +
        `Do you want to continue?`
      );
      
      if (!confirmSubmit) return;
    }

    try {
      const token = localStorage.getItem('token');
      const response = await fetch('/api/leaves/apply', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          ...formData,
          employeeId: parseInt(employeeId)
        })
      });

      if (response.ok) {
        alert('✓ Leave request submitted successfully!');
        setShowModal(false);
        setFormData({ leaveType: 'CASUAL', startDate: '', endDate: '', reason: '' });
        fetchLeaves();
        fetchLeaveBalance();
      } else {
        const error = await response.text();
        alert('Error: ' + error);
      }
    } catch (error) {
      alert('Error submitting leave request: ' + error.message);
    }
  };

  const handleCancel = async (leaveId) => {
    if (!confirm('Are you sure you want to cancel this leave request?')) return;
    
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`/api/leaves/cancel/${leaveId}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (response.ok) {
        alert('✓ Leave request cancelled successfully');
        fetchLeaves();
      } else {
        const error = await response.text();
        alert('Error: ' + error);
      }
    } catch (error) {
      alert('Error cancelling leave: ' + error.message);
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'APPROVED': return <CheckCircle size={20} className="text-green-500" />;
      case 'REJECTED': return <XCircle size={20} className="text-red-500" />;
      case 'PENDING': return <Clock size={20} className="text-yellow-500" />;
      default: return <Clock size={20} />;
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'APPROVED': return '#10b981';
      case 'REJECTED': return '#ef4444';
      case 'PENDING': return '#f59e0b';
      default: return '#6b7280';
    }
  };

  const getLeaveTypeName = (type) => {
    switch (type) {
      case 'CASUAL': return 'Casual Leave';
      case 'SICK': return 'Sick Leave';
      case 'ANNUAL': return 'Annual Leave';
      default: return type;
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  return (
    <div style={{ display: 'flex', minHeight: '100vh', background: '#f9fafb' }}>
      <Sidebar />
      
      <div style={{ flex: 1, padding: '2rem' }}>
        {/* Probation Warning Banner */}
        {probationInfo.isOnProbation && (
          <motion.div
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            style={{
              padding: '1rem',
              background: probationInfo.paidLeaveEligible ? '#d1fae5' : '#fee2e2',
              border: `2px solid ${probationInfo.paidLeaveEligible ? '#10b981' : '#ef4444'}`,
              borderRadius: '8px',
              marginBottom: '2rem',
              display: 'flex',
              alignItems: 'center',
              gap: '1rem'
            }}
          >
            <div style={{ fontSize: '1.5rem' }}>
              {probationInfo.paidLeaveEligible ? '✅' : '⚠️'}
            </div>
            <div style={{ flex: 1 }}>
              <strong style={{ color: probationInfo.paidLeaveEligible ? '#065f46' : '#991b1b', display: 'block', marginBottom: '0.25rem' }}>
                {probationInfo.paidLeaveEligible ? 'Probation Completed!' : '⚠️ You are on Probation Period'}
              </strong>
              <p style={{ color: probationInfo.paidLeaveEligible ? '#047857' : '#b91c1c', margin: 0, fontSize: '0.875rem' }}>
                {probationInfo.paidLeaveEligible 
                  ? 'Congratulations! Your probation has been completed. You are now eligible for all paid leaves.'
                  : `Probation ends in ${probationInfo.remainingDays} days. During probation, Casual and Sick leaves are UNPAID. Annual leaves can be availed after confirmation.`
                }
              </p>
            </div>
          </motion.div>
        )}

        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          style={{
            marginBottom: '2rem',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center'
          }}
        >
          <div>
            <h1 style={{ fontSize: '2rem', fontWeight: 'bold', color: '#1f2937', marginBottom: '0.5rem' }}>
              Leave Request
            </h1>
            <p style={{ color: '#6b7280' }}>Request time off and track your leave balance</p>
          </div>
          
          <motion.button
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            onClick={() => setShowModal(true)}
            style={{
              padding: '0.75rem 1.5rem',
              background: 'linear-gradient(135deg, #4f46e5, #7c3aed)',
              color: 'white',
              border: 'none',
              borderRadius: '8px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              fontWeight: 500,
              boxShadow: '0 4px 6px rgba(79, 70, 229, 0.3)'
            }}
          >
            <Plus size={20} />
            Request Leave
          </motion.button>
        </motion.div>

        {/* Leave Balance Cards */}
        <div style={{ 
          display: 'grid', 
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', 
          gap: '1rem', 
          marginBottom: '2rem' 
        }}>
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            style={{
              padding: '1.5rem',
              background: 'white',
              borderRadius: '12px',
              boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
              <Calendar size={24} color="#4f46e5" />
              <span style={{ fontSize: '0.875rem', color: '#6b7280', fontWeight: 500 }}>Casual Leave</span>
            </div>
            <div style={{ fontSize: '2rem', fontWeight: 'bold', color: '#4f46e5' }}>
              {leaveBalance.casual || 0}
            </div>
            <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>days available</div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
            style={{
              padding: '1.5rem',
              background: 'white',
              borderRadius: '12px',
              boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
              <FileText size={24} color="#10b981" />
              <span style={{ fontSize: '0.875rem', color: '#6b7280', fontWeight: 500 }}>Sick Leave</span>
            </div>
            <div style={{ fontSize: '2rem', fontWeight: 'bold', color: '#10b981' }}>
              {leaveBalance.sick || 0}
            </div>
            <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>days available</div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
            style={{
              padding: '1.5rem',
              background: 'white',
              borderRadius: '12px',
              boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
              <Clock size={24} color="#f59e0b" />
              <span style={{ fontSize: '0.875rem', color: '#6b7280', fontWeight: 500 }}>Annual Leave</span>
            </div>
            <div style={{ fontSize: '2rem', fontWeight: 'bold', color: '#f59e0b' }}>
              {leaveBalance.annual || 0}
            </div>
            <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>days available</div>
          </motion.div>
        </div>

        {/* Leave Requests Table */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
          style={{
            background: 'white',
            borderRadius: '12px',
            padding: '1.5rem',
            boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
          }}
        >
          <h2 style={{ fontSize: '1.25rem', fontWeight: '600', color: '#1f2937', marginBottom: '1rem' }}>
            My Leave Requests
          </h2>

          {loading ? (
            <div style={{ textAlign: 'center', padding: '3rem', color: '#9ca3af' }}>Loading...</div>
          ) : leaves.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '3rem', color: '#9ca3af' }}>
              No leave requests yet. Click "Request Leave" to apply for leave.
            </div>
          ) : (
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr style={{ borderBottom: '2px solid #e5e7eb' }}>
                    <th style={{ padding: '0.75rem', textAlign: 'left', color: '#6b7280', fontSize: '0.875rem' }}>Type</th>
                    <th style={{ padding: '0.75rem', textAlign: 'left', color: '#6b7280', fontSize: '0.875rem' }}>Start Date</th>
                    <th style={{ padding: '0.75rem', textAlign: 'left', color: '#6b7280', fontSize: '0.875rem' }}>End Date</th>
                    <th style={{ padding: '0.75rem', textAlign: 'left', color: '#6b7280', fontSize: '0.875rem' }}>Days</th>
                    <th style={{ padding: '0.75rem', textAlign: 'left', color: '#6b7280', fontSize: '0.875rem' }}>Reason</th>
                    <th style={{ padding: '0.75rem', textAlign: 'left', color: '#6b7280', fontSize: '0.875rem' }}>Status</th>
                    <th style={{ padding: '0.75rem', textAlign: 'left', color: '#6b7280', fontSize: '0.875rem' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {leaves.map((leave) => (
                    <tr key={leave.id} style={{ borderBottom: '1px solid #f3f4f6' }}>
                      <td style={{ padding: '1rem 0.75rem', color: '#1f2937', fontSize: '0.875rem' }}>
                        {getLeaveTypeName(leave.leaveType)}
                      </td>
                      <td style={{ padding: '1rem 0.75rem', color: '#1f2937', fontSize: '0.875rem' }}>
                        {formatDate(leave.startDate)}
                      </td>
                      <td style={{ padding: '1rem 0.75rem', color: '#1f2937', fontSize: '0.875rem' }}>
                        {formatDate(leave.endDate)}
                      </td>
                      <td style={{ padding: '1rem 0.75rem', color: '#1f2937', fontSize: '0.875rem' }}>
                        {Math.ceil((new Date(leave.endDate) - new Date(leave.startDate)) / (1000 * 60 * 60 * 24)) + 1}
                      </td>
                      <td style={{ padding: '1rem 0.75rem', color: '#1f2937', fontSize: '0.875rem', maxWidth: '200px' }}>
                        {leave.reason}
                      </td>
                      <td style={{ padding: '1rem 0.75rem' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                          {getStatusIcon(leave.status)}
                          <span style={{ color: getStatusColor(leave.status), fontSize: '0.875rem', fontWeight: 500 }}>
                            {leave.status}
                          </span>
                        </div>
                      </td>
                      <td style={{ padding: '1rem 0.75rem' }}>
                        {leave.status === 'PENDING' && (
                          <motion.button
                            whileHover={{ scale: 1.1 }}
                            whileTap={{ scale: 0.9 }}
                            onClick={() => handleCancel(leave.id)}
                            style={{
                              background: 'none',
                              border: 'none',
                              cursor: 'pointer',
                              color: '#ef4444',
                              padding: '0.25rem'
                            }}
                          >
                            <Trash2 size={18} />
                          </motion.button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </motion.div>

        {/* Request Modal */}
        {showModal && (
          <div style={modalOverlay} onClick={() => setShowModal(false)}>
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              onClick={(e) => e.stopPropagation()}
              style={modalContent}
            >
              <h2 style={{ fontSize: '1.5rem', fontWeight: '600', color: '#1f2937', marginBottom: '1.5rem' }}>
                Request Leave
              </h2>

              <form onSubmit={handleSubmit}>
                <div style={{ marginBottom: '1rem' }}>
                  <label style={{ display: 'block', marginBottom: '0.5rem', color: '#374151', fontSize: '0.875rem', fontWeight: 500 }}>
                    Leave Type
                  </label>
                  <select
                    value={formData.leaveType}
                    onChange={(e) => setFormData({ ...formData, leaveType: e.target.value })}
                    required
                    style={inputStyle}
                  >
                    <option value="CASUAL">Casual Leave</option>
                    <option value="SICK">Sick Leave</option>
                    <option value="ANNUAL">Annual Leave</option>
                  </select>
                </div>

                <div style={{ marginBottom: '1rem' }}>
                  <label style={{ display: 'block', marginBottom: '0.5rem', color: '#374151', fontSize: '0.875rem', fontWeight: 500 }}>
                    Start Date
                  </label>
                  <input
                    type="date"
                    value={formData.startDate}
                    onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
                    required
                    min={new Date().toISOString().split('T')[0]}
                    style={inputStyle}
                  />
                </div>

                <div style={{ marginBottom: '1rem' }}>
                  <label style={{ display: 'block', marginBottom: '0.5rem', color: '#374151', fontSize: '0.875rem', fontWeight: 500 }}>
                    End Date
                  </label>
                  <input
                    type="date"
                    value={formData.endDate}
                    onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
                    required
                    min={formData.startDate || new Date().toISOString().split('T')[0]}
                    style={inputStyle}
                  />
                </div>

                <div style={{ marginBottom: '1.5rem' }}>
                  <label style={{ display: 'block', marginBottom: '0.5rem', color: '#374151', fontSize: '0.875rem', fontWeight: 500 }}>
                    Reason
                  </label>
                  <textarea
                    value={formData.reason}
                    onChange={(e) => setFormData({ ...formData, reason: e.target.value })}
                    required
                    rows={4}
                    placeholder="Briefly describe the reason for your leave request"
                    style={{ ...inputStyle, resize: 'vertical' }}
                  />
                </div>

                <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
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
                      background: 'linear-gradient(135deg, #4f46e5, #7c3aed)',
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
    </div>
  );
};

const modalOverlay = {
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
};

const modalContent = {
  background: 'white',
  borderRadius: '12px',
  padding: '2rem',
  width: '90%',
  maxWidth: '500px',
  maxHeight: '90vh',
  overflowY: 'auto'
};

const inputStyle = {
  width: '100%',
  padding: '0.75rem',
  border: '1px solid #d1d5db',
  borderRadius: '8px',
  fontSize: '0.875rem',
  color: '#1f2937',
  boxSizing: 'border-box'
};

export default LeaveRequest;
