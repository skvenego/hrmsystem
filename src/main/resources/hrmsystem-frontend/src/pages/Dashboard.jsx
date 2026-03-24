import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { 
  Users, 
  Calendar, 
  DollarSign, 
  TrendingUp,
  MoreVertical,
  ArrowUpRight,
  ArrowDownRight,
  UserPlus,
  FileText,
  CheckCircle,
  BarChart
} from 'lucide-react';
import Sidebar from '../components/Sidebar';
import { useState, useEffect } from 'react';

const Dashboard = () => {
  const navigate = useNavigate();
  const [stats, setStats] = useState({
    totalEmployees: 0,
    presentToday: 0,
    monthlyPayroll: 0,
    leaveRequests: 0
  });
  const [recentEmployees, setRecentEmployees] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      const token = localStorage.getItem('token');
      const headers = {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      };

      // Fetch employees count
      const employeesRes = await fetch('/api/employees', { headers });
      if (employeesRes.ok) {
        const employees = await employeesRes.json();
        setStats(prev => ({ ...prev, totalEmployees: employees.length || 0 }));
        setRecentEmployees(employees.slice(0, 5).map(emp => ({
          id: emp.id,
          name: `${emp.firstName} ${emp.lastName}`,
          role: emp.designation || 'Employee',
          department: emp.department?.name || 'N/A',
          status: emp.status || 'Active',
          joinDate: emp.joiningDate || 'N/A'
        })));
      }

      // Fetch today's attendance for present count
      const today = new Date().toISOString().split('T')[0];
      const attendanceRes = await fetch(`/api/attendance/date/${today}`, { headers });
      if (attendanceRes.ok) {
        const attendance = await attendanceRes.json();
        const presentCount = attendance.filter(a => a.status === 'PRESENT').length;
        setStats(prev => ({ ...prev, presentToday: presentCount }));
      }

      // Fetch leave requests count
      const leaveRes = await fetch('/api/leaves', { headers });
      if (leaveRes.ok) {
        const leaves = await leaveRes.json();
        const pendingLeaves = leaves.filter(l => l.status === 'PENDING').length;
        setStats(prev => ({ ...prev, leaveRequests: pendingLeaves }));
      }

      // Fetch payroll data for current month
      const currentMonth = new Date().getMonth() + 1;
      const currentYear = new Date().getFullYear();
      const payrollRes = await fetch(`/api/payroll/list?t=${Date.now()}`, { headers });
      if (payrollRes.ok) {
        const payroll = await payrollRes.json();
        // Filter by current month and year
        const currentMonthPayroll = payroll.filter(p => p.month === currentMonth && p.year === currentYear);
        const totalPayroll = currentMonthPayroll.reduce((sum, p) => sum + (p.netSalary || 0), 0);
        setStats(prev => ({ ...prev, monthlyPayroll: totalPayroll }));
      }

      setLoading(false);
    } catch (error) {
      console.error('Error fetching dashboard data:', error);
      setLoading(false);
    }
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(amount);
  };

  const statsData = [
    { 
      title: 'Total Employees', 
      value: stats.totalEmployees, 
      change: '+12%', 
      trend: 'up',
      icon: <Users size={24} />,
      color: 'blue'
    },
    { 
      title: 'Present Today', 
      value: stats.presentToday || stats.totalEmployees, 
      change: '+5%', 
      trend: 'up',
      icon: <Calendar size={24} />,
      color: 'green'
    },
    { 
      title: 'Monthly Payroll', 
      value: formatCurrency(stats.monthlyPayroll), 
      change: '+8%', 
      trend: 'up',
      icon: <DollarSign size={24} />,
      color: 'orange'
    },
    { 
      title: 'Leave Requests', 
      value: stats.leaveRequests, 
      change: '-2%', 
      trend: 'down',
      icon: <TrendingUp size={24} />,
      color: 'red'
    },
  ];

  const quickActions = [
    { label: 'Add New Employee', color: '#4f46e5', icon: <UserPlus size={18} />, path: '/dashboard/employees' },
    { label: 'Process Payroll', color: '#10b981', icon: <DollarSign size={18} />, path: '/dashboard/payroll' },
    { label: 'Approve Leave', color: '#f59e0b', icon: <CheckCircle size={18} />, path: '/dashboard/leave' },
    { label: 'Generate Report', color: '#06b6d4', icon: <BarChart size={18} />, path: '/dashboard/payslips' },
  ];

  const containerVariants = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: {
        staggerChildren: 0.1
      }
    }
  };

  const itemVariants = {
    hidden: { opacity: 0, y: 20 },
    visible: {
      opacity: 1,
      y: 0,
      transition: { duration: 0.4 }
    }
  };

  return (
    <div className="dashboard">
      <Sidebar />
      
      <main className="main-content">
        <div className="dashboard-header">
          <div>
            <h1 className="dashboard-title">Dashboard</h1>
            <p style={{ color: '#6b7280', marginTop: '0.25rem' }}>Welcome back! Here's what's happening today.</p>
          </div>
          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={() => navigate('/dashboard/employees')}
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
            <UserPlus size={18} />
            Add Employee
          </motion.button>
        </div>

        <motion.div 
          className="stats-grid"
          variants={containerVariants}
          initial="hidden"
          animate="visible"
        >
          {statsData.map((stat, index) => (
            <motion.div 
              key={index}
              className="stat-card"
              variants={itemVariants}
              whileHover={{ scale: 1.02 }}
            >
              <div className={`stat-icon ${stat.color}`}>
                {stat.icon}
              </div>
              <div className="stat-info">
                <h3>{stat.value}</h3>
                <p>{stat.title}</p>
                <div style={{ 
                  display: 'flex', 
                  alignItems: 'center', 
                  gap: '0.25rem',
                  marginTop: '0.5rem',
                  fontSize: '0.875rem',
                  color: stat.trend === 'up' ? '#10b981' : '#ef4444'
                }}>
                  {stat.trend === 'up' ? <ArrowUpRight size={16} /> : <ArrowDownRight size={16} />}
                  <span>{stat.change} from last month</span>
                </div>
              </div>
            </motion.div>
          ))}
        </motion.div>

        <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '1.5rem' }}>
          <motion.div 
            className="table-container"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, delay: 0.4 }}
          >
            <div className="table-header">
              <h3 className="table-title">Recent Employees</h3>
              <button style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#6b7280' }}>
                <MoreVertical size={20} />
              </button>
            </div>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Employee</th>
                  <th>Role</th>
                  <th>Department</th>
                  <th>Status</th>
                  <th>Join Date</th>
                </tr>
              </thead>
              <tbody>
                {recentEmployees.map((employee) => (
                  <tr key={employee.id}>
                    <td style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                      <div style={{
                        width: '36px',
                        height: '36px',
                        borderRadius: '50%',
                        background: 'linear-gradient(135deg, #4f46e5, #06b6d4)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: 'white',
                        fontWeight: 600,
                        fontSize: '0.875rem'
                      }}>
                        {employee.name.split(' ').map(n => n[0]).join('')}
                      </div>
                      {employee.name}
                    </td>
                    <td>{employee.role}</td>
                    <td>{employee.department}</td>
                    <td>
                      <span className={`badge badge-${
                        employee.status === 'Active' ? 'success' : 
                        employee.status === 'On Leave' ? 'warning' : 'danger'
                      }`}>
                        {employee.status}
                      </span>
                    </td>
                    <td>{employee.joinDate}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </motion.div>

          <motion.div 
            style={{
              background: 'white',
              borderRadius: '12px',
              padding: '1.5rem',
              boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)'
            }}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, delay: 0.5 }}
          >
            <h3 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1rem' }}>Quick Actions</h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              {quickActions.map((action, index) => (
                <motion.button
                  key={index}
                  whileHover={{ scale: 1.02, x: 5 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={() => navigate(action.path)}
                  style={{
                    padding: '0.875rem 1rem',
                    background: `${action.color}10`,
                    color: action.color,
                    border: 'none',
                    borderRadius: '8px',
                    cursor: 'pointer',
                    textAlign: 'left',
                    fontWeight: 500,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: '0.5rem'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    {action.icon}
                    {action.label}
                  </div>
                  <ArrowUpRight size={18} />
                </motion.button>
              ))}
            </div>
          </motion.div>
        </div>
      </main>
    </div>
  );
};

export default Dashboard;
