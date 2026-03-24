import { motion } from 'framer-motion';
import { FileText, Download, Eye, Search, RefreshCw } from 'lucide-react';
import Sidebar from '../components/Sidebar';
import { useState, useEffect } from 'react';

const Payslips = () => {
 const [payslips, setPayslips] = useState([]);
const [loading, setLoading] = useState(true);
const [error, setError] = useState(null);
const [refreshKey, setRefreshKey] = useState(0); // Force refresh trigger
const [searchTerm, setSearchTerm] = useState('');

  // Fetch payslips from localhost:8080 - ALWAYS fetch fresh data
  useEffect(() => {
    console.log('[v0] Fetching FRESH payslips data... (refreshKey:', refreshKey, ')');
    const fetchPayslips = async () => {
      try {
        setLoading(true);
        const token = localStorage.getItem('token');
        
        // Add cache-busting timestamp to prevent browser caching
        const timestamp = new Date().getTime();
        
        const response = await fetch(`http://localhost:8080/api/payslips?t=${timestamp}`, {
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
  }, [refreshKey]); // Re-fetch when refreshKey changes

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
  
  // Filter by employee name or month-year
  const filteredPayslips = payslips.filter(p => {
    const employeeName = p.employee ? `${p.employee.firstName || ''} ${p.employee.lastName || ''}`.toLowerCase() : '';
    const monthYear = p.monthYear || '';
    const searchTermLower = searchTerm.toLowerCase();
    return employeeName.includes(searchTermLower) || monthYear.includes(searchTermLower);
  });

  if (loading) return <div>Loading payslips...</div>;
if (error) return <div style={{ color: 'red' }}>Error: {error}</div>;

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
            const employeeName = payslip.employee ? 
              `${payslip.employee.firstName || ''} ${payslip.employee.lastName || ''}`.trim() : 'Unknown';
            const monthYearDisplay = payslip.monthYear || 'N/A';
            
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
                  onClick={() => console.log('View payslip:', payslip.id)}
                  style={{
                    flex: 1,
                    padding: '0.5rem',
                    background: '#4f46e515',
                    color: '#4f46e5',
                    border: 'none',
                    borderRadius: '6px',
                    cursor: 'pointer',
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
                  onClick={() => console.log('Download payslip:', payslip.id)}
                  style={{
                    flex: 1,
                    padding: '0.5rem',
                    background: '#10b98115',
                    color: '#10b981',
                    border: 'none',
                    borderRadius: '6px',
                    cursor: 'pointer',
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
      </main>
    </div>
  );
};

export default Payslips;