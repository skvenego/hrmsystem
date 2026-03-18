import { useState, useEffect } from 'react'
import { FaClock, FaCheckCircle, FaTimesCircle, FaCalendarAlt } from 'react-icons/fa'
import axios from 'axios'
import { toast } from 'react-toastify'
import { format } from 'date-fns'

const Attendance = () => {
  const [attendance, setAttendance] = useState([])
  const [employees, setEmployees] = useState([])
  const [selectedDate, setSelectedDate] = useState(format(new Date(), 'yyyy-MM-dd'))
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    fetchData()
  }, [selectedDate])

  const fetchData = async () => {
    try {
      const [attendanceRes, employeesRes] = await Promise.all([
        axios.get(`/api/attendance/date/${selectedDate}`),
        axios.get('/api/employees')
      ])
      setAttendance(attendanceRes.data)
      setEmployees(employeesRes.data)
    } catch (error) {
      toast.error('Failed to fetch data')
    } finally {
      setIsLoading(false)
    }
  }

  const handleCheckIn = async (employeeId) => {
    try {
      await axios.post(`/api/attendance/check-in/${employeeId}`)
      toast.success('Checked in successfully')
      fetchData()
    } catch (error) {
      toast.error('Check-in failed')
    }
  }

  const handleCheckOut = async (employeeId) => {
    try {
      await axios.post(`/api/attendance/check-out/${employeeId}`)
      toast.success('Checked out successfully')
      fetchData()
    } catch (error) {
      toast.error('Check-out failed')
    }
  }

  const getAttendanceStatus = (employeeId) => {
    const record = attendance.find(a => a.employeeId === employeeId)
    return record?.status || 'ABSENT'
  }

  const stats = {
    present: attendance.filter(a => a.status === 'PRESENT' || a.status === 'LATE').length,
    absent: employees.length - attendance.length,
    onLeave: attendance.filter(a => a.status === 'ON_LEAVE').length
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
            <h1>Attendance</h1>
            <p>Track daily attendance</p>
          </div>
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2">
              <FaCalendarAlt className="text-gray-400" />
              <input
                type="date"
                value={selectedDate}
                onChange={(e) => setSelectedDate(e.target.value)}
                className="form-input"
                style={{ width: 'auto' }}
              />
            </div>
          </div>
        </div>

        <div className="stats-grid" style={{ gridTemplateColumns: 'repeat(3, 1fr)' }}>
          <div className="stat-card">
            <div className="stat-icon success">
              <FaCheckCircle />
            </div>
            <div className="stat-content">
              <h3>{stats.present}</h3>
              <p>Present</p>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon danger">
              <FaTimesCircle />
            </div>
            <div className="stat-content">
              <h3>{stats.absent}</h3>
              <p>Absent</p>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon warning">
              <FaClock />
            </div>
            <div className="stat-content">
              <h3>{stats.onLeave}</h3>
              <p>On Leave</p>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>Employee</th>
                  <th>Check In</th>
                  <th>Check Out</th>
                  <th>Working Hours</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {employees.map((employee) => {
                  const record = attendance.find(a => a.employeeId === employee.id)
                  const status = record?.status || 'ABSENT'
                  
                  return (
                    <tr key={employee.id}>
                      <td>
                        <strong>{employee.firstName} {employee.lastName}</strong>
                      </td>
                      <td>{record?.checkInTime || '-'}</td>
                      <td>{record?.checkOutTime || '-'}</td>
                      <td>{record?.workingHours ? `${record.workingHours}h` : '-'}</td>
                      <td>
                        <span className={`badge badge-${
                          status === 'PRESENT' ? 'success' : 
                          status === 'ABSENT' ? 'danger' : 
                          status === 'LATE' ? 'warning' : 'info'
                        }`}>
                          {status}
                        </span>
                      </td>
                      <td>
                        {!record?.checkInTime && (
                          <button 
                            className="btn btn-primary btn-sm"
                            onClick={() => handleCheckIn(employee.id)}
                          >
                            Check In
                          </button>
                        )}
                        {record?.checkInTime && !record?.checkOutTime && (
                          <button 
                            className="btn btn-secondary btn-sm"
                            onClick={() => handleCheckOut(employee.id)}
                          >
                            Check Out
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

export default Attendance
