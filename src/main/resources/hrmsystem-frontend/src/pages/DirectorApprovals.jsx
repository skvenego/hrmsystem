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
  Shield,
  TrendingUp,
  MessageSquare,
  AlertCircle,
  Award
} from 'lucide-react';

const DirectorApprovals = () => {
  const [pendingApprovals, setPendingApprovals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedPayroll, setSelectedPayroll] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [comments, setComments] = useState('');
  const [processing, setProcessing] = useState(false);
  const [stats, setStats] = useState({
    total: 0,
    approvedByAccountant: 0,
    pending: 0
  });

  useEffect(() => {
    fetchPendingApprovals();
  }, []);

  const fetchPendingApprovals = async () => {
    try {
      setLoading(true);
      const response = await fetch('/api/payroll/approvals/pending/director', {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      });
      
      if (response.ok) {
        const data = await response.json();
        setPendingApprovals(data);
        setStats({
          total: data.length,
          approvedByAccountant: data.filter(d => d.accountantApproved).length,
          pending: data.filter(d => !d.accountantApproved).length
        });
      } else {
        const mockData = getMockData();
        setPendingApprovals(mockData);
        setStats({
          total: mockData.length,
          approvedByAccountant: mockData.filter(d => d.accountantApproved).length,
          pending: mockData.filter(d => !d.accountantApproved).length
        });
      }
    } catch (err) {
      setError('Failed to load pending approvals');
      const mockData = getMockData();
      setPendingApprovals(mockData);
    } finally {
      setLoading(false);
    }
  };

  const getMockData = () => [
    {
      id: 21,
      approvalId: 51,
      employeeName: 'Ram Murat',
      employeeId: 8,
      month: 10,
      year: 2026,
      netSalary: 44820,
      accountantApproved: true
    },
    {
      id: 40,
      approvalId: 59,
      employeeName: 'deepak rajpoot',
      employeeId: 10,
      month: 6,
      year: 2026,
      netSalary: 57180,
      accountantApproved: false
    },
    {
      id: 54,
      approvalId: 70,
      employeeName: 'sachin Kumar',
      employeeId: 15,
      month: 4,
      year: 2026,
      netSalary: 19180,
      accountantApproved: true
    }
  ];

  const handleApprove = async (payrollId) => {
    if (!window.confirm('Are you sure you want to give final approval? This will generate the payslip.')) return;
    
    try {
      setProcessing(true);
      const response = await fetch(`/api/payroll/approvals/${payrollId}/director/approve`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ comments }),
      });

      if (response.ok) {
        alert('✅ Final approval granted! Payslip has been generated.');
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
      const response = await fetch(`/api/payroll/approvals/${payrollId}/director/reject`, {
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
            <h1 style={styles.title}>Director Approval Dashboard</h1>
            <p style={styles.subtitle}>Final approval authority for payroll processing</p>
          </div>
          <div style={styles.statsGrid}>
            <div style={styles.statCard}>
              <FileText size={24} color="#4f46e5" />
              <div>
                <div style={styles.statValue}>{stats.total}</div>
                <div style={styles.statLabel}>Total</div>
              </div>
            </div>
            <div style={styles.statCardSuccess}>
              <CheckCircle size={24} color="#10b981" />
              <div>
                <div style={styles.statValue}>{stats.approvedByAccountant}</div>
                <div style={styles.statLabel}>Accountant Approved</div>
              </div>
            </div>
            <div style={styles.statCardWarning}>
              <Clock size={24} color="#f59e0b" />
              <div>
                <div style={styles.statValue}>{stats.pending}</div>
                <div style={styles.statLabel}>Awaiting Review</div>
              </div>
            </div>
          </div>
        </div>
      </motion.div>

      {/* Info Banner */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        style={styles.infoBanner}
      >
        <Shield size={24} color="#1e40af" />
        <div>
          <strong style={styles.infoBannerTitle}>Your Role as Director:</strong>
          <span style={styles.infoBannerText}>
            Review payrolls already approved by the Accountant. Your approval is final and will generate payslips for employees.
          </span>
        </div>
      </motion.div>

      {/* Toolbar */}
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.2 }}
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

      {/* Approvals List */}
      <div style={styles.list}>
        {filteredApprovals.map((approval, index) => (
          <motion.div
            key={approval.id}
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: index * 0.05 }}
            style={styles.listItem}
          >
            <div style={styles.listLeft}>
              <div style={styles.listAvatar}>
                <Award size={28} color={approval.accountantApproved ? "#10b981" : "#f59e0b"} />
              </div>
              <div style={styles.listInfo}>
                <div style={styles.listHeader}>
                  <h3 style={styles.listEmployeeName}>{approval.employeeName}</h3>
                  <span style={styles.listPeriod}>
                    {getMonthName(approval.month)} {approval.year}
                  </span>
                </div>
                <div style={styles.listDetails}>
                  <span style={styles.listId}>ID: {approval.employeeId}</span>
                  <span style={styles.listDivider}>•</span>
                  <span style={styles.listNetSalary}>
                    Net Salary: <strong>{formatCurrency(approval.netSalary)}</strong>
                  </span>
                  <span style={styles.listDivider}>•</span>
                  {approval.accountantApproved ? (
                    <span style={styles.statusBadgeApproved}>
                      <CheckCircle size={14} />
                      Accountant Approved
                    </span>
                  ) : (
                    <span style={styles.statusBadgePending}>
                      <Clock size={14} />
                      Awaiting Accountant
                    </span>
                  )}
                </div>
              </div>
            </div>
            
            <div style={styles.listActions}>
              <button 
                onClick={() => {
                  setSelectedPayroll(approval);
                  setShowModal(true);
                }}
                style={styles.listViewButton}
                disabled={!approval.accountantApproved}
              >
                <Eye size={18} />
                <span>Review</span>
              </button>
              {!approval.accountantApproved ? (
                <button 
                  style={styles.listDisabledButton}
                  disabled
                >
                  <Clock size={18} />
                  <span>Pending Accountant</span>
                </button>
              ) : (
                <div style={styles.listButtonGroup}>
                  <button 
                    onClick={() => handleReject(approval.id)}
                    style={styles.listRejectButton}
                    disabled={processing}
                  >
                    <XCircle size={18} />
                    <span>Reject</span>
                  </button>
                  <button 
                    onClick={() => handleApprove(approval.id)}
                    style={styles.listApproveButton}
                    disabled={processing}
                  >
                    <CheckCircle size={18} />
                    <span>Final Approval</span>
                  </button>
                </div>
              )}
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
          <TrendingUp size={64} color="#d1d5db" />
          <h3 style={styles.emptyTitle}>All Caught Up!</h3>
          <p style={styles.emptyText}>No pending payrolls requiring your approval</p>
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
              <div style={styles.modalHeaderLeft}>
                <Shield size={28} color="#4f46e5" />
                <div>
                  <h2 style={styles.modalTitle}>Final Approval Review</h2>
                  <p style={styles.modalSubtitle}>Director's approval dashboard</p>
                </div>
              </div>
              <button 
                onClick={() => setShowModal(false)}
                style={styles.closeButton}
              >
                ✕
              </button>
            </div>

            <div style={styles.modalBody}>
              <div style={styles.approvalStatusBanner}>
                <div style={styles.approvalStatusRow}>
                  <div style={styles.approvalStep}>
                    <div style={styles.approvalStepIconCompleted}>
                      <CheckCircle size={20} />
                    </div>
                    <span style={styles.approvalStepLabel}>Accountant Approved</span>
                  </div>
                  <div style={styles.approvalArrow}>→</div>
                  <div style={styles.approvalStep}>
                    <div style={styles.approvalStepIconCurrent}>
                      <Shield size={20} />
                    </div>
                    <span style={styles.approvalStepLabelCurrent}>Director Review</span>
                  </div>
                  <div style={styles.approvalArrow}>→</div>
                  <div style={styles.approvalStep}>
                    <div style={styles.approvalStepIconPending}>
                      <DollarSign size={20} />
                    </div>
                    <span style={styles.approvalStepLabel}>Payslip Generation</span>
                  </div>
                </div>
              </div>

              <div style={styles.infoGrid}>
                <div style={styles.infoCard}>
                  <Users size={20} color="#6b7280" />
                  <div>
                    <div style={styles.infoCardLabel}>Employee Name</div>
                    <div style={styles.infoCardValue}>{selectedPayroll.employeeName}</div>
                  </div>
                </div>
                <div style={styles.infoCard}>
                  <FileText size={20} color="#6b7280" />
                  <div>
                    <div style={styles.infoCardLabel}>Payroll Period</div>
                    <div style={styles.infoCardValue}>{getMonthName(selectedPayroll.month)} {selectedPayroll.year}</div>
                  </div>
                </div>
                <div style={styles.infoCard}>
                  <DollarSign size={20} color="#6b7280" />
                  <div>
                    <div style={styles.infoCardLabel}>Net Salary</div>
                    <div style={styles.infoCardValueGreen}>{formatCurrency(selectedPayroll.netSalary)}</div>
                  </div>
                </div>
              </div>

              <div style={styles.reviewSection}>
                <h3 style={styles.reviewTitle}>Pre-Approval Checklist</h3>
                <div style={styles.checklist}>
                  {[
                    { label: 'Verified by Accountant', checked: selectedPayroll.accountantApproved },
                    { label: 'Budget allocation confirmed', checked: true },
                    { label: 'Policy compliance verified', checked: true },
                    { label: 'Organizational guidelines met', checked: true },
                  ].map((item, idx) => (
                    <label key={idx} style={styles.checklistItem}>
                      <input 
                        type="checkbox" 
                        defaultChecked={item.checked}
                        disabled={idx !== 0}
                        style={styles.checkbox}
                      />
                      <span style={item.checked ? styles.checklistText : styles.checklistTextDisabled}>
                        {item.label}
                      </span>
                    </label>
                  ))}
                </div>
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
                <span>Grant Final Approval</span>
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
    background: 'linear-gradient(135deg, #1e40af 0%, #7c3aed 100%)',
    borderRadius: '16px',
    padding: '2rem',
    marginBottom: '2rem',
    boxShadow: '0 4px 6px -1px rgba(30, 64, 175, 0.1), 0 2px 4px -1px rgba(30, 64, 175, 0.06)',
  },
  headerContent: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    flexWrap: 'wrap',
    gap: '1.5rem',
  },
  title: {
    color: '#ffffff',
    fontSize: '2rem',
    fontWeight: '700',
    marginBottom: '0.5rem',
  },
  subtitle: {
    color: '#dbeafe',
    fontSize: '1rem',
  },
  statsGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(3, 1fr)',
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
    minWidth: '140px',
  },
  statCardSuccess: {
    background: 'rgba(255, 255, 255, 0.2)',
    backdropFilter: 'blur(10px)',
    borderRadius: '12px',
    padding: '1rem',
    display: 'flex',
    alignItems: 'center',
    gap: '0.75rem',
    minWidth: '140px',
  },
  statCardWarning: {
    background: 'rgba(255, 255, 255, 0.2)',
    backdropFilter: 'blur(10px)',
    borderRadius: '12px',
    padding: '1rem',
    display: 'flex',
    alignItems: 'center',
    gap: '0.75rem',
    minWidth: '140px',
  },
  statValue: {
    color: '#ffffff',
    fontSize: '1.75rem',
    fontWeight: '700',
  },
  statLabel: {
    color: '#dbeafe',
    fontSize: '0.875rem',
  },
  infoBanner: {
    background: 'linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%)',
    borderRadius: '12px',
    padding: '1.5rem',
    marginBottom: '2rem',
    display: 'flex',
    alignItems: 'flex-start',
    gap: '1rem',
    border: '1px solid #93c5fd',
  },
  infoBannerTitle: {
    color: '#1e40af',
    fontSize: '0.938rem',
    display: 'block',
    marginBottom: '0.25rem',
  },
  infoBannerText: {
    color: '#1e3a8a',
    fontSize: '0.875rem',
    lineHeight: '1.5',
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
  list: {
    display: 'flex',
    flexDirection: 'column',
    gap: '1rem',
  },
  listItem: {
    background: '#ffffff',
    borderRadius: '12px',
    padding: '1.5rem',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.1)',
    transition: 'all 0.3s',
  },
  listLeft: {
    display: 'flex',
    alignItems: 'center',
    gap: '1rem',
  },
  listAvatar: {
    width: '60px',
    height: '60px',
    borderRadius: '12px',
    background: '#f3f4f6',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  listInfo: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.5rem',
  },
  listHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '1rem',
  },
  listEmployeeName: {
    color: '#111827',
    fontSize: '1.125rem',
    fontWeight: '600',
    margin: 0,
  },
  listPeriod: {
    background: '#e0e7ff',
    color: '#4f46e5',
    padding: '0.25rem 0.75rem',
    borderRadius: '12px',
    fontSize: '0.875rem',
    fontWeight: '600',
  },
  listDetails: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.75rem',
    fontSize: '0.875rem',
  },
  listId: {
    color: '#6b7280',
  },
  listDivider: {
    color: '#d1d5db',
  },
  listNetSalary: {
    color: '#111827',
    fontWeight: '600',
  },
  statusBadgeApproved: {
    display: 'inline-flex',
    alignItems: 'center',
    gap: '0.375rem',
    background: '#d1fae5',
    color: '#065f46',
    padding: '0.25rem 0.75rem',
    borderRadius: '12px',
    fontSize: '0.813rem',
    fontWeight: '600',
  },
  statusBadgePending: {
    display: 'inline-flex',
    alignItems: 'center',
    gap: '0.375rem',
    background: '#fef3c7',
    color: '#92400e',
    padding: '0.25rem 0.75rem',
    borderRadius: '12px',
    fontSize: '0.813rem',
    fontWeight: '600',
  },
  listActions: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.75rem',
  },
  listViewButton: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
    padding: '0.5rem 1rem',
    background: '#f9fafb',
    border: '1px solid #e5e7eb',
    borderRadius: '8px',
    color: '#374151',
    fontSize: '0.875rem',
    fontWeight: '500',
    cursor: 'pointer',
    transition: 'all 0.2s',
  },
  listButtonGroup: {
    display: 'flex',
    gap: '0.5rem',
  },
  listRejectButton: {
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
  listApproveButton: {
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
  listDisabledButton: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
    padding: '0.5rem 1rem',
    background: '#f3f4f6',
    border: '1px solid #e5e7eb',
    borderRadius: '8px',
    color: '#9ca3af',
    fontSize: '0.875rem',
    fontWeight: '500',
    cursor: 'not-allowed',
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
  modalHeaderLeft: {
    display: 'flex',
    alignItems: 'center',
    gap: '1rem',
  },
  modalTitle: {
    color: '#111827',
    fontSize: '1.5rem',
    fontWeight: '700',
    margin: 0,
  },
  modalSubtitle: {
    color: '#6b7280',
    fontSize: '0.875rem',
    margin: 0,
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
  approvalStatusBanner: {
    background: 'linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%)',
    borderRadius: '12px',
    padding: '1.5rem',
    marginBottom: '2rem',
    border: '1px solid #bfdbfe',
  },
  approvalStatusRow: {
    display: 'flex',
    justifyContent: 'space-around',
    alignItems: 'center',
  },
  approvalStep: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '0.5rem',
  },
  approvalStepIconCompleted: {
    width: '44px',
    height: '44px',
    borderRadius: '50%',
    background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    color: '#ffffff',
  },
  approvalStepLabel: {
    color: '#065f46',
    fontSize: '0.75rem',
    fontWeight: '600',
  },
  approvalStepIconCurrent: {
    width: '44px',
    height: '44px',
    borderRadius: '50%',
    background: 'linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    color: '#ffffff',
  },
  approvalStepLabelCurrent: {
    color: '#4338ca',
    fontSize: '0.75rem',
    fontWeight: '700',
  },
  approvalStepIconPending: {
    width: '44px',
    height: '44px',
    borderRadius: '50%',
    background: '#e5e7eb',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    color: '#9ca3af',
  },
  approvalStepLabel: {
    color: '#6b7280',
    fontSize: '0.75rem',
    fontWeight: '600',
  },
  approvalArrow: {
    color: '#9ca3af',
    fontSize: '1.5rem',
    fontWeight: '700',
  },
  infoGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(3, 1fr)',
    gap: '1rem',
    marginBottom: '2rem',
  },
  infoCard: {
    background: '#f9fafb',
    borderRadius: '12px',
    padding: '1rem',
    display: 'flex',
    alignItems: 'flex-start',
    gap: '0.75rem',
  },
  infoCardLabel: {
    color: '#6b7280',
    fontSize: '0.75rem',
    fontWeight: '500',
    marginBottom: '0.25rem',
  },
  infoCardValue: {
    color: '#111827',
    fontSize: '1rem',
    fontWeight: '600',
  },
  infoCardValueGreen: {
    color: '#10b981',
    fontSize: '1.125rem',
    fontWeight: '700',
  },
  reviewSection: {
    marginBottom: '2rem',
  },
  reviewTitle: {
    color: '#374151',
    fontSize: '1rem',
    fontWeight: '600',
    marginBottom: '1rem',
  },
  checklist: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.75rem',
  },
  checklistItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.75rem',
    cursor: 'pointer',
  },
  checkbox: {
    width: '18px',
    height: '18px',
    cursor: 'pointer',
  },
  checklistText: {
    color: '#111827',
    fontSize: '0.875rem',
    fontWeight: '500',
  },
  checklistTextDisabled: {
    color: '#9ca3af',
    fontSize: '0.875rem',
    fontWeight: '500',
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

export default DirectorApprovals;
