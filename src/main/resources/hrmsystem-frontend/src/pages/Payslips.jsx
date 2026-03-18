import { motion } from 'framer-motion';
import { FileText, Download, Eye, Search } from 'lucide-react';
import Sidebar from '../components/Sidebar';
import { useState } from 'react';

const Payslips = () => {
  const [searchTerm, setSearchTerm] = useState('');

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(amount);
  };
  
  const payslips = [
    { id: 1, employee: 'John Smith', month: 'January 2024', basicSalary: 5000, netSalary: 5300, generated: '2024-01-31' },
    { id: 2, employee: 'Sarah Johnson', month: 'January 2024', basicSalary: 4500, netSalary: 4720, generated: '2024-01-31' },
    { id: 3, employee: 'Michael Brown', month: 'January 2024', basicSalary: 4000, netSalary: 4450, generated: '2024-01-31' },
    { id: 4, employee: 'Emily Davis', month: 'January 2024', basicSalary: 4200, netSalary: 4480, generated: '2024-01-31' },
    { id: 5, employee: 'John Smith', month: 'December 2023', basicSalary: 5000, netSalary: 5300, generated: '2023-12-31' },
    { id: 6, employee: 'Sarah Johnson', month: 'December 2023', basicSalary: 4500, netSalary: 4720, generated: '2023-12-31' },
  ];

  const filteredPayslips = payslips.filter(p => 
    p.employee.toLowerCase().includes(searchTerm.toLowerCase()) ||
    p.month.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="dashboard">
      <Sidebar />
      <main className="main-content">
        <div className="dashboard-header">
          <div>
            <h1 className="dashboard-title">Payslips</h1>
            <p style={{ color: '#6b7280', marginTop: '0.25rem' }}>View and download employee payslips</p>
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
          {filteredPayslips.map((payslip, index) => (
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
                    <h4 style={{ fontWeight: 600 }}>{payslip.employee}</h4>
                    <p style={{ fontSize: '0.875rem', color: '#6b7280' }}>{payslip.month}</p>
                  </div>
                </div>
              </div>
              
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', marginBottom: '1rem' }}>
                <div>
                  <p style={{ fontSize: '0.75rem', color: '#6b7280' }}>Basic Salary</p>
                  <p style={{ fontWeight: 600 }}>{formatCurrency(payslip.basicSalary)}</p>
                </div>
                <div>
                  <p style={{ fontSize: '0.75rem', color: '#6b7280' }}>Net Salary</p>
                  <p style={{ fontWeight: 600, color: '#10b981' }}>{formatCurrency(payslip.netSalary)}</p>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button style={{
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
                }}>
                  <Eye size={16} />
                  View
                </button>
                <button style={{
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
                }}>
                  <Download size={16} />
                  Download
                </button>
              </div>
            </motion.div>
          ))}
        </div>
      </main>
    </div>
  );
};

export default Payslips;
