import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { CheckCircle, XCircle, Clock, UserX } from 'lucide-react';
import Sidebar from '../components/Sidebar';

const Attendance = () => {
  const [attendanceRecords, setAttendanceRecords] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedDate, setSelectedDate] = useState(
    new Date().toISOString().split('T')[0]
  );
  const [showPopup, setShowPopup] = useState(false);
  const [popupMessage, setPopupMessage] = useState('');

  // ✅ FIX DATE (IMPORTANT)
  const parseLocalDate = (dateString) => {
    const [y, m, d] = dateString.split('-');
    return new Date(y, m - 1, d);
  };

  const isFutureDate = (dateString) => {
    const selected = parseLocalDate(dateString);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return selected > today;
  };

  const isToday = (dateString) => {
    const selected = parseLocalDate(dateString);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    // Compare as strings to avoid timezone issues
    const selectedStr = dateString;
    const todayStr = today.getFullYear() + '-' + 
                     String(today.getMonth() + 1).padStart(2, '0') + '-' + 
                     String(today.getDate()).padStart(2, '0');
    
    return selectedStr === todayStr;
  };

  const isPastDate = (dateString) => {
    const selected = parseLocalDate(dateString);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return selected < today;
  };

  // ✅ HANDLE DATE CHANGE
  const handleDateChange = (e) => {
    const newDate = e.target.value;
    
    if (isFutureDate(newDate)) {
      setPopupMessage('⚠️ You cannot change attendance for future dates!');
      setShowPopup(true);
      // Don't update the date, keep today's date
      return;
    }
    
    setSelectedDate(newDate);
  };

  const closePopup = () => {
    setShowPopup(false);
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(amount || 0);
  };

  // ✅ FETCH DATA (FIXED)
  const fetchData = async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem('token');

      const empRes = await fetch('/api/employees', {
        headers: { Authorization: `Bearer ${token}` },
      });

      const employeesData = await empRes.json();
      setEmployees(employeesData);

      // ❌ FUTURE DATE → EMPTY BUT ALLOW ACTION BLOCK
      if (isFutureDate(selectedDate)) {
        const empty = employeesData.map((emp) => ({
          employeeId: emp.id,
          employeeName: `${emp.firstName} ${emp.lastName}`,
          date: selectedDate,
          status: 'NOT_MARKED',
        }));
        setAttendanceRecords(empty);
        setLoading(false);
        return;
      }

      // ✅ TODAY + PAST
      const attRes = await fetch(`/api/attendance/date/${selectedDate}`, {
        headers: { Authorization: `Bearer ${token}` },
      });

      let attendanceData = [];
      if (attRes.ok) {
        attendanceData = await attRes.json();
      }

      const ids = new Set(attendanceData.map((a) => a.employeeId));

      const records = [
        ...attendanceData,
        ...employeesData
          .filter((e) => !ids.has(e.id))
          .map((e) => ({
            employeeId: e.id,
            employeeName: `${e.firstName} ${e.lastName}`,
            date: selectedDate,
            status: 'NOT_MARKED',
          })),
      ];

      setAttendanceRecords(records);
      setLoading(false);
    } catch (err) {
      console.error(err);
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [selectedDate]);

  // ✅ MARK PRESENT
  const handlePresent = async (employeeId) => {
    const token = localStorage.getItem('token');

    if (isFutureDate(selectedDate)) {
      setPopupMessage('⚠️ You cannot mark attendance for future dates!');
      setShowPopup(true);
      return;
    }

    try {
      const response = await fetch(
        `/api/attendance/mark-present/${employeeId}?date=${selectedDate}`,
        { method: 'POST', headers: { Authorization: `Bearer ${token}` } }
      );

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Failed to mark present');
      }

      fetchData();
    } catch (error) {
      console.error('Error marking present:', error);
      setPopupMessage('❌ ' + error.message);
      setShowPopup(true);
    }
  };

  // ✅ MARK ABSENT
  const handleAbsent = async (employeeId) => {
    const token = localStorage.getItem('token');

    if (isFutureDate(selectedDate)) {
      setPopupMessage('⚠️ You cannot mark attendance for future dates!');
      setShowPopup(true);
      return;
    }

    try {
      const response = await fetch(
        `/api/attendance/mark-absent/${employeeId}?date=${selectedDate}`,
        { method: 'POST', headers: { Authorization: `Bearer ${token}` } }
      );

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Failed to mark absent');
      }

      fetchData();
    } catch (error) {
      console.error('Error marking absent:', error);
      setPopupMessage('❌ ' + error.message);
      setShowPopup(true);
    }
  };

  // ✅ SALARY CALCULATION (Per Day)
  const calculateSalary = (employeeId, status) => {
    const emp = employees.find((e) => e.id === employeeId);
    if (!emp || !emp.salary) return 0;

    const perDaySalary = emp.salary / 30;

    if (status === 'PRESENT' || status === 'LATE') {
      return perDaySalary;
    }
    if (status === 'HALF_DAY') {
      return perDaySalary / 2;
    }
    // ABSENT, NOT_MARKED, ON_LEAVE = 0
    return 0;
  };

  // ✅ CALCULATE TOTAL SALARY FOR MONTH BASED ON ATTENDANCE
  const calculateTotalSalaryForMonth = (employeeId) => {
    const emp = employees.find((e) => e.id === employeeId);
    if (!emp || !emp.salary) return 0;

    const perDaySalary = emp.salary / 30;
    
    // Get all attendance records for this employee in the selected month
    const employeeRecords = attendanceRecords.filter(r => r.employeeId === employeeId);
    
    let totalSalary = 0;
    employeeRecords.forEach(record => {
      if (record.status === 'PRESENT' || record.status === 'LATE') {
        totalSalary += perDaySalary;
      } else if (record.status === 'HALF_DAY') {
        totalSalary += (perDaySalary / 2);
      }
      // ABSENT, NOT_MARKED, ON_LEAVE = 0
    });
    
    return totalSalary;
  };

  // ✅ STATS
  const presentCount = attendanceRecords.filter(
    (r) => r.status === 'PRESENT'
  ).length;

  const absentCount = attendanceRecords.filter(
    (r) => r.status === 'ABSENT'
  ).length;

  const halfDayCount = attendanceRecords.filter(
    (r) => r.status === 'HALF_DAY'
  ).length;

  const notMarkedCount = attendanceRecords.filter(
    (r) => !r.status || r.status === 'NOT_MARKED'
  ).length;

  // ✅ SUMMARY MESSAGE
  const getSummaryMessage = () => {
    if (isFutureDate(selectedDate)) {
      return "Future date - No attendance records";
    } else if (isToday(selectedDate)) {
      return `Today's Attendance: ${presentCount} Present, ${absentCount} Absent, ${halfDayCount} Half-Day, ${notMarkedCount} Not Marked`;
    } else {
      return `Past Date (${selectedDate}): ${presentCount} Present, ${absentCount} Absent, ${halfDayCount} Half-Day, ${notMarkedCount} Not Marked`;
    }
  };

  return (
    <div className="dashboard">
      <Sidebar />
      <main className="main-content">
        {/* Popup Modal */}
        {showPopup && (
          <div style={{
            position: 'fixed',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            backgroundColor: 'white',
            padding: '30px',
            borderRadius: '10px',
            boxShadow: '0 4px 6px rgba(0, 0, 0, 0.3)',
            zIndex: 1000,
            minWidth: '300px',
            textAlign: 'center'
          }}>
            <p style={{ fontSize: '16px', marginBottom: '20px' }}>{popupMessage}</p>
            <button
              onClick={closePopup}
              style={{
                padding: '10px 20px',
                backgroundColor: '#007bff',
                color: 'white',
                border: 'none',
                borderRadius: '5px',
                cursor: 'pointer',
                fontSize: '14px'
              }}
            >
              OK
            </button>
          </div>
        )}

        <h2>Attendance - {selectedDate}</h2>

        <input
          type="date"
          value={selectedDate}
          onChange={handleDateChange}
        />

        {isPastDate(selectedDate) && (
          <p style={{ color: 'orange', fontWeight: 'bold' }}>
            ℹ️ Viewing past date - You can change attendance from Present to Absent and vice versa
          </p>
        )}

        {isToday(selectedDate) && (
          <p style={{ color: 'green', fontWeight: 'bold' }}>
            ✅ Today's Date - You can mark and update attendance freely
          </p>
        )}

        <div style={{ 
          marginTop: '15px', 
          padding: '10px', 
          backgroundColor: '#f0f0f0', 
          borderRadius: '5px',
          marginBottom: '20px'
        }}>
          <strong>📊 Summary:</strong> {getSummaryMessage()}
        </div>

        {loading ? (
          <p>Loading...</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Status</th>
                <th>Daily Salary</th>
                <th>Action</th>
              </tr>
            </thead>

            <tbody>
              {attendanceRecords.map((r) => (
                <tr key={r.employeeId}>
                  <td>{r.employeeName}</td>

                  <td>{r.status}</td>

                  <td>{formatCurrency(calculateSalary(r.employeeId, r.status))}</td>

                  <td>
                    {/* ✅ NOT MARKED - Allow marking for today and past dates */}
                    {r.status === 'NOT_MARKED' && !isFutureDate(selectedDate) && (
                      <>
                        <button onClick={() => handlePresent(r.employeeId)}>
                          Present
                        </button>
                        <button onClick={() => handleAbsent(r.employeeId)}>
                          Absent
                        </button>
                      </>
                    )}

                    {/* ✅ PRESENT → Can change to ABSENT for past dates AND today */}
                    {r.status === 'PRESENT' && !isFutureDate(selectedDate) && (isPastDate(selectedDate) || isToday(selectedDate)) && (
                      <button onClick={() => handleAbsent(r.employeeId)}>
                        Make Absent
                      </button>
                    )}

                    {/* ✅ ABSENT → Can change to PRESENT for past dates AND today */}
                    {r.status === 'ABSENT' && !isFutureDate(selectedDate) && (isPastDate(selectedDate) || isToday(selectedDate)) && (
                      <button onClick={() => handlePresent(r.employeeId)}>
                        Make Present
                      </button>
                    )}

                    {/* ✅ Already marked today - show indicator but still allow changes */}
                    {isToday(selectedDate) && (r.status === 'PRESENT' || r.status === 'ABSENT') && (
                      <span style={{ color: '#007bff', fontWeight: 'bold', marginLeft: '10px' }}>
                        (Today)
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </main>
    </div>
  );
};

export default Attendance;