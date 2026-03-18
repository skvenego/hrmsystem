import { useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import { 
  FaUsers, 
  FaClock, 
  FaMoneyBillWave, 
  FaChartLine,
  FaShieldAlt,
  FaMobileAlt,
  FaHeadset,
  FaArrowRight,
  FaCheckCircle
} from 'react-icons/fa'
import '../styles/Home.css'

const Home = () => {
  const observerRef = useRef(null)

  useEffect(() => {
    observerRef.current = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('animate-in')
        }
      })
    }, { threshold: 0.1 })

    const elements = document.querySelectorAll('.animate-on-scroll')
    elements.forEach((el) => observerRef.current.observe(el))

    return () => observerRef.current?.disconnect()
  }, [])

  const features = [
    {
      icon: FaUsers,
      title: 'Employee Management',
      description: 'Comprehensive employee database with profiles, documents, and history tracking.'
    },
    {
      icon: FaClock,
      title: 'Attendance Tracking',
      description: 'Real-time attendance monitoring with biometric integration support.'
    },
    {
      icon: FaMoneyBillWave,
      title: 'Payroll Processing',
      description: 'Automated payroll calculation with tax compliance and payslip generation.'
    },
    {
      icon: FaChartLine,
      title: 'Analytics & Reports',
      description: 'Insightful dashboards and customizable reports for data-driven decisions.'
    },
    {
      icon: FaShieldAlt,
      title: 'Secure & Compliant',
      description: 'Enterprise-grade security with role-based access control and audit trails.'
    },
    {
      icon: FaMobileAlt,
      title: 'Mobile Ready',
      description: 'Access your HR data anywhere with our responsive mobile-friendly interface.'
    }
  ]

  const stats = [
    { value: '10K+', label: 'Active Users' },
    { value: '500+', label: 'Companies' },
    { value: '99.9%', label: 'Uptime' },
    { value: '24/7', label: 'Support' }
  ]

  const benefits = [
    'Reduce HR administrative work by 70%',
    'Automate payroll processing',
    'Streamline recruitment workflow',
    'Ensure compliance with labor laws',
    'Improve employee engagement',
    'Real-time analytics and insights'
  ]

  return (
    <div className="home-page">
      {/* Hero Section */}
      <section className="hero-section">
        <div className="hero-bg">
          <div className="gradient-orb orb-1" />
          <div className="gradient-orb orb-2" />
          <div className="gradient-orb orb-3" />
        </div>
        <div className="container">
          <div className="hero-content">
            <div className="hero-badge">
              <span className="badge-dot" />
              <span>Now with AI-powered insights</span>
            </div>
            <h1 className="hero-title">
              Modern HR Management
              <span className="gradient-text"> Simplified</span>
            </h1>
            <p className="hero-description">
              Streamline your HR operations with our comprehensive solution. 
              From recruitment to retirement, manage your entire workforce 
              efficiently in one powerful platform.
            </p>
            <div className="hero-cta">
              <Link to="/register" className="btn btn-primary btn-lg">
                Get Started Free
                <FaArrowRight />
              </Link>
              <Link to="/login" className="btn btn-secondary btn-lg">
                Sign In
              </Link>
            </div>
            <div className="hero-stats">
              {stats.map((stat, index) => (
                <div key={index} className="stat-item">
                  <span className="stat-value">{stat.value}</span>
                  <span className="stat-label">{stat.label}</span>
                </div>
              ))}
            </div>
          </div>
          <div className="hero-visual">
            <div className="dashboard-preview">
              <div className="preview-header">
                <div className="preview-dots">
                  <span />
                  <span />
                  <span />
                </div>
              </div>
              <div className="preview-content">
                <div className="preview-card">
                  <div className="preview-card-header">
                    <div className="preview-avatar" />
                    <div className="preview-lines">
                      <div />
                      <div />
                    </div>
                  </div>
                  <div className="preview-chart" />
                </div>
                <div className="preview-stats">
                  <div className="preview-stat" />
                  <div className="preview-stat" />
                  <div className="preview-stat" />
                </div>
              </div>
            </div>
          </div>
        </div>
        <div className="scroll-indicator">
          <div className="mouse">
            <div className="wheel" />
          </div>
          <span>Scroll to explore</span>
        </div>
      </section>

      {/* Features Section */}
      <section className="features-section">
        <div className="container">
          <div className="section-header animate-on-scroll">
            <span className="section-tag">Features</span>
            <h2>Everything you need to manage HR</h2>
            <p>Powerful features to help you manage your workforce efficiently</p>
          </div>
          <div className="features-grid">
            {features.map((feature, index) => {
              const Icon = feature.icon
              return (
                <div 
                  key={index} 
                  className="feature-card animate-on-scroll"
                  style={{ animationDelay: `${index * 0.1}s` }}
                >
                  <div className="feature-icon">
                    <Icon />
                  </div>
                  <h3>{feature.title}</h3>
                  <p>{feature.description}</p>
                </div>
              )
            })}
          </div>
        </div>
      </section>

      {/* Benefits Section */}
      <section className="benefits-section">
        <div className="container">
          <div className="benefits-grid">
            <div className="benefits-content animate-on-scroll">
              <span className="section-tag">Why Choose Us</span>
              <h2>Transform your HR operations</h2>
              <p>
                Join thousands of companies that have streamlined their HR processes 
                and improved employee satisfaction with our platform.
              </p>
              <ul className="benefits-list">
                {benefits.map((benefit, index) => (
                  <li key={index} className="benefit-item">
                    <FaCheckCircle className="benefit-icon" />
                    <span>{benefit}</span>
                  </li>
                ))}
              </ul>
              <Link to="/register" className="btn btn-primary">
                Start Free Trial
                <FaArrowRight />
              </Link>
            </div>
            <div className="benefits-visual animate-on-scroll">
              <div className="floating-cards">
                <div className="float-card card-1">
                  <div className="float-icon">
                    <FaUsers />
                  </div>
                  <div className="float-content">
                    <span className="float-value">2,547</span>
                    <span className="float-label">Total Employees</span>
                  </div>
                </div>
                <div className="float-card card-2">
                  <div className="float-icon success">
                    <FaChartLine />
                  </div>
                  <div className="float-content">
                    <span className="float-value">+23%</span>
                    <span className="float-label">Growth Rate</span>
                  </div>
                </div>
                <div className="float-card card-3">
                  <div className="float-icon warning">
                    <FaClock />
                  </div>
                  <div className="float-content">
                    <span className="float-value">98.5%</span>
                    <span className="float-label">Attendance</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="cta-section">
        <div className="container">
          <div className="cta-content animate-on-scroll">
            <h2>Ready to streamline your HR?</h2>
            <p>
              Get started with a free 14-day trial. No credit card required. 
              Cancel anytime.
            </p>
            <div className="cta-buttons">
              <Link to="/register" className="btn btn-white btn-lg">
                Get Started Free
              </Link>
              <a href="#contact" className="btn btn-outline-white btn-lg">
                Contact Sales
              </a>
            </div>
            <div className="cta-support">
              <FaHeadset />
              <span>Need help? Our team is available 24/7</span>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="footer">
        <div className="container">
          <div className="footer-grid">
            <div className="footer-brand">
              <div className="footer-logo">
                <FaUsers />
                <span>HRM<span className="highlight">System</span></span>
              </div>
              <p>
                Modern HR management solution for businesses of all sizes. 
                Simplify your workforce management.
              </p>
            </div>
            <div className="footer-links">
              <h4>Product</h4>
              <ul>
                <li><Link to="/features">Features</Link></li>
                <li><Link to="/pricing">Pricing</Link></li>
                <li><Link to="/integrations">Integrations</Link></li>
                <li><Link to="/api">API</Link></li>
              </ul>
            </div>
            <div className="footer-links">
              <h4>Company</h4>
              <ul>
                <li><Link to="/about">About</Link></li>
                <li><Link to="/blog">Blog</Link></li>
                <li><Link to="/careers">Careers</Link></li>
                <li><Link to="/contact">Contact</Link></li>
              </ul>
            </div>
            <div className="footer-links">
              <h4>Support</h4>
              <ul>
                <li><Link to="/help">Help Center</Link></li>
                <li><Link to="/docs">Documentation</Link></li>
                <li><Link to="/status">System Status</Link></li>
              </ul>
            </div>
          </div>
          <div className="footer-bottom">
            <p>&copy; 2024 HRM System. All rights reserved.</p>
          </div>
        </div>
      </footer>
    </div>
  )
}

export default Home
