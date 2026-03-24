import { motion } from 'framer-motion';
import { FileText, Download, Eye, Search, RefreshCw, X } from 'lucide-react';
import Sidebar from '../components/Sidebar';
import { useState, useEffect } from 'react';

const Payslips = () => {
  const [payslips, setPayslips] = useState([]);
  const [loading, setLoading] = useState(true);
  const [regenerating, setRegenerating] = useState(false);
  const [error, setError] = useState(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedMonth, setSelectedMonth] = useState(new Date().getMonth() + 1);
  const [selectedYear, setSelectedYear] = useState(new Date().getFullYear());
  const [showViewModal, setShowViewModal] = useState(false);
  const [selectedPayslip, setSelectedPayslip] = useState(null);

  // Fetch payslips from localhost:8080 - ALWAYS fetch fresh data
  useEffect(() => {
    console.log('[v0] Fetching FRESH payslips data... (refreshKey:', refreshKey, ')');
    const fetchPayslips = async () => {
      try {
        setLoading(true);
        const token = localStorage.getItem('token');
        
        // Add cache-busting timestamp to prevent browser caching
        const timestamp = new Date().getTime();

        const monthYear = `${selectedYear}-${String(selectedMonth).padStart(2, '0')}`;
        
        const response = await fetch(`/api/payslips/month/${monthYear}?t=${timestamp}`, {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
            'Cache-Control': 'no-cache, no-store, must-revalidate',
            'Pragma': 'no-cache'
          }
        });
        
        if (!response.ok) throw new Error('Failed to fetch payslips');
        
        const data = await response.json();
        console.log('[v0] Real payslips fetched from API:', data);
        
        // Ensure we're setting fresh data (not appending)
        const freshData = Array.isArray(data) ? data : data.payslips || [];
        console.log('[v0] Setting', freshData.length, 'payslip records');
        setPayslips(freshData); // REPLACE completely
      } catch (err) {
        console.error('[v0] Error fetching payslips:', err);
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchPayslips();
  }, [refreshKey, selectedMonth, selectedYear]); // Re-fetch when month/year changes

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(amount);
  };
  
  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-IN');
  };
  
  const getAuthHeaders = () => ({
    'Authorization': `Bearer ${localStorage.getItem('token')}`,
    'Content-Type': 'application/json'
  });

  const monthYear = `${selectedYear}-${String(selectedMonth).padStart(2, '0')}`;

  const handleRegenerateMonthPayslips = async () => {
    const ok = window.confirm(`This will DELETE and regenerate all payslips for ${monthYear}. Continue?`);
    if (!ok) return;

    try {
      setRegenerating(true);
      const token = localStorage.getItem('token');
      const headers = { 'Authorization': `Bearer ${token}` };

      const response = await fetch(`/api/payslips/regenerate-month?monthYear=${monthYear}`, {
        method: 'POST',
        headers
      });

      if (!response.ok) throw new Error('Failed to regenerate payslips');
      setRefreshKey(prev => prev + 1);
    } catch (err) {
      alert('Regenerate failed: ' + err.message);
    } finally {
      setRegenerating(false);
    }
  };

  const handleViewPayslip = async (payslip) => {
    try {
      if (payslip?.payrollStatus !== 'PAID') {
        alert('Payroll is not paid for this month. Payslip view will be available after payment.');
        return;
      }
      // Always regenerate so attendance changes reflect correct salary/deductions.
      const token = localStorage.getItem('token');
      const headers = { 'Authorization': `Bearer ${token}` };
      const employeeId = payslip.employeeId;
      const monthYearForPayslip = payslip.monthYear;

      const regenResponse = await fetch(
        `/api/payslips/generate?employeeId=${employeeId}&monthYear=${monthYearForPayslip}`,
        { method: 'POST', headers }
      );

      if (regenResponse.ok) {
        const regeneratedPayslip = await regenResponse.json();
        setSelectedPayslip(regeneratedPayslip);
      } else {
        const response = await fetch(`/api/payslips/${payslip.id}`, { headers: getAuthHeaders() });
        if (response.ok) {
          const data = await response.json();
          setSelectedPayslip(data);
        } else {
          setSelectedPayslip(payslip);
        }
      }

      setShowViewModal(true);
    } catch (err) {
      setSelectedPayslip(payslip);
      setShowViewModal(true);
    }
  };

  const handleDownloadPayslip = async (payslip) => {
    try {
      if (payslip?.payrollStatus !== 'PAID') {
        alert('Payroll is not paid for this month. Payslip download will be available after payment.');
        return;
      }
      const token = localStorage.getItem('token');
      const headers = { 'Authorization': `Bearer ${token}` };
      
      // Regenerate payslip first so deductions match latest attendance.
      const regenResponse = await fetch(
        `/api/payslips/generate?employeeId=${payslip.employeeId}&monthYear=${payslip.monthYear}`,
        { method: 'POST', headers }
      );

      if (!regenResponse.ok) {
        throw new Error('Could not regenerate payslip before download');
      }

      const regeneratedPayslip = await regenResponse.json();
      const updatedId = regeneratedPayslip.id || payslip.id;

      // Generate PDF then download.
      const genPdfResponse = await fetch(`/api/payslips/${updatedId}/generate-pdf`, { headers });
      if (!genPdfResponse.ok) {
        const msg = await genPdfResponse.text().catch(() => '');
        throw new Error(msg || 'Could not generate PDF');
      }

      const response = await fetch(`/api/payslips/${updatedId}/download-pdf`, { headers });
      if (response.ok) {
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        const empName = regeneratedPayslip.employeeName ? regeneratedPayslip.employeeName.replace(/\s+/g, '_') : payslip.id;
        a.download = `Payslip_${empName}_${regeneratedPayslip.monthYear || 'unknown'}.pdf`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
      } else {
        const msg = await response.text().catch(() => '');
        alert(msg ? `Could not download PDF: ${msg}` : 'Could not download PDF.');
      }
    } catch (err) {
      alert('Download failed: ' + err.message);
    }
  };

  // Filter by employee name or month-year
  const filteredPayslips = payslips.filter(p => {
    const employeeName = (p.employeeName || (p.employee ? `${p.employee.firstName || ''} ${p.employee.lastName || ''}`.trim() : '')).toLowerCase();
    const monthYear = p.monthYear || '';
    const searchTermLower = searchTerm.toLowerCase();
    return employeeName.includes(searchTermLower) || monthYear.includes(searchTermLower);
  });

  if (loading) {
    return (
      <div className="dashboard">
        <Sidebar />
        <main className="main-content"><div style={{ textAlign: 'center', padding: '2rem', color: '#6b7280' }}>Loading payslips...</div></main>
      </div>
    );
  }
  if (error) {
    return (
      <div className="dashboard">
        <Sidebar />
        <main className="main-content"><div style={{ color: 'red', padding: '2rem' }}>Error: {error}</div></main>
      </div>
    );
  }

  return (
    <div className="dashboard">
      <Sidebar />
      <main className="main-content">
        <div className="dashboard-header">
          <div>
            <h1 className="dashboard-title">Payslips</h1>
            <p style={{ color: '#6b7280', marginTop: '0.25rem' }}>View and download employee payslips</p>
          </div>
          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={() => setRefreshKey(prev => prev + 1)}
            title="Refresh data"
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
            onClick={handleRegenerateMonthPayslips}
            title="Clear & regenerate payslips for selected month"
            disabled={regenerating}
            style={{
              padding: '0.75rem 1.5rem',
              background: '#111827',
              color: 'white',
              border: 'none',
              borderRadius: '8px',
              fontWeight: 600,
              cursor: regenerating ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              opacity: regenerating ? 0.7 : 1
            }}
          >
            {regenerating ? 'Regenerating...' : 'Clear & Regenerate'}
          </motion.button>
        </div>

        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', marginBottom: '1.5rem' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
            <label style={{ fontWeight: 500, color: '#6b7280' }}>Month</label>
            <select
              value={selectedMonth}
              onChange={(e) => setSelectedMonth(parseInt(e.target.value))}
              style={{ padding: '0.65rem', borderRadius: '8px', border: '1px solid #e5e7eb', background: 'white' }}
            >
              {[1,2,3,4,5,6,7,8,9,10,11,12].map(m => (
                <option key={m} value={m}>
                  {m}
                </option>
              ))}
            </select>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
            <label style={{ fontWeight: 500, color: '#6b7280' }}>Year</label>
            <input
              type="number"
              value={selectedYear}
              onChange={(e) => setSelectedYear(parseInt(e.target.value) || new Date().getFullYear())}
              style={{ padding: '0.65rem', width: '120px', borderRadius: '8px', border: '1px solid #e5e7eb', background: 'white' }}
            />
          </div>
        </div>

        <div style={{ marginBottom: '1.5rem' }}>
          <div style={{ position: 'relative', maxWidth: '400px' }}>
            <Search size={20} style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)', color: '#9ca3af' }} />
            <input
              type="text"
              placeholder="Search by employee or month..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              style={{
                width: '100%',
                padding: '0.75rem 1rem 0.75rem 3rem',
                border: '1px solid #e5e7eb',
                borderRadius: '8px',
                fontSize: '0.875rem'
              }}
            />
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(350px, 1fr))', gap: '1rem' }}>
          {filteredPayslips.map((payslip, index) => {
            const employeeName = payslip.employeeName || (payslip.employee ? 
              `${payslip.employee.firstName || ''} ${payslip.employee.lastName || ''}`.trim() : null) || 'Unknown';
            const monthYearDisplay = payslip.monthYear || 'N/A';
            const isPaid = payslip.payrollStatus === 'PAID';
            
            return (
            <motion.div
              key={payslip.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.05 }}
              style={{
                background: 'white',
                borderRadius: '12px',
                padding: '1.5rem',
                boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)'
              }}
            >
              <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '1rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  <div style={{
                    width: '48px',
                    height: '48px',
                    borderRadius: '12px',
                    background: 'linear-gradient(135deg, #4f46e5, #06b6d4)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: 'white'
                  }}>
                    <FileText size={24} />
                  </div>
                  <div>
                    <h4 style={{ fontWeight: 600 }}>{employeeName}</h4>
                    <p style={{ fontSize: '0.875rem', color: '#6b7280' }}>{monthYearDisplay}</p>
                    {!isPaid && (
                      <p style={{ fontSize: '0.8rem', color: '#9ca3af', marginTop: '0.25rem' }}>Not Paid</p>
                    )}
                  </div>
                </div>
              </div>
              
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', marginBottom: '1rem' }}>
                <div>
                  <p style={{ fontSize: '0.75rem', color: '#6b7280' }}>Basic Salary</p>
                  <p style={{ fontWeight: 600 }}>{formatCurrency(payslip.basicSalary || 0)}</p>
                </div>
                <div>
                  <p style={{ fontSize: '0.75rem', color: '#6b7280' }}>Net Salary</p>
                  <p style={{ fontWeight: 600, color: '#10b981' }}>{formatCurrency(payslip.netSalary || 0)}</p>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button 
                  onClick={() => handleViewPayslip(payslip)}
                  disabled={!isPaid}
                  style={{
                    flex: 1,
                    padding: '0.5rem',
                    background: isPaid ? '#4f46e515' : '#e5e7eb',
                    color: isPaid ? '#4f46e5' : '#6b7280',
                    border: 'none',
                    borderRadius: '6px',
                    cursor: isPaid ? 'pointer' : 'not-allowed',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '0.25rem',
                    fontSize: '0.875rem'
                  }}
                >
                  <Eye size={16} />
                  View
                </button>
                <button 
                  onClick={() => handleDownloadPayslip(payslip)}
                  disabled={!isPaid}
                  style={{
                    flex: 1,
                    padding: '0.5rem',
                    background: isPaid ? '#10b98115' : '#e5e7eb',
                    color: isPaid ? '#10b981' : '#6b7280',
                    border: 'none',
                    borderRadius: '6px',
                    cursor: isPaid ? 'pointer' : 'not-allowed',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '0.25rem',
                    fontSize: '0.875rem'
                  }}
                >
                  <Download size={16} />
                  Download
                </button>
              </div>
            </motion.div>
          );
          })}
        </div>

        {/* View Payslip Modal */}
        {showViewModal && selectedPayslip && (
          <div style={{
            position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
            background: 'rgba(0, 0, 0, 0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000
          }}>
            <motion.div
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              style={{ background: 'white', borderRadius: '12px', padding: '2rem', width: '450px', maxHeight: '90vh', overflow: 'auto' }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                <h3 style={{ fontSize: '1.25rem', fontWeight: 600 }}>Payslip Details</h3>
                <button onClick={() => { setShowViewModal(false); setSelectedPayslip(null); }} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                  <X size={20} />
                </button>
              </div>
              <div style={{ display: 'grid', gap: '0.75rem' }}>
                <div><span style={{ color: '#6b7280' }}>Employee:</span> {selectedPayslip.employeeName || (selectedPayslip.employee ? `${selectedPayslip.employee.firstName || ''} ${selectedPayslip.employee.lastName || ''}`.trim() : '') || 'N/A'}</div>
                <div><span style={{ color: '#6b7280' }}>Period:</span> {selectedPayslip.monthYear || 'N/A'}</div>
                <div><span style={{ color: '#6b7280' }}>Basic Salary:</span> {formatCurrency(selectedPayslip.basicSalary || 0)}</div>
                <div><span style={{ color: '#6b7280' }}>Gross Salary:</span> {formatCurrency(selectedPayslip.grossSalary || 0)}</div>
                <div><span style={{ color: '#6b7280' }}>Deductions:</span> {formatCurrency(selectedPayslip.totalDeduction ?? selectedPayslip.totalDeductions ?? 0)}</div>
                {(selectedPayslip.absentLeaveDeduction > 0 || (selectedPayslip.absentDays || 0) > 0 || (selectedPayslip.leaveDays || 0) > 0 || (selectedPayslip.halfDays || 0) > 0) && (
                  <div style={{ background: '#fffbeb', padding: '0.75rem', borderRadius: '8px', border: '2px solid #fcd34d', marginTop: '0.5rem' }}>
                    <div style={{ fontSize: '0.8rem', fontWeight: 600, color: '#b45309', marginBottom: '0.5rem' }}>💰 Attendance Deduction</div>
                    <div style={{ fontSize: '0.8rem', color: '#92400e' }}>Present: {selectedPayslip.presentDays || 0} | Absent: {selectedPayslip.absentDays || 0} | Leave: {selectedPayslip.leaveDays || 0} | Half Day: {selectedPayslip.halfDays || 0}</div>
                    {(selectedPayslip.absentLeaveDeduction > 0) && (
                      <div style={{ fontSize: '0.8rem', fontWeight: 600, color: '#b45309', marginTop: '0.25rem' }}>Deducted: −{formatCurrency(selectedPayslip.absentLeaveDeduction)}</div>
                    )}
                  </div>
                )}
                <div style={{ fontWeight: 600, color: '#10b981', fontSize: '1.25rem' }}>Net Salary: {formatCurrency(selectedPayslip.netSalary || 0)}</div>
              </div>
            </motion.div>
          </div>
        )}
      </main>
    </div>
  );
};

export default Payslips;