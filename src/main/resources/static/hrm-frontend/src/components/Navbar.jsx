import { useState, useEffect } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { 
  FaHome, 
  FaUsers, 
  FaBuilding, 
  FaClock, 
  FaCalendarAlt, 
  FaMoneyBillWave,
  FaChartDashboard,
  FaBars,
  FaTimes,
  FaUserCircle,
  FaSignOutAlt,
  FaChevronDown
} from 'react-icons/fa'
import '../styles/Navbar.css'

const Navbar = () => {
  const [isOpen, setIsOpen] = useState(false)
  const [isScrolled, setIsScrolled] = useState(false)
  const [showProfileMenu, setShowProfileMenu] = useState(false)
  const { user, logout } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20)
    }
    window.addEventListener('scroll', handleScroll)
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const navLinks = [
    { path: '/', label: 'Home', icon: FaHome, public: true },
    { path: '/dashboard', label: 'Dashboard', icon: FaChartDashboard, public: false },
    { path: '/employees', label: 'Employees', icon: FaUsers, public: false },
    { path: '/departments', label: 'Departments', icon: FaBuilding, public: false },
    { path: '/attendance', label: 'Attendance', icon: FaClock, public: false },
    { path: '/leaves', label: 'Leaves', icon: FaCalendarAlt, public: false },
    { path: '/payroll', label: 'Payroll', icon: FaMoneyBillWave, public: false },
  ]

  const isActive = (path) => location.pathname === path

  return (
    <nav className={`navbar ${isScrolled ? 'scrolled' : ''}`}>
      <div className="navbar-container">
        <Link to="/" className="navbar-logo">
          <div className="logo-icon">
            <FaUsers />
          </div>
          <span className="logo-text">HRM<span className="logo-highlight">System</span></span>
        </Link>

        <button 
          className="mobile-menu-btn"
          onClick={() => setIsOpen(!isOpen)}
          aria-label="Toggle menu"
        >
          {isOpen ? <FaTimes /> : <FaBars />}
        </button>

        <div className={`navbar-menu ${isOpen ? 'active' : ''}`}>
          <ul className="navbar-links">
            {navLinks.map((link) => {
              if (!link.public && !user) return null
              const Icon = link.icon
              return (
                <li key={link.path}>
                  <Link
                    to={link.path}
                    className={`nav-link ${isActive(link.path) ? 'active' : ''}`}
                    onClick={() => setIsOpen(false)}
                  >
                    <Icon className="nav-icon" />
                    <span>{link.label}</span>
                    {isActive(link.path) && <div className="active-indicator" />}
                  </Link>
                </li>
              )
            })}
          </ul>

          <div className="navbar-actions">
            {user ? (
              <div className="profile-dropdown">
                <button 
                  className="profile-btn"
                  onClick={() => setShowProfileMenu(!showProfileMenu)}
                >
                  <div className="profile-avatar">
                    {user.username.charAt(0).toUpperCase()}
                  </div>
                  <span className="profile-name">{user.username}</span>
                  <FaChevronDown className={`dropdown-arrow ${showProfileMenu ? 'open' : ''}`} />
                </button>
                
                {showProfileMenu && (
                  <div className="dropdown-menu">
                    <div className="dropdown-header">
                      <FaUserCircle className="dropdown-icon" />
                      <div>
                        <p className="dropdown-name">{user.username}</p>
                        <p className="dropdown-role">{user.role}</p>
                      </div>
                    </div>
                    <div className="dropdown-divider" />
                    <button onClick={handleLogout} className="dropdown-item logout">
                      <FaSignOutAlt />
                      <span>Logout</span>
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <div className="auth-buttons">
                <Link to="/login" className="btn btn-secondary btn-sm">
                  Login
                </Link>
                <Link to="/register" className="btn btn-primary btn-sm">
                  Register
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </nav>
  )
}

export default Navbar
