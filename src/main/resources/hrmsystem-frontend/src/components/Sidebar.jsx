import { Link, useLocation, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { 
  LayoutDashboard, 
  Users, 
  Calendar, 
  DollarSign, 
  FileText, 
  Settings, 
  LogOut,
  Briefcase,
  UserCircle
} from 'lucide-react';

const Sidebar = () => {
  const location = useLocation();
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
    window.location.reload(); // Force reload to clear all state
  };

  const menuItems = [
    { path: '/dashboard', icon: <LayoutDashboard size={20} />, label: 'Dashboard' },
    { path: '/dashboard/employees', icon: <Users size={20} />, label: 'Employees' },
    { path: '/dashboard/attendance', icon: <Calendar size={20} />, label: 'Attendance' },
    { path: '/dashboard/leave', icon: <Briefcase size={20} />, label: 'Leave Management' },
    { path: '/dashboard/payroll', icon: <DollarSign size={20} />, label: 'Payroll' },
    { path: '/dashboard/payslips', icon: <FileText size={20} />, label: 'Payslips' },
    { path: '/dashboard/profile', icon: <UserCircle size={20} />, label: 'My Profile' },
    { path: '/dashboard/settings', icon: <Settings size={20} />, label: 'Settings' },
  ];

  return (
    <motion.aside 
      className="sidebar"
      initial={{ x: -260 }}
      animate={{ x: 0 }}
      transition={{ duration: 0.3 }}
    >
      <div className="sidebar-header">
        <Link to="/" className="sidebar-logo">
          <Users size={28} />
          <span>HRM System</span>
        </Link>
      </div>

      <nav className="sidebar-nav">
        <ul>
          {menuItems.map((item, index) => (
            <motion.li 
              key={item.path}
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.3, delay: index * 0.05 }}
              className={location.pathname === item.path ? 'active' : ''}
            >
              <Link to={item.path} style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', color: 'inherit' }}>
                {item.icon}
                {item.label}
              </Link>
            </motion.li>
          ))}
        </ul>
      </nav>

      <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, padding: '1rem', borderTop: '1px solid rgba(255,255,255,0.1)' }}>
        <motion.button
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
          onClick={handleLogout}
          style={{
            width: '100%',
            padding: '0.75rem',
            background: 'rgba(239, 68, 68, 0.2)',
            color: '#ef4444',
            border: 'none',
            borderRadius: '8px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '0.5rem',
            fontWeight: 500
          }}
        >
          <LogOut size={18} />
          Logout
        </motion.button>
      </div>
    </motion.aside>
  );
};

export default Sidebar;
