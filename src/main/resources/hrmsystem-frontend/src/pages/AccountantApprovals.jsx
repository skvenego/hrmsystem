import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  CheckCircle, 
  XCircle, 
  Clock, 
  DollarSign, 
  Users, 
  FileText,
  Search,
  Filter,
  Download,
  Eye,
  MessageSquare,
  AlertCircle
} from 'lucide-react';

const AccountantApprovals = () => {
  const [pendingApprovals, setPendingApprovals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedPayroll, setSelectedPayroll] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [comments, setComments] = useState('');
  const [processing, setProcessing] = useState(false);

  useEffect(() => {
    fetchPendingApprovals();
  }, []);

  const fetchPendingApprovals = async () => {
    try {
      setLoading(true);
      // Mock data - replace with actual API call
      const response = await fetch('/api/payroll/approvals/pending/accountant', {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      });
      
      if (response.ok) {
        const data = await response.json();
        setPendingApprovals(data);
      } else {
        // Use mock data for demonstration
        setPendingApprovals(getMockData());
      }
    } catch (err) {
      setError('Failed to load pending approvals');
      setPendingApprovals(getMockData());
    } finally {
      setLoading(false);
    }
  };

  const getMockData = () => [
    {
      id: 21,
      approvalId: 20,
      employeeName: 'Ram Murat',
      employeeId: 8,
      month: 10,
      year: 2026,
      netSalary: 44820,
      grossSalary: 55000,
      basicSalary: 30000,
      hra: 12000,
      da: 8000,
      ta: 3000,
      providentFund: 3600,
      tax: 2580,
      totalDeductions: 10180
    },
    {
      id: 40,
      approvalId: 28,
      employeeName: 'deepak rajpoot',
      employeeId: 10,
      month: 6,
      year: 2026,
      netSalary: 57180,
      grossSalary: 70000,
      basicSalary: 40000,
      hra: 16000,
      da: 10000,
      ta: 4000,
      providentFund: 4800,
      tax: 4020,
      totalDeductions: 12820
    }
  ];

  const handleApprove = async (payrollId) => {
    if (!window.confirm('Are you sure you want to approve this payroll?')) return;
    
    try {
      setProcessing(true);
      const response = await fetch(`/api/payroll/approvals/${payrollId}/accountant/approve`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ comments }),
      });

      if (response.ok) {
        alert('✅ Payroll approved successfully!');
        setShowModal(false);
        setComments('');
        fetchPendingApprovals();
      } else {
        const data = await response.json();
        alert(`❌ Error: ${data.error}`);
      }
    } catch (err) {
      alert('❌ Failed to approve payroll');
    } finally {
      setProcessing(false);
    }
  };

  const handleReject = async (payrollId) => {
    const reason = prompt('Please enter rejection reason:');
    if (!reason) return;
    
    try {
      setProcessing(true);
      const response = await fetch(`/api/payroll/approvals/${payrollId}/accountant/reject`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ reason }),
      });

      if (response.ok) {
        alert('✅ Payroll rejected and sent back for correction');
        setShowModal(false);
        setComments('');
        fetchPendingApprovals();
      } else {
        const data = await response.json();
        alert(`❌ Error: ${data.error}`);
      }
    } catch (err) {
      alert('❌ Failed to reject payroll');
    } finally {
      setProcessing(false);
    }
  };

  const filteredApprovals = pendingApprovals.filter(approval =>
    approval.employeeName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    approval.employeeId.toString().includes(searchTerm)
  );

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(amount);
  };

  const getMonthName = (month) => {
    const months = ['January', 'February', 'March', 'April', 'May', 'June', 
                    'July', 'August', 'September', 'October', 'November', 'December'];
    return months[month - 1];
  };

  if (loading) {
    return (
      <div style={styles.loadingContainer}>
        <div style={styles.spinner} />
        <p style={styles.loadingText}>Loading pending approvals...</p>
      </div>
    );
  }

  return (
    <div style={styles.container}>
      {/* Header */}
      <motion.div 
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        style={styles.header}
      >
        <div style={styles.headerContent}>
          <div>
            <h1 style={styles.title}>Payroll Approvals - Accountant</h1>
            <p style={styles.subtitle}>Review and approve payroll calculations</p>
          </div>
          <div style={styles.statsContainer}>
            <div style={styles.statCard}>
              <Clock size={24} color="#4f46e5" />
              <div>
                <div style={styles.statValue}>{pendingApprovals.length}</div>
                <div style={styles.statLabel}>Pending</div>
              </div>
            </div>
          </div>
        </div>
      </motion.div>

      {/* Search and Filters */}
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        style={styles.toolbar}
      >
        <div style={styles.searchBox}>
          <Search size={20} color="#9ca3af" />
          <input
            type="text"
            placeholder="Search by employee name or ID..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={styles.searchInput}
          />
        </div>
        <div style={styles.filterButtons}>
          <button style={styles.filterButton}>
            <Filter size={18} />
            <span>Filter</span>
          </button>
          <button style={styles.exportButton}>
            <Download size={18} />
            <span>Export</span>
          </button>
        </div>
      </motion.div>

      {/* Error Message */}
      {error && (
        <motion.div 
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          style={styles.errorBox}
        >
          <AlertCircle size={20} color="#dc2626" />
          <span style={styles.errorText}>{error}</span>
        </motion.div>
      )}

      {/* Approvals Grid */}
      <div style={styles.grid}>
        {filteredApprovals.map((approval, index) => (
          <motion.div
            key={approval.id}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: index * 0.05 }}
            style={styles.card}
          >
            {/* Card Header */}
            <div style={styles.cardHeader}>
              <div style={styles.employeeInfo}>
                <div style={styles.avatar}>
                  <Users size={24} color="#4f46e5" />
                </div>
                <div>
                  <h3 style={styles.employeeName}>{approval.employeeName}</h3>
                  <p style={styles.employeeId}>ID: {approval.employeeId}</p>
                </div>
              </div>
              <div style={styles.periodBadge}>
                {getMonthName(approval.month)} {approval.year}
              </div>
            </div>

            {/* Salary Breakdown */}
            <div style={styles.salaryGrid}>
              <div style={styles.salarySection}>
                <h4 style={styles.sectionTitle}>Earnings</h4>
                <div style={styles.salaryItem}>
                  <span style={styles.label}>Basic Salary:</span>
                  <span style={styles.amount}>{formatCurrency(approval.basicSalary)}</span>
                </div>
                <div style={styles.salaryItem}>
                  <span style={styles.label}>HRA:</span>
                  <span style={styles.amount}>{formatCurrency(approval.hra)}</span>
                </div>
                <div style={styles.salaryItem}>
                  <span style={styles.label}>DA:</span>
                  <span style={styles.amount}>{formatCurrency(approval.da)}</span>
                </div>
                <div style={styles.salaryItem}>
                  <span style={styles.label}>TA:</span>
                  <span style={styles.amount}>{formatCurrency(approval.ta)}</span>
                </div>
                <div style={styles.totalRow}>
                  <span style={styles.totalLabel}>Gross Salary:</span>
                  <span style={styles.totalAmount}>{formatCurrency(approval.grossSalary)}</span>
                </div>
              </div>

              <div style={styles.salarySection}>
                <h4 style={styles.sectionTitle}>Deductions</h4>
                <div style={styles.salaryItem}>
                  <span style={styles.label}>Provident Fund:</span>
                  <span style={styles.amount}>{formatCurrency(approval.providentFund)}</span>
                </div>
                <div style={styles.salaryItem}>
                  <span style={styles.label}>Tax:</span>
                  <span style={styles.amount}>{formatCurrency(approval.tax)}</span>
                </div>
                <div style={styles.salaryItem}>
                  <span style={styles.label}>Other Deductions:</span>
                  <span style={styles.amount}>{formatCurrency(approval.totalDeductions - approval.providentFund - approval.tax)}</span>
                </div>
                <div style={styles.totalRow}>
                  <span style={styles.totalLabel}>Total Deductions:</span>
                  <span style={styles.totalAmount}>{formatCurrency(approval.totalDeductions)}</span>
                </div>
              </div>
            </div>

            {/* Net Salary */}
            <div style={styles.netSalaryRow}>
              <DollarSign size={24} color="#10b981" />
              <span style={styles.netSalaryLabel}>Net Salary:</span>
              <span style={styles.netSalaryAmount}>{formatCurrency(approval.netSalary)}</span>
            </div>

            {/* Action Buttons */}
            <div style={styles.cardActions}>
              <button 
                onClick={() => {
                  setSelectedPayroll(approval);
                  setShowModal(true);
                }}
                style={styles.viewButton}
              >
                <Eye size={18} />
                <span>Detailed View</span>
              </button>
              <div style={styles.actionButtons}>
                <button 
                  onClick={() => handleReject(approval.id)}
                  style={styles.rejectButton}
                  disabled={processing}
                >
                  <XCircle size={18} />
                  <span>Reject</span>
                </button>
                <button 
                  onClick={() => handleApprove(approval.id)}
                  style={styles.approveButton}
                  disabled={processing}
                >
                  <CheckCircle size={18} />
                  <span>Approve</span>
                </button>
              </div>
            </div>
          </motion.div>
        ))}
      </div>

      {filteredApprovals.length === 0 && (
        <motion.div 
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          style={styles.emptyState}
        >
          <FileText size={64} color="#d1d5db" />
          <h3 style={styles.emptyTitle}>No Pending Approvals</h3>
          <p style={styles.emptyText}>All payrolls have been reviewed</p>
        </motion.div>
      )}

      {/* Detail Modal */}
      {showModal && selectedPayroll && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          style={styles.modalOverlay}
          onClick={() => setShowModal(false)}
        >
          <motion.div
            initial={{ scale: 0.95, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            style={styles.modalContent}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={styles.modalHeader}>
              <h2 style={styles.modalTitle}>Payroll Details</h2>
              <button 
                onClick={() => setShowModal(false)}
                style={styles.closeButton}
              >
                ✕
              </button>
            </div>

            <div style={styles.modalBody}>
              <div style={styles.infoRow}>
                <span style={styles.infoLabel}>Employee:</span>
                <span style={styles.infoValue}>{selectedPayroll.employeeName}</span>
              </div>
              <div style={styles.infoRow}>
                <span style={styles.infoLabel}>Period:</span>
                <span style={styles.infoValue}>{getMonthName(selectedPayroll.month)} {selectedPayroll.year}</span>
              </div>
              
              <div style={styles.breakdownSection}>
                <h3 style={styles.breakdownTitle}>Complete Salary Breakdown</h3>
                <div style={styles.breakdownGrid}>
                  <div>
                    <h4 style={styles.subsectionTitle}>Earnings</h4>
                    {[
                      { label: 'Basic Salary', value: selectedPayroll.basicSalary },
                      { label: 'HRA', value: selectedPayroll.hra },
                      { label: 'DA', value: selectedPayroll.da },
                      { label: 'TA', value: selectedPayroll.ta },
                    ].map((item, idx) => (
                      <div key={idx} style={styles.breakdownRow}>
                        <span style={styles.breakdownLabel}>{item.label}:</span>
                        <span style={styles.breakdownValue}>{formatCurrency(item.value)}</span>
                      </div>
                    ))}
                    <div style={styles.breakdownTotal}>
                      <span>Gross Salary:</span>
                      <span>{formatCurrency(selectedPayroll.grossSalary)}</span>
                    </div>
                  </div>
                  
                  <div>
                    <h4 style={styles.subsectionTitle}>Deductions</h4>
                    {[
                      { label: 'Provident Fund', value: selectedPayroll.providentFund },
                      { label: 'Tax', value: selectedPayroll.tax },
                      { label: 'Insurance', value: 0 },
                      { label: 'Other Deductions', value: selectedPayroll.totalDeductions - selectedPayroll.providentFund - selectedPayroll.tax },
                    ].map((item, idx) => (
                      <div key={idx} style={styles.breakdownRow}>
                        <span style={styles.breakdownLabel}>{item.label}:</span>
                        <span style={styles.breakdownValue}>{formatCurrency(item.value)}</span>
                      </div>
                    ))}
                    <div style={styles.breakdownTotal}>
                      <span>Total Deductions:</span>
                      <span>{formatCurrency(selectedPayroll.totalDeductions)}</span>
                    </div>
                  </div>
                </div>
              </div>

              <div style={styles.netSalaryDisplay}>
                <span style={styles.netSalaryTextLabel}>Net Salary:</span>
                <span style={styles.netSalaryDisplayValue}>{formatCurrency(selectedPayroll.netSalary)}</span>
              </div>

              <div style={styles.commentsSection}>
                <label style={styles.commentsLabel}>
                  <MessageSquare size={18} />
                  <span>Add Comments (Optional):</span>
                </label>
                <textarea
                  value={comments}
                  onChange={(e) => setComments(e.target.value)}
                  placeholder="Enter any comments or notes..."
                  rows={4}
                  style={styles.commentsTextarea}
                />
              </div>
            </div>

            <div style={styles.modalFooter}>
              <button
                onClick={() => handleReject(selectedPayroll.id)}
                style={styles.modalRejectButton}
                disabled={processing}
              >
                <XCircle size={18} />
                <span>Reject</span>
              </button>
              <button
                onClick={() => handleApprove(selectedPayroll.id)}
                style={styles.modalApproveButton}
                disabled={processing}
              >
                <CheckCircle size={18} />
                <span>Approve</span>
              </button>
            </div>
          </motion.div>
        </motion.div>
      )}
    </div>
  );
};

