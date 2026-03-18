import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Calendar, Clock, CheckCircle, XCircle, UserCheck, UserX } from 'lucide-react';
import Sidebar from '../components/Sidebar';

const Attendance = () => {
  const [attendanceRecords, setAttendanceRecords] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
  const [isHistoryMode, setIsHistoryMode] = useState(false);

  // Office timings
  const OFFICE_START_HOUR = 10; // 10:00 AM
  const OFFICE_END_HOUR = 18; // 6:00 PM
  const OFFICE_END_MINUTE = 30; // 30 minutes

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(amount || 0);
  };

  // Check if date is in past or future
  const isPastDate = (dateString) => {
    const selected = new Date(dateString);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return selected < today;
  };

  // Fetch employees and attendance data
  const fetchData = async () => {
    try {
      const token = localStorage.getItem('token');
      const historyMode = isPastDate(selectedDate);
      setIsHistoryMode(historyMode);
      
      // Always fetch employees
      const employeesResponse = await fetch('/api/employees', {
        headers: { 
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (!employeesResponse.ok) {
        console.error('Failed to fetch employees:', employeesResponse.status);
        const errorText = await employeesResponse.text();
        console.error('Error details:', errorText);
        setLoading(false);
        return;
      }
      
      const employeesData = await employeesResponse.json();
      console.log('Fetched employees:', employeesData);
      setEmployees(employeesData);
      
      // Only fetch attendance for past dates
      if (historyMode) {
        const attendanceResponse = await fetch(`/api/attendance/date/${selectedDate}`, {
          headers: { 
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        });
        
        let attendanceData = [];
        if (attendanceResponse.ok) {
          attendanceData = await attendanceResponse.json();
          console.log('Fetched attendance:', attendanceData);
        } else {
          console.log('No attendance records found for this date');
        }
        
        // Create records for all employees (including those without attendance)
        const employeeIdsWithAttendance = new Set(attendanceData.map(a => a.employeeId));
        const completeRecords = [
          ...attendanceData,
          ...employeesData
            .filter(emp => !employeeIdsWithAttendance.has(emp.id))
            .map(emp => ({
              id: null,
              employeeId: emp.id,
              employeeName: `${emp.firstName} ${emp.lastName}`,
              date: selectedDate,
              checkInTime: null,
              checkOutTime: null,
              workingHours: 0,
              status: 'NOT_MARKED',
              remarks: null
            }))
        ];
        
        console.log('Complete attendance records:', completeRecords);
        setAttendanceRecords(completeRecords);
      } else {
        // Future date - show empty records
        console.log('Future date selected - showing empty records');
        const emptyRecords = employeesData.map(emp => ({
          id: null,
          employeeId: emp.id,
          employeeName: `${emp.firstName} ${emp.lastName}`,
          date: selectedDate,
          checkInTime: null,
          checkOutTime: null,
          workingHours: 0,
          status: 'NOT_MARKED',
          remarks: null
        }));
        setAttendanceRecords(emptyRecords);
      }
      
      setLoading(false);
    } catch (error) {
      console.error('Error fetching data:', error);
      alert('Error loading data: ' + error.message);
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [selectedDate]);

  // Mark attendance as Present
  const handlePresent = async (employeeId, recordId) => {
    try {
      const token = localStorage.getItem('token');
      
      if (recordId) {
        // Update existing record
        const response = await fetch(`/api/attendance/${recordId}/mark-present`, {
          method: 'PUT',
          headers: { 
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ 
            status: 'PRESENT',
            workingHours: 8
          })
        });
        
        if (response.ok) {
          alert('✓ Marked as Present - Full day salary');
          fetchData();
        } else {
          const error = await response.text();
          alert('Error: ' + error);
        }
      } else {
        // Create new attendance record for selected date
        const response = await fetch(`/api/attendance/mark-present/${employeeId}?date=${selectedDate}`, {
          method: 'POST',
          headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (response.ok) {
          alert('✓ Marked as Present - Full day salary');
          fetchData();
        } else {
          const error = await response.text();
          alert('Error: ' + error);
        }
      }
    } catch (error) {
      alert('Error marking present: ' + error.message);
    }
  };

  // Mark attendance as Absent
  const handleAbsent = async (employeeId, recordId) => {
    try {
      const token = localStorage.getItem('token');
      
      const response = await fetch(`/api/attendance/mark-absent/${employeeId}?date=${selectedDate}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      
      if (response.ok) {
        alert('Marked as Absent - Full day salary will be deducted');
        fetchData();
      } else {
        const error = await response.text();
        alert('Error: ' + error);
      }
    } catch (error) {
      alert('Error marking absent: ' + error.message);
    }
  };

  // Show Half Day / Full Day options for Present status
  const handlePresentOptions = async (employeeId, recordId, isHalfDay) => {
    try {
      const token = localStorage.getItem('token');
      const workingHours = isHalfDay ? 4 : 8;
      
      if (recordId) {
        // Update existing record
        const response = await fetch(`/api/attendance/${recordId}/mark-present`, {
          method: 'PUT',
          headers: { 
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ 
            status: isHalfDay ? 'HALF_DAY' : 'PRESENT',
            workingHours: workingHours
          })
        });
        
        if (response.ok) {
          alert(`Marked as ${isHalfDay ? 'Half Day' : 'Full Day'} - ${isHalfDay ? '0.5 day' : '1 day'} salary calculated`);
          fetchData();
        } else {
          const error = await response.text();
          alert('Error: ' + error);
        }
      }
    } catch (error) {
      alert('Error marking attendance: ' + error.message);
    }
  };

  // Calculate statistics
  const presentCount = attendanceRecords.filter(r => r.status === 'PRESENT' || r.status === 'LATE').length;
  const absentCount = attendanceRecords.filter(r => r.status === 'ABSENT').length;
  const halfDayCount = attendanceRecords.filter(r => r.status === 'HALF_DAY').length;
  const notMarkedCount = attendanceRecords.filter(r => !r.status || r.status === 'NOT_MARKED').length;

  const getStatusBadge = (status) => {
    const statusColors = {
      'PRESENT': 'success',
      'ABSENT': 'danger',
      'LATE': 'warning',
      'HALF_DAY': 'info',
      'NOT_MARKED': 'secondary'
    };
    return statusColors[status] || 'secondary';
  };

  return (
    <div className="dashboard">
      <Sidebar />
      <main className="main-content">
        <div className="dashboard-header">
          <div>
            <h1 className="dashboard-title">Attendance Management</h1>
            <p style={{ color: '#6b7280', marginTop: '0.25rem' }}>
              {isHistoryMode ? 'Viewing historical attendance' : 'Mark today\'s attendance'} • Office Hours: 10:00 AM - 6:30 PM
            </p>
          </div>
          <input
            type="date"
            value={selectedDate}
            onChange={(e) => setSelectedDate(e.target.value)}
            style={{
              padding: '0.75rem 1rem',
              border: '1px solid #d1d5db',
              borderRadius: '8px',
              fontSize: '0.875rem',
              fontWeight: 500
            }}
          />
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '1rem', marginBottom: '1.5rem' }}>
          {[
            { label: 'Present Today', value: presentCount.toString(), icon: <CheckCircle />, color: '#10b981' },
            { label: 'Absent Today', value: absentCount.toString(), icon: <XCircle />, color: '#ef4444' },
            { label: 'Half Day', value: halfDayCount.toString(), icon: <Clock />, color: '#f59e0b' },
            { label: 'Not Marked', value: notMarkedCount.toString(), icon: <UserX />, color: '#6b7280' },
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
          <div style={{ textAlign: 'center', padding: '3rem', color: '#6b7280' }}>
            <p>Loading attendance records...</p>
          </div>
        ) : (
          <motion.div
            className="table-container"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <div className="table-header">
              <h3 className="table-title">Attendance Records - {new Date(selectedDate).toLocaleDateString()}</h3>
            </div>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Employee</th>
                  <th>Date</th>
                  <th>Check In</th>
                  <th>Check Out</th>
                  <th>Working Hours</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {attendanceRecords.map((record) => (
                  <tr key={record.employeeId || record.id}>
                    <td>
                      <div>
                        <div style={{ fontWeight: 600 }}>{record.employeeName}</div>
                        <div style={{ fontSize: '0.75rem', color: '#6b7280' }}>EMP{String(record.employeeId).padStart(3, '0')}</div>
                      </div>
                    </td>
                    <td>{new Date(record.date).toLocaleDateString()}</td>
                    <td>{record.checkInTime ? new Date(`2000-01-01T${record.checkInTime}`).toLocaleTimeString() : '-'}</td>
                    <td>{record.checkOutTime ? new Date(`2000-01-01T${record.checkOutTime}`).toLocaleTimeString() : '-'}</td>
                    <td>{record.workingHours ? `${record.workingHours}h` : '-'}</td>
                    <td>
                      <span className={`badge badge-${getStatusBadge(record.status)}`}>
                        {record.status || 'NOT MARKED'}
                      </span>
                    </td>
                    <td>
                      {(!record.status || record.status === 'NOT_MARKED') && (
                        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                          <button 
                            onClick={() => handlePresent(record.employeeId, record.id)}
                            style={{
                              padding: '0.5rem 1rem',
                              background: '#10b981',
                              color: 'white',
                              border: 'none',
                              borderRadius: '6px',
                              cursor: 'pointer',
                              fontSize: '0.75rem',
                              fontWeight: 600,
                              display: 'flex',
                              alignItems: 'center',
                              gap: '0.25rem'
                            }}
                            title="Mark Present - Full day salary"
                          >
                            <CheckCircle size={14} />
                            Present
                          </button>
                          <button 
                            onClick={() => handleAbsent(record.employeeId, record.id)}
                            style={{
                              padding: '0.5rem 1rem',
                              background: '#ef4444',
                              color: 'white',
                              border: 'none',
                              borderRadius: '6px',
                              cursor: 'pointer',
                              fontSize: '0.75rem',
                              fontWeight: 600,
                              display: 'flex',
                              alignItems: 'center',
                              gap: '0.25rem'
                            }}
                            title="Mark Absent - Full day salary deducted"
                          >
                            <XCircle size={14} />
                            Absent
                          </button>
                        </div>
                      )}
                      {record.status === 'PRESENT' && (
                        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                          <button 
                            onClick={() => handleAbsent(record.employeeId, record.id)}
                            style={{
                              padding: '0.5rem 1rem',
                              background: '#ef4444',
                              color: 'white',
                              border: 'none',
                              borderRadius: '6px',
                              cursor: 'pointer',
                              fontSize: '0.75rem',
                              fontWeight: 600,
                              display: 'flex',
                              alignItems: 'center',
                              gap: '0.25rem'
                            }}
                            title="Toggle to Absent"
                          >
                            <XCircle size={14} />
                            Absent
                          </button>
                          <span style={{ color: '#10b981', fontWeight: 600, fontSize: '0.875rem' }}>
                            ✓ Present - Full day salary
                          </span>
                        </div>
                      )}
                      {record.status === 'ABSENT' && (
                        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                          <button 
                            onClick={() => handlePresent(record.employeeId, record.id)}
                            style={{
                              padding: '0.5rem 1rem',
                              background: '#10b981',
                              color: 'white',
                              border: 'none',
                              borderRadius: '6px',
                              cursor: 'pointer',
                              fontSize: '0.75rem',
                              fontWeight: 600,
                              display: 'flex',
                              alignItems: 'center',
                              gap: '0.25rem'
                            }}
                            title="Toggle to Present"
                          >
                            <CheckCircle size={14} />
                            Present
                          </button>
                          <span style={{ color: '#ef4444', fontWeight: 600, fontSize: '0.875rem' }}>
                            ✗ Absent - Salary deducted
                          </span>
                        </div>
                      )}
                      {record.status === 'HALF_DAY' && (
                        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
                          <span style={{ color: '#f59e0b', fontWeight: 600, fontSize: '0.875rem' }}>
                            ◐ Half Day - 0.5 day salary
                          </span>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </motion.div>
        )}
      </main>
    </div>
  );
};

export default Attendance;
