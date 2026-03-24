import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  Search, 
  Plus, 
  Filter, 
  MoreVertical,
  Mail,
  Phone,
  Edit,
  Trash2,
  Eye,
  X
} from 'lucide-react';
import Sidebar from '../components/Sidebar';

const Employees = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [filterDepartment, setFilterDepartment] = useState('all');
  const [showModal, setShowModal] = useState(false);
  const [showViewModal, setShowViewModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedEmployee, setSelectedEmployee] = useState(null);
  const [newEmployee, setNewEmployee] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    designation: '',
    salary: '',
    joiningDate: '',
    departmentName: ''
  });

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(amount);
  };

  const getInitials = (firstName, lastName) => {
    return (firstName?.charAt(0) || '') + (lastName?.charAt(0) || '');
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('en-IN');
  };

  const getStatusBadge = (status) => {
    const statusColors = {
      'ACTIVE': 'success',
      'ON_LEAVE': 'warning',
      'INACTIVE': 'danger',
      'TERMINATED': 'danger'
    };
    return statusColors[status] || 'secondary';
  };

  const formatStatus = (status) => {
    if (!status) return 'Active';
    return status.replace('_', ' ').replace(/\b\w/g, l => l.toUpperCase());
  };

  // Fetch employees from API
  const fetchEmployees = async () => {
    try {
      const token = localStorage.getItem('token');
      // Add cache-busting parameter to ensure fresh data
      const response = await fetch(`/api/employees?_t=${Date.now()}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Cache-Control': 'no-cache'
        }
      });
      if (response.ok) {
        const data = await response.json();
        setEmployees(data);
      } else {
        console.error('Failed to fetch employees');
      }
    } catch (error) {
      console.error('Error fetching employees:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEmployees();
    
    // Refresh data when window gains focus (e.g., after navigating back from departments)
    const handleFocus = () => {
      fetchEmployees();
    };
    
    window.addEventListener('focus', handleFocus);
    
    return () => {
      window.removeEventListener('focus', handleFocus);
    };
  }, []);

  // View employee
  const handleView = (employee) => {
    setSelectedEmployee(employee);
    setShowViewModal(true);
  };

  // Edit employee
  const handleEdit = (employee) => {
    setSelectedEmployee(employee);
    setNewEmployee({
      firstName: employee.firstName || '',
      lastName: employee.lastName || '',
      email: employee.email || '',
      phone: employee.phone || '',
      designation: employee.designation || '',
      salary: employee.salary || '',
      joiningDate: employee.joiningDate || '',
      departmentName: employee.departmentName || ''
    });
    setShowEditModal(true);
  };

  // Update employee
  const handleUpdate = async (e) => {
    e.preventDefault();
    try {
      const token = localStorage.getItem('token');
      const payload = {
        ...newEmployee,
        salary: newEmployee.salary ? parseFloat(newEmployee.salary) : null,
        joiningDate: newEmployee.joiningDate || null
      };
      
      const response = await fetch(`/api/employees/${selectedEmployee.id}`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
      });
      
      if (response.ok) {
        alert('Employee updated successfully!');
        setShowEditModal(false);
        setSelectedEmployee(null);
        fetchEmployees();
      } else {
        const error = await response.text();
        alert('Error: ' + error);
      }
    } catch (error) {
      alert('Error updating employee: ' + error.message);
    }
  };

  const departments = ['all', 'IT', 'HR', 'Sales', 'Design', 'Finance', 'Marketing', 'Product', 'Operations'];

  const filteredEmployees = employees.filter(employee => {
    const fullName = `${employee.firstName} ${employee.lastName}`.toLowerCase();
    const matchesSearch = fullName.includes(searchTerm.toLowerCase()) ||
                         employee.email?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         employee.designation?.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesDepartment = filterDepartment === 'all' || employee.departmentName === filterDepartment;
    return matchesSearch && matchesDepartment;
  });

  return (
    <div className="dashboard">
      <Sidebar />
      
      <main className="main-content">
        <div className="dashboard-header">
          <div>
            <h1 className="dashboard-title">Employees</h1>
            <p style={{ color: '#6b7280', marginTop: '0.25rem' }}>Manage your team members</p>
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
            <Plus size={20} />
            Add Employee
          </motion.button>
        </div>

        <motion.div 
          style={{
            background: 'white',
            borderRadius: '12px',
            padding: '1.5rem',
            marginBottom: '1.5rem',
            boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)'
          }}
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
        >
          <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
            <div style={{ position: 'relative', flex: 1, minWidth: '300px' }}>
              <Search 
                size={20} 
                style={{ 
                  position: 'absolute', 
                  left: '1rem', 
                  top: '50%', 
                  transform: 'translateY(-50%)',
                  color: '#9ca3af'
                }} 
              />
              <input
                type="text"
                placeholder="Search employees..."
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
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Filter size={20} color="#6b7280" />
              <select
                value={filterDepartment}
                onChange={(e) => setFilterDepartment(e.target.value)}
                style={{
                  padding: '0.75rem 1rem',
                  border: '1px solid #e5e7eb',
                  borderRadius: '8px',
                  fontSize: '0.875rem',
                  cursor: 'pointer'
                }}
              >
                {departments.map(dept => (
                  <option key={dept} value={dept}>
                    {dept === 'all' ? 'All Departments' : dept}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </motion.div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '2rem', color: '#6b7280' }}>
            Loading employees...
          </div>
        ) : filteredEmployees.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '2rem', color: '#6b7280' }}>
            No employees found. Click "Add Employee" to add your first employee.
          </div>
        ) : (
        <motion.div 
          className="table-container"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, delay: 0.1 }}
        >
          <table className="data-table">
            <thead>
              <tr>
                <th>Employee</th>
                <th>Contact</th>
                <th>Role</th>
                <th>Department</th>
                <th>Status</th>
                <th>Salary</th>
                <th>Join Date</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredEmployees.map((employee) => (
                <motion.tr 
                  key={employee.id}
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  transition={{ duration: 0.3 }}
                >
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                      <div style={{
                        width: '40px',
                        height: '40px',
                        borderRadius: '50%',
                        background: 'linear-gradient(135deg, #4f46e5, #06b6d4)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: 'white',
                        fontWeight: 600,
                        fontSize: '0.875rem'
                      }}>
                        {getInitials(employee.firstName, employee.lastName)}
                      </div>
                      <div>
                        <div style={{ fontWeight: 600, color: '#1f2937' }}>{employee.firstName} {employee.lastName}</div>
                        <div style={{ fontSize: '0.75rem', color: '#6b7280' }}>ID: EMP{String(employee.id).padStart(3, '0')}</div>
                      </div>
                    </div>
                  </td>
                  <td>
                    <div style={{ fontSize: '0.875rem' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', marginBottom: '0.25rem' }}>
                        <Mail size={14} color="#6b7280" />
                        {employee.email || '-'}
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                        <Phone size={14} color="#6b7280" />
                        {employee.phone || '-'}
                      </div>
                    </div>
                  </td>
                  <td>{employee.designation || '-'}</td>
                  <td>
                    <span style={{
                      padding: '0.25rem 0.75rem',
                      background: '#f3f4f6',
                      borderRadius: '9999px',
                      fontSize: '0.75rem',
                      fontWeight: 500
                    }}>
                      {employee.departmentName || '-'}
                    </span>
                  </td>
                  <td>
                    <span className={`badge badge-${getStatusBadge(employee.status)}`}>
                      {formatStatus(employee.status)}
                    </span>
                  </td>
                  <td style={{ fontWeight: 600, color: '#1f2937' }}>{employee.salary ? formatCurrency(employee.salary) : '-'}</td>
                  <td>{formatDate(employee.joiningDate)}</td>
                  <td>
                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                      <motion.button
                        whileHover={{ scale: 1.1 }}
                        whileTap={{ scale: 0.9 }}
                        onClick={() => handleView(employee)}
                        style={{
                          padding: '0.5rem',
                          background: '#f3f4f6',
                          border: 'none',
                          borderRadius: '6px',
                          cursor: 'pointer',
                          color: '#4f46e5'
                        }}
                        title="View"
                      >
                        <Eye size={16} />
                      </motion.button>
                      <motion.button
                        whileHover={{ scale: 1.1 }}
                        whileTap={{ scale: 0.9 }}
                        onClick={() => handleEdit(employee)}
                        style={{
                          padding: '0.5rem',
                          background: '#f3f4f6',
                          border: 'none',
                          borderRadius: '6px',
                          cursor: 'pointer',
                          color: '#f59e0b'
                        }}
                        title="Edit"
                      >
                        <Edit size={16} />
                      </motion.button>
                      <motion.button
                        whileHover={{ scale: 1.1 }}
                        whileTap={{ scale: 0.9 }}
                        onClick={() => handleDelete(employee)}
                        style={{
                          padding: '0.5rem',
                          background: '#f3f4f6',
                          border: 'none',
                          borderRadius: '6px',
                          cursor: 'pointer',
                          color: '#ef4444'
                        }}
                        title="Delete"
                      >
                        <Trash2 size={16} />
                      </motion.button>
                    </div>
                  </td>
                </motion.tr>
              ))}
            </tbody>
          </table>
        </motion.div>
        )}

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1.5rem' }}>
          <p style={{ color: '#6b7280', fontSize: '0.875rem' }}>
            Showing {filteredEmployees.length} of {employees.length} employees
          </p>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button style={{
              padding: '0.5rem 1rem',
              border: '1px solid #e5e7eb',
              borderRadius: '6px',
              background: 'white',
              cursor: 'pointer',
              fontSize: '0.875rem'
            }}>
              Previous
            </button>
            <button style={{
              padding: '0.5rem 1rem',
              border: '1px solid #e5e7eb',
              borderRadius: '6px',
              background: 'white',
              cursor: 'pointer',
              fontSize: '0.875rem'
            }}>
              Next
            </button>
          </div>
        </div>
      </main>

      {/* Add Employee Modal */}
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
            <h2 style={{ marginBottom: '1.5rem', fontSize: '1.5rem', fontWeight: 600 }}>Add New Employee</h2>
            <form onSubmit={async (e) => {
              e.preventDefault();
              try {
                const token = localStorage.getItem('token');
                
                // Convert salary to number and prepare the payload
                const payload = {
                  ...newEmployee,
                  salary: newEmployee.salary ? parseFloat(newEmployee.salary) : null,
                  joiningDate: newEmployee.joiningDate || null,
                  status: 'ACTIVE'
                };
                
                console.log('Sending payload:', payload);
                
                const response = await fetch('/api/employees', {
                  method: 'POST',
                  headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                  },
                  body: JSON.stringify(payload)
                });
                
                if (response.ok) {
                  alert('Employee added successfully!');
                  setShowModal(false);
                  setNewEmployee({
                    firstName: '',
                    lastName: '',
                    email: '',
                    phone: '',
                    designation: '',
                    salary: '',
                    joiningDate: '',
                    departmentName: ''
                  });
                  window.location.reload();
                } else {
                  const errorText = await response.text();
                  console.error('Error response:', errorText);
                  alert('Error: ' + errorText);
                }
              } catch (error) {
                console.error('Error adding employee:', error);
                alert('Error adding employee: ' + error.message);
              }
            }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>First Name *</label>
                  <input
                    type="text"
                    required
                    value={newEmployee.firstName}
                    onChange={(e) => setNewEmployee({...newEmployee, firstName: e.target.value})}
                    style={{
                      width: '100%',
                      padding: '0.75rem',
                      border: '1px solid #e5e7eb',
                      borderRadius: '8px',
                      fontSize: '0.875rem'
                    }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>Last Name *</label>
                  <input
                    type="text"
                    required
                    value={newEmployee.lastName}
                    onChange={(e) => setNewEmployee({...newEmployee, lastName: e.target.value})}
                    style={{
                      width: '100%',
                      padding: '0.75rem',
                      border: '1px solid #e5e7eb',
                      borderRadius: '8px',
                      fontSize: '0.875rem'
                    }}
                  />
                </div>
              </div>
              <div style={{ marginBottom: '1rem' }}>
                <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>Email *</label>
                <input
                  type="email"
                  required
                  value={newEmployee.email}
                  onChange={(e) => setNewEmployee({...newEmployee, email: e.target.value})}
                  style={{
                    width: '100%',
                    padding: '0.75rem',
                    border: '1px solid #e5e7eb',
                    borderRadius: '8px',
                    fontSize: '0.875rem'
                  }}
                />
              </div>
              <div style={{ marginBottom: '1rem' }}>
                <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>Phone</label>
                <input
                  type="tel"
                  value={newEmployee.phone}
                  onChange={(e) => setNewEmployee({...newEmployee, phone: e.target.value})}
                  style={{
                    width: '100%',
                    padding: '0.75rem',
                    border: '1px solid #e5e7eb',
                    borderRadius: '8px',
                    fontSize: '0.875rem'
                  }}
                />
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>Department *</label>
                  <select
                    required
                    value={newEmployee.departmentName}
                    onChange={(e) => setNewEmployee({...newEmployee, departmentName: e.target.value})}
                    style={{
                      width: '100%',
                      padding: '0.75rem',
                      border: '1px solid #e5e7eb',
                      borderRadius: '8px',
                      fontSize: '0.875rem'
                    }}
                  >
                    <option value="">Select Department</option>
                    <option value="IT">IT</option>
                    <option value="HR">HR</option>
                    <option value="Sales">Sales</option>
                    <option value="Design">Design</option>
                    <option value="Finance">Finance</option>
                    <option value="Marketing">Marketing</option>
                  </select>
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>Designation *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g., Software Engineer"
                    value={newEmployee.designation}
                    onChange={(e) => setNewEmployee({...newEmployee, designation: e.target.value})}
                    style={{
                      width: '100%',
                      padding: '0.75rem',
                      border: '1px solid #e5e7eb',
                      borderRadius: '8px',
                      fontSize: '0.875rem'
                    }}
                  />
                </div>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1.5rem' }}>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>Basic Salary (₹) *</label>
                  <input
                    type="number"
                    required
                    placeholder="e.g., 50000"
                    value={newEmployee.salary}
                    onChange={(e) => setNewEmployee({...newEmployee, salary: e.target.value})}
                    style={{
                      width: '100%',
                      padding: '0.75rem',
                      border: '1px solid #e5e7eb',
                      borderRadius: '8px',
                      fontSize: '0.875rem'
                    }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>Join Date *</label>
                  <input
                    type="date"
                    required
                    value={newEmployee.joiningDate}
                    onChange={(e) => setNewEmployee({...newEmployee, joiningDate: e.target.value})}
                    style={{
                      width: '100%',
                      padding: '0.75rem',
                      border: '1px solid #e5e7eb',
                      borderRadius: '8px',
                      fontSize: '0.875rem'
                    }}
                  />
                </div>
              </div>
              <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end' }}>
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  style={{
                    padding: '0.75rem 1.5rem',
                    border: '1px solid #e5e7eb',
                    borderRadius: '8px',
                    background: 'white',
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
                  Add Employee
                </button>
              </div>
            </form>
          </motion.div>
        </div>
      )}

      {/* View Employee Modal */}
      {showViewModal && selectedEmployee && (
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
              <h2 style={{ fontSize: '1.5rem', fontWeight: 600 }}>Employee Details</h2>
              <button onClick={() => { setShowViewModal(false); setSelectedEmployee(null); }} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1rem' }}>
                <div style={{
                  width: '60px',
                  height: '60px',
                  borderRadius: '50%',
                  background: 'linear-gradient(135deg, #4f46e5, #06b6d4)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'white',
                  fontWeight: 600,
                  fontSize: '1.25rem'
                }}>
                  {getInitials(selectedEmployee.firstName, selectedEmployee.lastName)}
                </div>
                <div>
                  <div style={{ fontWeight: 600, fontSize: '1.25rem' }}>{selectedEmployee.firstName} {selectedEmployee.lastName}</div>
                  <div style={{ color: '#6b7280' }}>{selectedEmployee.designation}</div>
                </div>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div>
                  <label style={{ fontSize: '0.75rem', color: '#6b7280' }}>Email</label>
                  <div style={{ fontWeight: 500 }}>{selectedEmployee.email || '-'}</div>
                </div>
                <div>
                  <label style={{ fontSize: '0.75rem', color: '#6b7280' }}>Phone</label>
                  <div style={{ fontWeight: 500 }}>{selectedEmployee.phone || '-'}</div>
                </div>
                <div>
                  <label style={{ fontSize: '0.75rem', color: '#6b7280' }}>Department</label>
                  <div style={{ fontWeight: 500 }}>{selectedEmployee.departmentName || '-'}</div>
                </div>
                <div>
                  <label style={{ fontSize: '0.75rem', color: '#6b7280' }}>Status</label>
                  <div><span className={`badge badge-${getStatusBadge(selectedEmployee.status)}`}>{formatStatus(selectedEmployee.status)}</span></div>
                </div>
                <div>
                  <label style={{ fontSize: '0.75rem', color: '#6b7280' }}>Salary</label>
                  <div style={{ fontWeight: 500 }}>{selectedEmployee.salary ? formatCurrency(selectedEmployee.salary) : '-'}</div>
                </div>
                <div>
                  <label style={{ fontSize: '0.75rem', color: '#6b7280' }}>Join Date</label>
                  <div style={{ fontWeight: 500 }}>{formatDate(selectedEmployee.joiningDate)}</div>
                </div>
              </div>
            </div>
            <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end', marginTop: '1.5rem' }}>
              <button
                onClick={() => { setShowViewModal(false); setSelectedEmployee(null); }}
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
                Close
              </button>
            </div>
          </motion.div>
        </div>
      )}

      {/* Edit Employee Modal */}
      {showEditModal && selectedEmployee && (
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
              <h2 style={{ fontSize: '1.5rem', fontWeight: 600 }}>Edit Employee</h2>
              <button onClick={() => { setShowEditModal(false); setSelectedEmployee(null); }} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleUpdate}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>First Name *</label>
                  <input
                    type="text"
                    required
                    value={newEmployee.firstName}
                    onChange={(e) => setNewEmployee({...newEmployee, firstName: e.target.value})}
                    style={{
                      width: '100%',
                      padding: '0.75rem',
                      border: '1px solid #e5e7eb',
                      borderRadius: '8px',
                      fontSize: '0.875rem'
                    }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>Last Name *</label>
                  <input
                    type="text"
                    required
                    value={newEmployee.lastName}
                    onChange={(e) => setNewEmployee({...newEmployee, lastName: e.target.value})}
                    style={{
                      width: '100%',
                      padding: '0.75rem',
                      border: '1px solid #e5e7eb',
                      borderRadius: '8px',
                      fontSize: '0.875rem'
                    }}
                  />
                </div>
              </div>
              <div style={{ marginBottom: '1rem' }}>
                <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>Email *</label>
                <input
                  type="email"
                  required
                  value={newEmployee.email}
                  onChange={(e) => setNewEmployee({...newEmployee, email: e.target.value})}
                  style={{
                    width: '100%',
                    padding: '0.75rem',
                    border: '1px solid #e5e7eb',
                    borderRadius: '8px',
                    fontSize: '0.875rem'
                  }}
                />
              </div>
              <div style={{ marginBottom: '1rem' }}>
                <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>Phone</label>
                <input
                  type="tel"
                  value={newEmployee.phone}
                  onChange={(e) => setNewEmployee({...newEmployee, phone: e.target.value})}
                  style={{
                    width: '100%',
                    padding: '0.75rem',
                    border: '1px solid #e5e7eb',
                    borderRadius: '8px',
                    fontSize: '0.875rem'
                  }}
                />
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>Department *</label>
                  <select
                    required
                    value={newEmployee.departmentName}
                    onChange={(e) => setNewEmployee({...newEmployee, departmentName: e.target.value})}
                    style={{
                      width: '100%',
                      padding: '0.75rem',
                      border: '1px solid #e5e7eb',
                      borderRadius: '8px',
                      fontSize: '0.875rem'
                    }}
                  >
                    <option value="">Select Department</option>
                    <option value="IT">IT</option>
                    <option value="HR">HR</option>
                    <option value="Sales">Sales</option>
                    <option value="Design">Design</option>
                    <option value="Finance">Finance</option>
                    <option value="Marketing">Marketing</option>
                    <option value="Product">Product</option>
                    <option value="Operations">Operations</option>
                  </select>
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>Designation *</label>
                  <input
                    type="text"
                    required
                    value={newEmployee.designation}
                    onChange={(e) => setNewEmployee({...newEmployee, designation: e.target.value})}
                    style={{
                      width: '100%',
                      padding: '0.75rem',
                      border: '1px solid #e5e7eb',
                      borderRadius: '8px',
                      fontSize: '0.875rem'
                    }}
                  />
                </div>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1.5rem' }}>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>Basic Salary (₹) *</label>
                  <input
                    type="number"
                    required
                    value={newEmployee.salary}
                    onChange={(e) => setNewEmployee({...newEmployee, salary: e.target.value})}
                    style={{
                      width: '100%',
                      padding: '0.75rem',
                      border: '1px solid #e5e7eb',
                      borderRadius: '8px',
                      fontSize: '0.875rem'
                    }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>Join Date *</label>
                  <input
                    type="date"
                    required
                    value={newEmployee.joiningDate}
                    onChange={(e) => setNewEmployee({...newEmployee, joiningDate: e.target.value})}
                    style={{
                      width: '100%',
                      padding: '0.75rem',
                      border: '1px solid #e5e7eb',
                      borderRadius: '8px',
                      fontSize: '0.875rem'
                    }}
                  />
                </div>
              </div>
              <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end' }}>
                <button
                  type="button"
                  onClick={() => { setShowEditModal(false); setSelectedEmployee(null); }}
                  style={{
                    padding: '0.75rem 1.5rem',
                    border: '1px solid #e5e7eb',
                    borderRadius: '8px',
                    background: 'white',
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
                  Update Employee
                </button>
              </div>
            </form>
          </motion.div>
        </div>
      )}
    </div>
  );
};

export default Employees;