const styles = {
  container: {
    maxWidth: '1400px',
    margin: '0 auto',
    padding: '2rem',
    background: 'linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%)',
    minHeight: '100vh',
  },
  loadingContainer: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    height: '100vh',
    background: '#f9fafb',
  },
  spinner: {
    width: '50px',
    height: '50px',
    border: '4px solid #e5e7eb',
    borderTop: '4px solid #4f46e5',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
    marginBottom: '1rem',
  },
  loadingText: {
    color: '#6b7280',
    fontSize: '1rem',
  },
  header: {
    background: 'linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%)',
    borderRadius: '16px',
    padding: '2rem',
    marginBottom: '2rem',
    boxShadow: '0 4px 6px -1px rgba(79, 70, 229, 0.1), 0 2px 4px -1px rgba(79, 70, 229, 0.06)',
  },
  headerContent: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  title: {
    color: '#ffffff',
    fontSize: '2rem',
    fontWeight: '700',
    marginBottom: '0.5rem',
  },
  subtitle: {
    color: '#e0e7ff',
    fontSize: '1rem',
  },
  statsContainer: {
    display: 'flex',
    gap: '1rem',
  },
  statCard: {
    background: 'rgba(255, 255, 255, 0.2)',
    backdropFilter: 'blur(10px)',
    borderRadius: '12px',
    padding: '1rem',
    display: 'flex',
    alignItems: 'center',
    gap: '0.75rem',
    minWidth: '150px',
  },
  statValue: {
    color: '#ffffff',
    fontSize: '1.75rem',
    fontWeight: '700',
  },
  statLabel: {
    color: '#e0e7ff',
    fontSize: '0.875rem',
  },
  toolbar: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '2rem',
    background: '#ffffff',
    padding: '1rem',
    borderRadius: '12px',
    boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.1)',
  },
  searchBox: {
    display: 'flex',
    alignItems: 'center',
    background: '#f9fafb',
    borderRadius: '8px',
    padding: '0.5rem 1rem',
    width: '400px',
    gap: '0.5rem',
  },
  searchInput: {
    border: 'none',
    outline: 'none',
    flex: 1,
    fontSize: '0.875rem',
    background: 'transparent',
  },
  filterButtons: {
    display: 'flex',
    gap: '0.75rem',
  },
  filterButton: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
    padding: '0.5rem 1rem',
    background: '#f9fafb',
    border: '1px solid #e5e7eb',
    borderRadius: '8px',
    color: '#374151',
    fontSize: '0.875rem',
    cursor: 'pointer',
    transition: 'all 0.2s',
  },
  exportButton: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
    padding: '0.5rem 1rem',
    background: '#4f46e5',
    border: 'none',
    borderRadius: '8px',
    color: '#ffffff',
    fontSize: '0.875rem',
    cursor: 'pointer',
    transition: 'all 0.2s',
  },
  errorBox: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.75rem',
    background: '#fef2f2',
    border: '1px solid #fecaca',
    borderRadius: '12px',
    padding: '1rem',
    marginBottom: '2rem',
  },
  errorText: {
    color: '#dc2626',
    fontSize: '0.875rem',
    fontWeight: '500',
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(450px, 1fr))',
    gap: '1.5rem',
  },
  card: {
    background: '#ffffff',
    borderRadius: '16px',
    padding: '1.5rem',
    boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)',
    transition: 'all 0.3s',
    cursor: 'pointer',
  },
  cardHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '1.5rem',
    paddingBottom: '1rem',
    borderBottom: '2px solid #f3f4f6',
  },
  employeeInfo: {
    display: 'flex',
    alignItems: 'center',
    gap: '1rem',
  },
  avatar: {
    width: '50px',
    height: '50px',
    borderRadius: '12px',
    background: '#e0e7ff',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  employeeName: {
    color: '#111827',
    fontSize: '1.125rem',
    fontWeight: '600',
    marginBottom: '0.25rem',
  },
  employeeId: {
    color: '#6b7280',
    fontSize: '0.875rem',
  },
  periodBadge: {
    background: 'linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%)',
    color: '#ffffff',
    padding: '0.5rem 1rem',
    borderRadius: '20px',
    fontSize: '0.875rem',
    fontWeight: '600',
  },
  salaryGrid: {
    display: 'grid',
    gridTemplateColumns: '1fr 1fr',
    gap: '1.5rem',
    marginBottom: '1.5rem',
  },
  salarySection: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.75rem',
  },
  sectionTitle: {
    color: '#374151',
    fontSize: '0.875rem',
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: '0.05em',
    marginBottom: '0.5rem',
  },
  salaryItem: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  label: {
    color: '#6b7280',
    fontSize: '0.875rem',
  },
  amount: {
    color: '#111827',
    fontSize: '0.875rem',
    fontWeight: '600',
  },
  totalRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingTop: '0.75rem',
    marginTop: '0.5rem',
    borderTop: '2px solid #e5e7eb',
  },
  totalLabel: {
    color: '#374151',
    fontSize: '0.875rem',
    fontWeight: '600',
  },
  totalAmount: {
    color: '#4f46e5',
    fontSize: '1rem',
    fontWeight: '700',
  },
  netSalaryRow: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.75rem',
    background: 'linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%)',
    padding: '1rem',
    borderRadius: '12px',
    marginBottom: '1.5rem',
  },
  netSalaryLabel: {
    color: '#065f46',
    fontSize: '0.875rem',
    fontWeight: '600',
  },
  netSalaryAmount: {
    color: '#10b981',
    fontSize: '1.5rem',
    fontWeight: '700',
    marginLeft: 'auto',
  },
  cardActions: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: '1rem',
  },
  viewButton: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
    padding: '0.5rem 1rem',
    background: '#f9fafb',
    border: '1px solid #e5e7eb',
    borderRadius: '8px',
    color: '#374151',
    fontSize: '0.875rem',
    cursor: 'pointer',
    transition: 'all 0.2s',
  },
  actionButtons: {
    display: 'flex',
    gap: '0.75rem',
  },
  rejectButton: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
    padding: '0.5rem 1rem',
    background: '#fef2f2',
    border: '1px solid #fecaca',
    borderRadius: '8px',
    color: '#dc2626',
    fontSize: '0.875rem',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'all 0.2s',
  },
  approveButton: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
    padding: '0.5rem 1rem',
    background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
    border: 'none',
    borderRadius: '8px',
    color: '#ffffff',
    fontSize: '0.875rem',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'all 0.2s',
    boxShadow: '0 2px 4px rgba(16, 185, 129, 0.2)',
  },
  emptyState: {
    textAlign: 'center',
    padding: '4rem 2rem',
    background: '#ffffff',
    borderRadius: '16px',
    boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
  },
  emptyTitle: {
    color: '#374151',
    fontSize: '1.25rem',
    fontWeight: '600',
    marginTop: '1rem',
  },
  emptyText: {
    color: '#6b7280',
    fontSize: '0.875rem',
    marginTop: '0.5rem',
  },
  modalOverlay: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    background: 'rgba(0, 0, 0, 0.5)',
    backdropFilter: 'blur(4px)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1000,
    padding: '2rem',
  },
  modalContent: {
    background: '#ffffff',
    borderRadius: '16px',
    maxWidth: '800px',
    width: '100%',
    maxHeight: '90vh',
    overflow: 'auto',
    boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1)',
  },
  modalHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '1.5rem 2rem',
    borderBottom: '2px solid #f3f4f6',
  },
  modalTitle: {
    color: '#111827',
    fontSize: '1.5rem',
    fontWeight: '700',
  },
  closeButton: {
    background: '#f9fafb',
    border: 'none',
    borderRadius: '8px',
    width: '36px',
    height: '36px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    cursor: 'pointer',
    fontSize: '1.25rem',
    color: '#6b7280',
    transition: 'all 0.2s',
  },
  modalBody: {
    padding: '2rem',
  },
  infoRow: {
    display: 'flex',
    justifyContent: 'space-between',
    padding: '0.75rem 0',
    borderBottom: '1px solid #f3f4f6',
  },
  infoLabel: {
    color: '#6b7280',
    fontSize: '0.875rem',
    fontWeight: '500',
  },
  infoValue: {
    color: '#111827',
    fontSize: '0.875rem',
    fontWeight: '600',
  },
  breakdownSection: {
    marginTop: '2rem',
  },
  breakdownTitle: {
    color: '#374151',
    fontSize: '1rem',
    fontWeight: '600',
    marginBottom: '1rem',
  },
  breakdownGrid: {
    display: 'grid',
    gridTemplateColumns: '1fr 1fr',
    gap: '2rem',
  },
  subsectionTitle: {
    color: '#4f46e5',
    fontSize: '0.875rem',
    fontWeight: '600',
    marginBottom: '1rem',
  },
  breakdownRow: {
    display: 'flex',
    justifyContent: 'space-between',
    padding: '0.5rem 0',
  },
  breakdownLabel: {
    color: '#6b7280',
    fontSize: '0.875rem',
  },
  breakdownValue: {
    color: '#111827',
    fontSize: '0.875rem',
    fontWeight: '600',
  },
  breakdownTotal: {
    display: 'flex',
    justifyContent: 'space-between',
    padding: '0.75rem 0',
    marginTop: '0.5rem',
    borderTop: '2px solid #e5e7eb',
    fontSize: '0.875rem',
    fontWeight: '700',
    color: '#4f46e5',
  },
  netSalaryDisplay: {
    display: 'flex',
    alignItems: 'center',
    gap: '1rem',
    background: 'linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%)',
    padding: '1.5rem',
    borderRadius: '12px',
    marginTop: '2rem',
  },
  netSalaryTextLabel: {
    color: '#065f46',
    fontSize: '1rem',
    fontWeight: '600',
  },
  netSalaryDisplayValue: {
    color: '#10b981',
    fontSize: '2rem',
    fontWeight: '700',
    marginLeft: 'auto',
  },
  commentsSection: {
    marginTop: '2rem',
  },
  commentsLabel: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
    color: '#374151',
    fontSize: '0.875rem',
    fontWeight: '600',
    marginBottom: '0.75rem',
  },
  commentsTextarea: {
    width: '100%',
    padding: '1rem',
    border: '1px solid #e5e7eb',
    borderRadius: '8px',
    fontSize: '0.875rem',
    resize: 'vertical',
    fontFamily: 'inherit',
    transition: 'all 0.2s',
  },
  modalFooter: {
    display: 'flex',
    justifyContent: 'flex-end',
    gap: '1rem',
    padding: '1.5rem 2rem',
    borderTop: '2px solid #f3f4f6',
  },
  modalRejectButton: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
    padding: '0.75rem 1.5rem',
    background: '#fef2f2',
    border: '1px solid #fecaca',
    borderRadius: '8px',
    color: '#dc2626',
    fontSize: '0.875rem',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'all 0.2s',
  },
  modalApproveButton: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
    padding: '0.75rem 1.5rem',
    background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
    border: 'none',
    borderRadius: '8px',
    color: '#ffffff',
    fontSize: '0.875rem',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'all 0.2s',
    boxShadow: '0 2px 4px rgba(16, 185, 129, 0.2)',
  },
};

export default AccountantApprovals;
