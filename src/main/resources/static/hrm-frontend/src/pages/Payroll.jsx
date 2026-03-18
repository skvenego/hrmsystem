import { useState, useEffect } from 'react'
import { FaMoneyBillWave, FaFilePdf, FaCheck, FaCalculator } from 'react-icons/fa'
import axios from 'axios'
import { toast } from 'react-toastify'

const Payroll = () => {
  const [payrolls, setPayrolls] = useState([])
  const [employees, setEmployees] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [selectedMonth, setSelectedMonth] = useState(new Date().getMonth() + 1)
  const [selectedYear, setSelectedYear] = useState(new Date().getFullYear())

  useEffect(() => {
    fetchData()
  }, [selectedMonth, selectedYear])

  const fetchData = async () => {
    try {
      const [payrollRes, employeesRes] = await Promise.all([
        axios.get(`/api/payroll/month?month=${selectedMonth}&year=${selectedYear}`),
        axios.get('/api/employees')
      ])
      setPayrolls(payrollRes.data)
      setEmployees(employeesRes.data)
    } catch (error) {
      toast.error('Failed to fetch payroll data')
    } finally {
      setIsLoading(false)
    }
  }

  const handleGeneratePayroll = async (employeeId) => {
    try {
      await axios.post(`/api/payroll/generate/${employeeId}?month=${selectedMonth}&year=${selectedYear}`)
      toast.success('Payroll generated successfully')
      fetchData()
    } catch (error) {
      toast.error('Failed to generate payroll')
    }
  }

  const handleProcessPayroll = async (payrollId) => {
    try {
      await axios.post(`/api/payroll/process/${payrollId}`)
      toast.success('Payroll processed')
      fetchData()
    } catch (error) {
      toast.error('Failed to process payroll')
    }
  }

  const handleMarkAsPaid = async (payrollId) => {
    try {
      await axios.post(`/api/payroll/pay/${payrollId}`)
      toast.success('Marked as paid')
      fetchData()
    } catch (error) {
      toast.error('Failed to mark as paid')
    }
  }

  const getStatusBadge = (status) => {
    const badges = {
      'PENDING': 'badge-warning',
      'PROCESSED': 'badge-info',
      'PAID': 'badge-success'
    }
    return badges[status] || 'badge-info'
  }

  const months = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ]

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
            <h1>Payroll</h1>
            <p>Manage employee salaries</p>
          </div>
          <div className="flex items-center gap-4">
            <select
              value={selectedMonth}
              onChange={(e) => setSelectedMonth(Number(e.target.value))}
              className="form-input"
              style={{ width: 'auto' }}
            >
              {months.map((month, index) => (
                <option key={index + 1} value={index + 1}>{month}</option>
              ))}
            </select>
            <select
              value={selectedYear}
              onChange={(e) => setSelectedYear(Number(e.target.value))}
              className="form-input"
              style={{ width: 'auto' }}
            >
              {[2023, 2024, 2025].map(year => (
                <option key={year} value={year}>{year}</option>
              ))}
            </select>
          </div>
        </div>

        <div className="stats-grid" style={{ gridTemplateColumns: 'repeat(3, 1fr)' }}>
          <div className="stat-card">
            <div className="stat-icon primary">
              <FaMoneyBillWave />
            </div>
            <div className="stat-content">
              <h3>₹{payrolls.reduce((sum, p) => sum + (p.netSalary || 0), 0).toLocaleString()}</h3>
              <p>Total Payroll</p>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon warning">
              <FaCalculator />
            </div>
            <div className="stat-content">
              <h3>{payrolls.filter(p => p.status === 'PENDING').length}</h3>
              <p>Pending</p>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon success">
              <FaCheck />
            </div>
            <div className="stat-content">
              <h3>{payrolls.filter(p => p.status === 'PAID').length}</h3>
              <p>Paid</p>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>Employee</th>
                  <th>Basic Salary</th>
                  <th>Allowances</th>
                  <th>Deductions</th>
                  <th>Net Salary</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {employees.map((employee) => {
                  const payroll = payrolls.find(p => p.employeeId === employee.id)
                  
                  if (!payroll) {
                    return (
                      <tr key={employee.id}>
                        <td><strong>{employee.firstName} {employee.lastName}</strong></td>
                        <td colSpan="6">
                          <button 
                            className="btn btn-primary btn-sm"
                            onClick={() => handleGeneratePayroll(employee.id)}
                          >
                            <FaCalculator /> Generate Payroll
                          </button>
                        </td>
                      </tr>
                    )
                  }

                  return (
                    <tr key={payroll.id}>
                      <td><strong>{payroll.employeeName}</strong></td>
                      <td>₹{payroll.basicSalary?.toLocaleString()}</td>
                      <td>₹{(payroll.hra + payroll.da + payroll.ta)?.toLocaleString()}</td>
                      <td>₹{(payroll.providentFund + payroll.tax + payroll.insurance)?.toLocaleString()}</td>
                      <td><strong>₹{payroll.netSalary?.toLocaleString()}</strong></td>
                      <td>
                        <span className={`badge ${getStatusBadge(payroll.status)}`}>
                          {payroll.status}
                        </span>
                      </td>
                      <td>
                        {payroll.status === 'PENDING' && (
                          <button 
                            className="btn btn-primary btn-sm"
                            onClick={() => handleProcessPayroll(payroll.id)}
                          >
                            Process
                          </button>
                        )}
                        {payroll.status === 'PROCESSED' && (
                          <button 
                            className="btn btn-success btn-sm"
                            onClick={() => handleMarkAsPaid(payroll.id)}
                          >
                            Mark Paid
                          </button>
                        )}
                        {payroll.status === 'PAID' && (
                          <button className="action-btn">
                            <FaFilePdf />
                          </button>
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  )
}

export default Payroll
