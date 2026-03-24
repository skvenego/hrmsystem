import { motion } from 'framer-motion';
import { Link, useLocation } from 'react-router-dom';
import { useEffect } from 'react';
import { 
  Users, 
  Calendar, 
  DollarSign, 
  FileText, 
  TrendingUp, 
  Shield,
  ArrowRight,
  CheckCircle
} from 'lucide-react';

const Home = () => {
  const { hash } = useLocation();

  useEffect(() => {
    if (hash) {
      const id = hash.replace('#', '');
      const el = document.getElementById(id);
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, [hash]);

  const features = [
    {
      icon: <Users size={32} />,
      title: 'Employee Management',
      description: 'Comprehensive employee database with profiles, documents, and performance tracking.'
    },
    {
      icon: <Calendar size={32} />,
      title: 'Attendance Tracking',
      description: 'Automated attendance system with leave management and real-time reports.'
    },
    {
      icon: <DollarSign size={32} />,
      title: 'Payroll Processing',
      description: 'Streamlined payroll with automated calculations, payslips, and tax compliance.'
    },
    {
      icon: <FileText size={32} />,
      title: 'Leave Management',
      description: 'Easy leave requests, approvals, and balance tracking for all employees.'
    },
    {
      icon: <TrendingUp size={32} />,
      title: 'Performance Reviews',
      description: '360-degree feedback system with goal setting and appraisal management.'
    },
    {
      icon: <Shield size={32} />,
      title: 'Secure & Reliable',
      description: 'Enterprise-grade security with role-based access control and data encryption.'
    }
  ];

  const containerVariants = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: {
        staggerChildren: 0.2
      }
    }
  };

  const itemVariants = {
    hidden: { opacity: 0, y: 20 },
    visible: {
      opacity: 1,
      y: 0,
      transition: { duration: 0.6 }
    }
  };

  return (
    <div>
      {/* Hero Section */}
      <section className="hero">
        <div className="hero-content">
          <motion.h1 
            className="hero-title"
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8 }}
          >
            Modern HR Management
          </motion.h1>
          <motion.p 
            className="hero-subtitle"
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.2 }}
          >
            Streamline your workforce management with our comprehensive HRM solution
          </motion.p>
          <motion.div 
            className="hero-buttons"
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.4 }}
          >
            <Link to="/register" className="btn btn-primary btn-large">
              Get Started Free <ArrowRight size={20} />
            </Link>
            <Link to="/#features" className="btn btn-secondary btn-large">
              Watch Demo
            </Link>
          </motion.div>
        </div>
      </section>

      {/* Features Section */}
      <section id="features" className="features">
        <div className="container">
          <motion.h2 
            className="section-title"
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.6 }}
          >
            Powerful Features
          </motion.h2>
          <motion.p 
            className="section-subtitle"
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.6, delay: 0.1 }}
          >
            Everything you need to manage your human resources effectively
          </motion.p>

          <motion.div 
            className="features-grid"
            variants={containerVariants}
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true }}
          >
            {features.map((feature, index) => (
              <motion.div 
                key={index}
                className="feature-card"
                variants={itemVariants}
                whileHover={{ scale: 1.02 }}
              >
                <div className="feature-icon">
                  {feature.icon}
                </div>
                <h3 className="feature-title">{feature.title}</h3>
                <p className="feature-description">{feature.description}</p>
              </motion.div>
            ))}
          </motion.div>
        </div>
      </section>

      {/* Stats Section - About */}
      <section id="about" style={{ padding: '6rem 0', background: '#f3f4f6' }}>
        <div className="container">
          <div className="stats-grid">
            {[
              { number: '10K+', label: 'Active Users' },
              { number: '500+', label: 'Companies' },
              { number: '99.9%', label: 'Uptime' },
              { number: '24/7', label: 'Support' }
            ].map((stat, index) => (
              <motion.div 
                key={index}
                className="stat-card"
                initial={{ opacity: 0, scale: 0.9 }}
                whileInView={{ opacity: 1, scale: 1 }}
                viewport={{ once: true }}
                transition={{ duration: 0.5, delay: index * 0.1 }}
              >
                <div className="stat-info" style={{ textAlign: 'center', width: '100%' }}>
                  <h3 style={{ fontSize: '2.5rem', color: '#4f46e5' }}>{stat.number}</h3>
                  <p style={{ fontSize: '1.1rem' }}>{stat.label}</p>
                </div>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section - Contact */}
      <section id="contact" style={{ padding: '6rem 0', background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' }}>
        <div className="container" style={{ textAlign: 'center' }}>
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.6 }}
          >
            <h2 style={{ fontSize: '2.5rem', color: 'white', marginBottom: '1rem' }}>
              Ready to Transform Your HR?
            </h2>
            <p style={{ fontSize: '1.25rem', color: 'rgba(255,255,255,0.9)', marginBottom: '2rem' }}>
              Join thousands of companies already using our platform
            </p>
            <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap' }}>
              <Link to="/register" className="btn btn-primary btn-large" style={{ background: 'white', color: '#667eea' }}>
                Start Free Trial
              </Link>
              <Link to="/#contact" className="btn btn-secondary btn-large">
                Contact Sales
              </Link>
            </div>
          </motion.div>
        </div>
      </section>

      {/* Benefits Section */}
      <section style={{ padding: '6rem 0', background: 'white' }}>
        <div className="container">
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '4rem', alignItems: 'center' }}>
            <motion.div
              initial={{ opacity: 0, x: -30 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.6 }}
            >
              <h2 style={{ fontSize: '2.5rem', marginBottom: '1.5rem', color: '#1f2937' }}>
                Why Choose Our Platform?
              </h2>
              <ul style={{ listStyle: 'none', space: '1.5rem' }}>
                {[
                  'Intuitive and user-friendly interface',
                  'Comprehensive reporting and analytics',
                  'Mobile-responsive design',
                  'Seamless integrations with popular tools',
                  'Dedicated customer support',
                  'Regular updates and new features'
                ].map((benefit, index) => (
                  <motion.li 
                    key={index}
                    style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1rem', color: '#4b5563' }}
                    initial={{ opacity: 0, x: -20 }}
                    whileInView={{ opacity: 1, x: 0 }}
                    viewport={{ once: true }}
                    transition={{ duration: 0.4, delay: index * 0.1 }}
                  >
                    <CheckCircle size={20} style={{ color: '#10b981', flexShrink: 0 }} />
                    {benefit}
                  </motion.li>
                ))}
              </ul>
            </motion.div>
            <motion.div
              initial={{ opacity: 0, x: 30 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.6 }}
              style={{ 
                background: 'linear-gradient(135deg, #4f46e5 0%, #06b6d4 100%)',
                borderRadius: '20px',
                padding: '3rem',
                color: 'white',
                textAlign: 'center'
              }}
            >
              <h3 style={{ fontSize: '2rem', marginBottom: '1rem' }}>Get Started Today</h3>
              <p style={{ marginBottom: '2rem', opacity: 0.9 }}>
                Experience the future of HR management with our 14-day free trial
              </p>
              <Link to="/register" className="btn btn-primary" style={{ background: 'white', color: '#4f46e5' }}>
                Create Free Account
              </Link>
            </motion.div>
          </div>
        </div>
      </section>
    </div>
  );
};

export default Home;
