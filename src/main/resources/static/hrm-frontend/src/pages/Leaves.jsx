import { useState, useEffect } from 'react'
import { FaPlus, FaCheck, FaTimes, FaCalendarAlt } from 'react-icons/fa'
import axios from 'axios'
import { toast } from 'react-toastify'
import { format } from 'date-fns'

const Leaves = () => {
  const [leaves, setLeaves] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [showModal, setShowModal] = useState(false)
  const [employees, setEmployees] = useState([])
  const [formData, setFormData] = useState({
    employeeId: '',
    leaveType: 'CASUAL',
    startDate: '',
    endDate: '',
    reason: ''
  })

  useEffect(() => {
    fetchData()
  }, [])

  const fetchData = async () => {
    try {
      const [leavesRes, employeesRes] = await Promise.all([
        axios.get('/api/leaves'),
        axios.get('/api/employees')
      ])
      setLeaves(leavesRes.data)
      setEmployees(employeesRes.data)
    } catch (error) {
      toast.error('Failed to fetch data')
    } finally {
      setIsLoading(false)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      await axios.post('/api/leaves/apply', formData)
      toast.success('Leave application submitted')
      setShowModal(false)
      setFormData({ employeeId: '', leaveType: 'CASUAL', startDate: '', endDate: '', reason: '' })
      fetchData()
    } catch (error) {
      toast.error('Failed to submit leave application')
    }
  }

  const handleApprove = async (id) => {
    try {
      await axios.post(`/api/leaves/approve/${id}?approvedBy=Admin`)
      toast.success('Leave approved')
      fetchData()
    } catch (error) {
      toast.error('Approval failed')
    }
  }

  const handleReject = async (id) => {
    const reason = prompt('Enter rejection reason:')
    if (!reason) return
    try {
      await axios.post(`/api/leaves/reject/${id}?rejectionReason=${reason}`)
      toast.success('Leave rejected')
      fetchData()
    } catch (error) {
      toast.error('Rejection failed')
    }
  }

  const getLeaveTypeColor = (type) => {
    const colors = {
      'SICK': 'badge-danger',
      'CASUAL': 'badge-info',
      'ANNUAL': 'badge-success',
      'MATERNITY': 'badge-warning',
      'PATERNITY': 'badge-warning'
    }
    return colors[type] || 'badge-info'
  }

  if (isLoading) {
    return (
      <div className="page-container">
        <div className="container">
          <div className="loading-state">
            <div className="spinner" />
            <p>Loading...</p>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="page-container">
      <div className="container">
        <div className="page-header">
          <div>
            <h1>Leave Management</h1>
            <p>Manage employee leaves</p>
          </div>
          <button 
            className="btn btn-primary"
            onClick={() => setShowModal(true)}
          >
            <FaPlus /> Apply Leave
          </button>
        </div>

        <div className="card">
          <div className="table-container">
            <table className="table">
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
                {leaves.map((leave) => (
                  <tr key={leave.id}>
                    <td><strong>{leave.employeeName}</strong></td>
                    <td>
                      <span className={`badge ${getLeaveTypeColor(leave.leaveType)}`}>
                        {leave.leaveType}
                      </span>
                    </td>
                    <td>{format(new Date(leave.startDate), 'MMM dd, yyyy')}</td>
                    <td>{format(new Date(leave.endDate), 'MMM dd, yyyy')}</td>
                    <td>{leave.totalDays}</td>
                    <td>{leave.reason}</td>
                    <td>
                      <span className={`badge badge-${
                        leave.status === 'APPROVED' ? 'success' : 
                        leave.status === 'REJECTED' ? 'danger' : 'warning'
                      }`}>
                        {leave.status}
                      </span>
                    </td>
                    <td>
                      {leave.status === 'PENDING' && (
                        <>
                          <button 
                            className="action-btn edit"
                            onClick={() => handleApprove(leave.id)}
                            title="Approve"
                          >
                            <FaCheck />
                          </button>
                          <button 
                            className="action-btn delete"
                            onClick={() => handleReject(leave.id)}
                            title="Reject"
                          >
                            <FaTimes />
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {showModal && (
          <div className="modal-overlay" onClick={() => setShowModal(false)}>
            <div className="modal" onClick={(e) => e.stopPropagation()}>
              <div className="modal-header">
                <h3>Apply for Leave</h3>
                <button className="modal-close" onClick={() => setShowModal(false)}>×</button>
              </div>
              <form onSubmit={handleSubmit}>
                <div className="modal-body">
                  <div className="form-group">
                    <label>Employee *</label>
                    <select
                      value={formData.employeeId}
                      onChange={(e) => setFormData({...formData, employeeId: e.target.value})}
                      required
                      className="form-input"
                    >
                      <option value="">Select Employee</option>
                      {employees.map(emp => (
                        <option key={emp.id} value={emp.id}>
                          {emp.firstName} {emp.lastName}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="form-group">
                    <label>Leave Type *</label>
                    <select
                      value={formData.leaveType}
                      onChange={(e) => setFormData({...formData, leaveType: e.target.value})}
                      required
                      className="form-input"
                    >
                      <option value="CASUAL">Casual Leave</option>
                      <option value="SICK">Sick Leave</option>
                      <option value="ANNUAL">Annual Leave</option>
                      <option value="MATERNITY">Maternity Leave</option>
                      <option value="PATERNITY">Paternity Leave</option>
                    </select>
                  </div>
                  <div className="grid grid-cols-2">
                    <div className="form-group">
                      <label>Start Date *</label>
                      <input
                        type="date"
                        value={formData.startDate}
                        onChange={(e) => setFormData({...formData, startDate: e.target.value})}
                        required
                        className="form-input"
                      />
                    </div>
                    <div className="form-group">
                      <label>End Date *</label>
                      <input
                        type="date"
                        value={formData.endDate}
                        onChange={(e) => setFormData({...formData, endDate: e.target.value})}
                        required
                        className="form-input"
                      />
                    </div>
                  </div>
                  <div className="form-group">
                    <label>Reason</label>
                    <textarea
                      value={formData.reason}
                      onChange={(e) => setFormData({...formData, reason: e.target.value})}
                      rows="3"
                      className="form-input"
                    />
                  </div>
                </div>
                <div className="modal-footer">
                  <button 
                    type="button" 
                    className="btn btn-secondary"
                    onClick={() => setShowModal(false)}
                  >
                    Cancel
                  </button>
                  <button type="submit" className="btn btn-primary">
                    Submit
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default Leaves
