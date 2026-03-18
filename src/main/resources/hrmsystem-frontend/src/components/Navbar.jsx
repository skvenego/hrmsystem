import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Users, Menu, X } from 'lucide-react';
import { useState } from 'react';

const Navbar = () => {
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  return (
    <motion.nav 
      className="navbar"
      initial={{ y: -100 }}
      animate={{ y: 0 }}
      transition={{ duration: 0.5 }}
    >
      <div className="container navbar-content">
        <Link to="/" className="navbar-logo">
          <Users size={32} />
          <span>HRM System</span>
        </Link>

        <ul className="navbar-links">
          <motion.li whileHover={{ scale: 1.05 }}>
            <Link to="/">Home</Link>
          </motion.li>
          <motion.li whileHover={{ scale: 1.05 }}>
            <Link to="/features">Features</Link>
          </motion.li>
          <motion.li whileHover={{ scale: 1.05 }}>
            <Link to="/about">About</Link>
          </motion.li>
          <motion.li whileHover={{ scale: 1.05 }}>
            <Link to="/contact">Contact</Link>
          </motion.li>
        </ul>

        <div className="navbar-auth">
          <motion.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
            <Link to="/login" className="btn btn-secondary">Sign In</Link>
          </motion.div>
          <motion.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
            <Link to="/register" className="btn btn-primary">Get Started</Link>
          </motion.div>
        </div>

        <button 
          className="mobile-menu-btn"
          onClick={() => setIsMenuOpen(!isMenuOpen)}
          style={{ display: 'none', background: 'none', color: 'white' }}
        >
          {isMenuOpen ? <X size={24} /> : <Menu size={24} />}
        </button>
      </div>
    </motion.nav>
  );
};

export default Navbar;
