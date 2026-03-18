import { useEffect, useState } from 'react'
import { FaUsers, FaClock, FaMoneyBillWave, FaCalendarAlt, FaChartLine } from 'react-icons/fa'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts'
import '../styles/Dashboard.css'

const Dashboard = () => {
  const [stats, setStats] = useState({
    totalEmployees: 0,
    presentToday: 0,
    onLeave: 0,
    payrollPending: 0
  })

  // Sample data for charts
  const attendanceData = [
    { name: 'Mon', present: 45, absent: 5 },
    { name: 'Tue', present: 48, absent: 2 },
    { name: 'Wed', present: 47, absent: 3 },
    { name: 'Thu', present: 46, absent: 4 },
    { name: 'Fri', present: 44, absent: 6 },
  ]

  const departmentData = [
    { name: 'Engineering', value: 25, color: '#4f46e5' },
    { name: 'Sales', value: 15, color: '#06b6d4' },
    { name: 'Marketing', value: 10, color: '#f59e0b' },
    { name: 'HR', value: 8, color: '#10b981' },
    { name: 'Finance', value: 12, color: '#ef4444' },
  ]

  const recentActivities = [
    { id: 1, type: 'employee', message: 'New employee John Doe joined', time: '2 hours ago' },
    { id: 2, type: 'leave', message: 'Sarah Smith approved leave request', time: '3 hours ago' },
    { id: 3, type: 'payroll', message: 'January payroll processed', time: '5 hours ago' },
    { id: 4, type: 'attendance', message: 'Mark Johnson checked in', time: '6 hours ago' },
  ]

  return (
    <div className="page-container">
      <div className="container">
        <div className="page-header">
          <h1>Dashboard</h1>
          <p>Welcome back! Here's what's happening today.</p>
        </div>

        {/* Stats Cards */}
        <div className="stats-grid">
          <div className="stat-card">
            <div className="stat-icon primary">
              <FaUsers />
            </div>
            <div className="stat-content">
              <h3>50</h3>
              <p>Total Employees</p>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon success">
              <FaClock />
            </div>
            <div className="stat-content">
              <h3>45</h3>
              <p>Present Today</p>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon warning">
              <FaCalendarAlt />
            </div>
            <div className="stat-content">
              <h3>5</h3>
              <p>On Leave</p>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon danger">
              <FaMoneyBillWave />
            </div>
            <div className="stat-content">
              <h3>3</h3>
              <p>Payroll Pending</p>
            </div>
          </div>
        </div>

        {/* Charts Section */}
        <div className="content-grid">
          <div className="section-card">
            <div className="section-header">
              <h2><FaChartLine /> Weekly Attendance</h2>
            </div>
            <div className="section-body">
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={attendanceData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                  <XAxis dataKey="name" stroke="#6b7280" />
                  <YAxis stroke="#6b7280" />
                  <Tooltip 
                    contentStyle={{ 
                      background: 'white', 
                      border: '1px solid #e5e7eb',
                      borderRadius: '8px'
                    }} 
                  />
                  <Bar dataKey="present" fill="#4f46e5" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="absent" fill="#ef4444" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="section-card">
            <div className="section-header">
              <h2>Department Distribution</h2>
            </div>
            <div className="section-body">
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie
                    data={departmentData}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={100}
                    paddingAngle={5}
                    dataKey="value"
                  >
                    {departmentData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
              <div className="chart-legend">
                {departmentData.map((dept, index) => (
                  <div key={index} className="legend-item">
                    <span className="legend-dot" style={{ background: dept.color }} />
                    <span>{dept.name}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Recent Activity */}
        <div className="section-card">
          <div className="section-header">
            <h2>Recent Activity</h2>
          </div>
          <div className="section-body">
            <div className="activity-list">
              {recentActivities.map((activity) => (
                <div key={activity.id} className="activity-item">
                  <div className={`activity-icon ${activity.type}`}>
                    {activity.type === 'employee' && <FaUsers />}
                    {activity.type === 'leave' && <FaCalendarAlt />}
                    {activity.type === 'payroll' && <FaMoneyBillWave />}
                    {activity.type === 'attendance' && <FaClock />}
                  </div>
                  <div className="activity-content">
                    <p>{activity.message}</p>
                    <span className="activity-time">{activity.time}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default Dashboard
