import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { DollarSign, FileText, TrendingUp, RefreshCw, X, Eye } from 'lucide-react';
import Sidebar from '../components/Sidebar';

const Payroll = () => {
  const [payrollRecords, setPayrollRecords] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showGenerateModal, setShowGenerateModal] = useState(false);
  const [showPayslipModal, setShowPayslipModal] = useState(false);
  const [selectedPayroll, setSelectedPayroll] = useState(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [generateForm, setGenerateForm] = useState({
    month: new Date().getMonth() + 1,
    year: new Date().getFullYear()
  });

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(amount || 0);
  };

  const getMonthName = (month) => {
    const months = ['January', 'February', 'March', 'April', 'May', 'June',
                    'July', 'August', 'September', 'October', 'November', 'December'];
    return months[month - 1] || '';
  };

  const getStatusBadge = (status) => {
    const statusColors = {
      'PENDING': 'warning',
      'PROCESSED': 'info',
      'PAID': 'success'
    };
    return statusColors[status] || 'secondary';
  };

  // Fetch data
  const fetchData = async () => {
    try {
      const token = localStorage.getItem('token');
      const currentDate = new Date();
      const currentMonth = currentDate.getMonth() + 1;
      const currentYear = currentDate.getFullYear();
      const timestamp = new Date().getTime();

      const payrollResponse = await fetch(`/api/payroll/list?t=${timestamp}`, {
        headers: { 
          'Authorization': `Bearer ${token}`,
          'Cache-Control': 'no-cache'
        }
      });

      if (payrollResponse.ok) {
  let payrollData = await payrollResponse.json();

  const groupedMap = new Map();

  payrollData.forEach(item => {
    const key = `${item.employeeId}-${item.month}-${item.year}`;

    if (!groupedMap.has(key) || item.id > groupedMap.get(key).id) {
      groupedMap.set(key, item);
    }
  });

  setPayrollRecords(Array.from(groupedMap.values()));
}

      const employeesResponse = await fetch(`/api/employees?t=${timestamp}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (employeesResponse.ok) {
        setEmployees(await employeesResponse.json());
      }
    } catch (error) {
      console.error('Error fetching data:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [refreshKey]);

  const handleStatusChange = async (payrollId, action) => {
    try {
      const token = localStorage.getItem('token');
      let url = '';
      
      if (action === 'process') url = `/api/payroll/process/${payrollId}`;
      else if (action === 'pay') url = `/api/payroll/pay/${payrollId}`;
      else if (action === 'unpay') url = `/api/payroll/unpay/${payrollId}`;

      const response = await fetch(url, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (response.ok) {
        alert(`Payroll ${action}ed successfully!`);
        fetchData();
      } else {
        alert('Error: ' + await response.text());
      }
    } catch (error) {
      alert('Error: ' + error.message);
    }
  };

  const handleGeneratePayroll = async (e) => {
    e.preventDefault();
    try {
      const token = localStorage.getItem('token');
      const response = await fetch('/api/payroll/generate-all', {
        method: 'POST',
        headers: { 
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          month: generateForm.month,
          year: generateForm.year
        })
      });

      if (response.ok) {
        alert('Payroll generated successfully!');
        setShowGenerateModal(false);
        setRefreshKey(prev => prev + 1);
      } else {
        alert('Error: ' + await response.text());
      }
    } catch (error) {
      alert('Error: ' + error.message);
    }
  };

  const handleViewPayslip = async (payroll) => {
    try {
      const token = localStorage.getItem('token');
      // Use payroll data directly since it now includes attendance info
      setSelectedPayroll(payroll);
      setShowPayslipModal(true);
    } catch (error) {
      alert('Error loading payslip: ' + error.message);
    }
  };

  const totalPayroll = payrollRecords.reduce((sum, r) => sum + (r.netSalary || 0), 0);
  const paidAmount = payrollRecords.filter(r => r.status === 'PAID').reduce((sum, r) => sum + (r.netSalary || 0), 0);
  const pendingAmount = payrollRecords.filter(r => r.status === 'PENDING').reduce((sum, r) => sum + (r.netSalary || 0), 0);

  return (
    <div className="dashboard">
      <Sidebar />
      <main className="main-content">
        <div className="dashboard-header">
          <div>
            <h1 className="dashboard-title">Payroll Management</h1>
            <p style={{ color: '#6b7280', marginTop: '0.25rem' }}>Manage employee salaries and payments</p>
          </div>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              onClick={() => setRefreshKey(prev => prev + 1)}
              style={{
                padding: '0.75rem 1.5rem',
                background: '#4f46e5',
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
              <RefreshCw size={18} />
              Refresh
            </motion.button>
            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              onClick={() => setShowGenerateModal(true)}
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
              <DollarSign size={18} />
              Generate Payroll
            </motion.button>
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '1rem', marginBottom: '1.5rem' }}>
          {[
            { label: 'Total Payroll', value: formatCurrency(totalPayroll), icon: <DollarSign />, color: '#10b981' },
            { label: 'Paid This Month', value: formatCurrency(paidAmount), icon: <TrendingUp />, color: '#6366f1' },
            { label: 'Pending Payments', value: formatCurrency(pendingAmount), icon: <FileText />, color: '#f59e0b' },
            { label: 'Employees', value: employees.length.toString(), icon: <DollarSign />, color: '#06b6d4' },
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

        {loading ? (
          <div style={{ textAlign: 'center', padding: '2rem', color: '#6b7280' }}>Loading payroll data...</div>
        ) : payrollRecords.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '2rem', color: '#6b7280', background: 'white', borderRadius: '12px' }}>
            <FileText size={48} style={{ marginBottom: '1rem', opacity: 0.5 }} />
            <p>No payroll records found.</p>
            <p>Click "Generate Payroll" to create payroll for your employees.</p>
          </div>
        ) : (
          <motion.div
            className="table-container"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <div className="table-header">
              <h3 className="table-title">Payroll Records - {getMonthName(new Date().getMonth() + 1)} {new Date().getFullYear()}</h3>
            </div>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Employee</th>
                  <th>Department</th>
                  <th>Basic Salary</th>
                  <th>Allowances</th>
                  <th>Deductions</th>
                  <th>Net Salary</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {payrollRecords.map((record) => (
                  <tr key={record.id}>
                    <td>
                      <div>
                        <div style={{ fontWeight: 600 }}>{record.employeeName}</div>
                        <div style={{ fontSize: '0.75rem', color: '#6b7280' }}>
                          {record.designation || '-'} • EMP{String(record.employeeId).padStart(3, '0')}
                        </div>
                      </div>
                    </td>
                    <td>{record.department || '-'}</td>
                    <td>{formatCurrency(record.basicSalary)}</td>
                    <td>{formatCurrency((record.hra || 0) + (record.da || 0) + (record.ta || 0))}</td>
                    <td>{formatCurrency(record.totalDeductions)}</td>
                    <td><strong>{formatCurrency(record.netSalary)}</strong></td>
                    <td>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                        <span className={`badge badge-${getStatusBadge(record.status)}`}>
                          {record.status}
                        </span>
                        {record.paymentDate && (
                          <span style={{ fontSize: '0.65rem', color: '#6b7280' }}>
                            Paid: {new Date(record.paymentDate).toLocaleDateString('en-IN')}
                          </span>
                        )}
                      </div>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                        {record.status === 'PENDING' && (
                          <button onClick={() => handleStatusChange(record.id, 'process')}
                            style={{ padding: '0.25rem 0.5rem', background: '#6366f1', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '0.75rem' }}>
                            Process
                          </button>
                        )}
                        {record.status === 'PROCESSED' && (
                          <>
                            <button onClick={() => handleStatusChange(record.id, 'pay')}
                              style={{ padding: '0.25rem 0.5rem', background: '#10b981', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '0.75rem' }}>
                              Mark Paid
                            </button>
                            <button onClick={() => handleStatusChange(record.id, 'unpay')}
                              style={{ padding: '0.25rem 0.5rem', background: '#ef4444', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '0.75rem' }}>
                              Mark Unpaid
                            </button>
                          </>
                        )}
                        {record.status === 'PAID' && (
                          <button onClick={() => handleStatusChange(record.id, 'unpay')}
                            style={{ padding: '0.25rem 0.5rem', background: '#ef4444', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '0.75rem' }}>
                            Mark Unpaid
                          </button>
                        )}
                        <button onClick={() => handleViewPayslip(record)}
                          style={{ padding: '0.25rem 0.5rem', background: '#4f46e5', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '0.25rem', fontSize: '0.75rem' }}>
                          <Eye size={12} />
                          View
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </motion.div>
        )}

        {/* Generate Payroll Modal */}
        {showGenerateModal && (
          <div style={{
            position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
            background: 'rgba(0, 0, 0, 0.5)', display: 'flex',
            alignItems: 'center', justifyContent: 'center', zIndex: 1000
          }}>
            <motion.div
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              style={{ background: 'white', borderRadius: '12px', padding: '2rem', width: '400px' }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                <h2 style={{ fontSize: '1.25rem', fontWeight: 600 }}>Generate Payroll</h2>
                <button onClick={() => setShowGenerateModal(false)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                  <X size={20} />
                </button>
              </div>
              <form onSubmit={handleGeneratePayroll}>
                <div style={{ marginBottom: '1rem' }}>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>Month</label>
                  <select
                    value={generateForm.month}
                    onChange={(e) => setGenerateForm({...generateForm, month: parseInt(e.target.value)})}
                    style={{ width: '100%', padding: '0.75rem', border: '1px solid #e5e7eb', borderRadius: '8px', fontSize: '0.875rem' }}
                  >
                    {[1,2,3,4,5,6,7,8,9,10,11,12].map(m => (
                      <option key={m} value={m}>{getMonthName(m)}</option>
                    ))}
                  </select>
                </div>
                <div style={{ marginBottom: '1.5rem' }}>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>Year</label>
                  <input
                    type="number"
                    value={generateForm.year}
                    onChange={(e) => setGenerateForm({...generateForm, year: parseInt(e.target.value)})}
                    style={{ width: '100%', padding: '0.75rem', border: '1px solid #e5e7eb', borderRadius: '8px', fontSize: '0.875rem' }}
                  />
                </div>
                <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end' }}>
                  <button type="button" onClick={() => setShowGenerateModal(false)}
                    style={{ padding: '0.75rem 1.5rem', border: '1px solid #e5e7eb', borderRadius: '8px', background: 'white', cursor: 'pointer', fontWeight: 500 }}>
                    Cancel
                  </button>
                  <button type="submit"
                    style={{ padding: '0.75rem 1.5rem', background: 'linear-gradient(135deg, #4f46e5, #06b6d4)', color: 'white', border: 'none', borderRadius: '8px', cursor: 'pointer', fontWeight: 500 }}>
                    Generate
                  </button>
                </div>
              </form>
            </motion.div>
          </div>
        )}

        {/* View Payslip Modal */}
        {showPayslipModal && selectedPayroll && (
          <div style={{
            position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
            background: 'rgba(0, 0, 0, 0.5)', display: 'flex',
            alignItems: 'center', justifyContent: 'center', zIndex: 1000
          }}>
            <motion.div
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              style={{
                background: 'white', borderRadius: '12px', padding: '2rem', width: '600px',
                maxHeight: '90vh', overflow: 'auto'
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                <h2 style={{ fontSize: '1.25rem', fontWeight: 600 }}>Payslip Details</h2>
                <button onClick={() => { setShowPayslipModal(false); setSelectedPayroll(null); }} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                  <X size={20} />
                </button>
              </div>
              
              <div style={{ background: '#f9fafb', padding: '1rem', borderRadius: '8px', marginBottom: '1rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                  <span style={{ color: '#6b7280' }}>Employee:</span>
                  <span style={{ fontWeight: 600 }}>{selectedPayroll.employeeName}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                  <span style={{ color: '#6b7280' }}>Department:</span>
                  <span>{selectedPayroll.department || '-'}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                  <span style={{ color: '#6b7280' }}>Period:</span>
                  <span>{getMonthName(selectedPayroll.month)} {selectedPayroll.year}</span>
                </div>
                {/* Attendance Summary */}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.5rem', marginTop: '0.75rem', paddingTop: '0.75rem', borderTop: '1px solid #e5e7eb' }}>
                  <div style={{ textAlign: 'center' }}>
                    <div style={{ fontSize: '0.75rem', color: '#6b7280' }}>Present</div>
                    <div style={{ fontSize: '1.25rem', fontWeight: 600, color: '#10b981' }}>{selectedPayroll.presentDays || 0}</div>
                  </div>
                  <div style={{ textAlign: 'center' }}>
                    <div style={{ fontSize: '0.75rem', color: '#6b7280' }}>Absent</div>
                    <div style={{ fontSize: '1.25rem', fontWeight: 600, color: '#ef4444' }}>{selectedPayroll.absentDays || 0}</div>
                  </div>
                  <div style={{ textAlign: 'center' }}>
                    <div style={{ fontSize: '0.75rem', color: '#6b7280' }}>Leave</div>
                    <div style={{ fontSize: '1.25rem', fontWeight: 600, color: '#f59e0b' }}>{selectedPayroll.leaveDays || 0}</div>
                  </div>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
                <div>
                  <h4 style={{ marginBottom: '0.75rem', color: '#10b981' }}>Earnings</h4>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.25rem' }}>
                    <span>Basic Salary:</span>
                    <span>{formatCurrency(selectedPayroll.basicSalary)}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.25rem' }}>
                    <span>HRA (20%):</span>
                    <span>{formatCurrency(selectedPayroll.hra)}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.25rem' }}>
                    <span>DA (10%):</span>
                    <span>{formatCurrency(selectedPayroll.da)}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.25rem' }}>
                    <span>TA (5%):</span>
                    <span>{formatCurrency(selectedPayroll.ta)}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 600, borderTop: '1px solid #e5e7eb', paddingTop: '0.5rem', marginTop: '0.5rem' }}>
                    <span>Gross Salary:</span>
                    <span>{formatCurrency(selectedPayroll.grossSalary)}</span>
                  </div>
                </div>
                <div>
                  <h4 style={{ marginBottom: '0.75rem', color: '#ef4444' }}>Deductions</h4>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.25rem' }}>
                    <span>PF (12%):</span>
                    <span>{formatCurrency(selectedPayroll.providentFund)}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.25rem' }}>
                    <span>Tax:</span>
                    <span>{formatCurrency(selectedPayroll.tax)}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.25rem' }}>
                    <span>Insurance:</span>
                    <span>{formatCurrency(selectedPayroll.insurance)}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 600, borderTop: '1px solid #e5e7eb', paddingTop: '0.5rem', marginTop: '0.5rem' }}>
                    <span>Total Deductions:</span>
                    <span>{formatCurrency(selectedPayroll.totalDeductions)}</span>
                  </div>
                </div>
              </div>

              {/* Salary Calculation Breakdown */}
              {(selectedPayroll.absentDays > 0 || selectedPayroll.leaveDays > 0) && (
                <div style={{ background: '#fffbeb', padding: '1rem', borderRadius: '8px', marginBottom: '1rem', border: '1px solid #fcd34d' }}>
                  <h4 style={{ marginBottom: '0.75rem', color: '#b45309', fontSize: '0.875rem', fontWeight: 600 }}>💰 Salary Calculation Details</h4>
                  
                  {/* Per Day Salary */}
                  <div style={{ marginBottom: '0.75rem' }}>
                    <div style={{ fontSize: '0.875rem', color: '#6b7280', marginBottom: '0.25rem' }}>Per Day Basic Salary:</div>
                    <div style={{ fontSize: '1rem', fontWeight: 600, color: '#10b981' }}>
                      {formatCurrency((selectedPayroll.basicSalary || 0) / 30)} per day
                    </div>
                  </div>

                  {/* Absent Days Deduction */}
                  {selectedPayroll.absentDays > 0 && (
                    <div style={{ background: '#fef2f2', padding: '0.75rem', borderRadius: '6px', marginBottom: '0.5rem' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.25rem', fontSize: '0.875rem' }}>
                        <span style={{ color: '#6b7280' }}>Absent Days ({selectedPayroll.absentDays} days):</span>
                        <span style={{ fontWeight: 600, color: '#ef4444' }}>
                          -{formatCurrency(((selectedPayroll.basicSalary || 0) / 30) * selectedPayroll.absentDays)}
                        </span>
                      </div>
                      <div style={{ fontSize: '0.75rem', color: '#991b1b' }}>
                        Salary deducted for unauthorized absence
                      </div>
                    </div>
                  )}
                  .dashboard {
  display: flex;
}

.main-content {
  flex: 1;
  min-width: 0;   /* ⭐ KEY FIX */
  padding: 1rem;
}

.table-container {
  width: 100%;
  overflow-x: auto;
}

.data-table {
  min-width: 1200px;
  width: max-content;
}


                  {/* Leave Days */}
                  {selectedPayroll.leaveDays > 0 && (
                    <div style={{ background: '#fef3c7', padding: '0.75rem', borderRadius: '6px' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.25rem', fontSize: '0.875rem' }}>
                        <span style={{ color: '#6b7280' }}>Leave Days ({selectedPayroll.leaveDays} days):</span>
                        <span style={{ fontWeight: 600, color: '#f59e0b' }}>
                          PAID (Approved Leave)
                        </span>
                      </div>
                      <div style={{ fontSize: '0.75rem', color: '#92400e' }}>
                        These days are included in paid salary
                      </div>
                    </div>
                  )}
                </div>
              )}

              <div style={{ background: '#4f46e5', color: 'white', padding: '1rem', borderRadius: '8px', textAlign: 'center' }}>
                <div style={{ fontSize: '0.875rem', marginBottom: '0.25rem' }}>Net Salary</div>
                <div style={{ fontSize: '1.5rem', fontWeight: 600 }}>{formatCurrency(selectedPayroll.netSalary)}</div>
              </div>

              <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end', marginTop: '1.5rem' }}>
                <button onClick={() => { setShowPayslipModal(false); setSelectedPayroll(null); }}
                  style={{ padding: '0.75rem 1.5rem', background: '#4f46e5', color: 'white', border: 'none', borderRadius: '8px', cursor: 'pointer', fontWeight: 500 }}>
                  Close
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </main>
    </div>
  );
};

export default Payroll;
