import { motion } from 'framer-motion';
import { User, Mail, Phone, MapPin, Briefcase, Calendar, Edit2 } from 'lucide-react';
import Sidebar from '../components/Sidebar';
import { useAuth } from '../context/AuthContext';

const Profile = () => {
  const { user } = useAuth();

  const userInfo = {
    name: user?.username || 'John Doe',
    email: user?.email || 'john@example.com',
    phone: '+1 234 567 890',
    address: '123 Main Street, New York, NY 10001',
    department: 'Information Technology',
    position: 'Software Engineer',
    joinDate: '2023-01-15',
    role: user?.role || 'EMPLOYEE'
  };

  return (
    <div className="dashboard">
      <Sidebar />
      <main className="main-content">
        <div className="dashboard-header">
          <div>
            <h1 className="dashboard-title">My Profile</h1>
            <p style={{ color: '#6b7280', marginTop: '0.25rem' }}>Manage your personal information</p>
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '1.5rem' }}>
          {/* Profile Card */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            style={{
              background: 'white',
              borderRadius: '12px',
              padding: '2rem',
              boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
              textAlign: 'center'
            }}
          >
            <div style={{
              width: '120px',
              height: '120px',
              borderRadius: '50%',
              background: 'linear-gradient(135deg, #4f46e5, #06b6d4)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 1rem',
              color: 'white',
              fontSize: '3rem',
              fontWeight: 600
            }}>
              {userInfo.name.split(' ').map(n => n[0]).join('')}
            </div>
            <h2 style={{ fontSize: '1.5rem', marginBottom: '0.25rem' }}>{userInfo.name}</h2>
            <p style={{ color: '#6b7280', marginBottom: '0.5rem' }}>{userInfo.position}</p>
            <span className="badge badge-success">{userInfo.role}</span>
            
            <div style={{ marginTop: '1.5rem', paddingTop: '1.5rem', borderTop: '1px solid #e5e7eb' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.75rem', color: '#6b7280' }}>
                <Briefcase size={18} />
                <span>{userInfo.department}</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', color: '#6b7280' }}>
                <Calendar size={18} />
                <span>Joined {userInfo.joinDate}</span>
              </div>
            </div>
          </motion.div>

          {/* Details Card */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            style={{
              background: 'white',
              borderRadius: '12px',
              padding: '2rem',
              boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
              <h3 style={{ fontSize: '1.25rem', fontWeight: 600 }}>Personal Information</h3>
              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                style={{
                  padding: '0.5rem 1rem',
                  background: '#4f46e5',
                  color: 'white',
                  border: 'none',
                  borderRadius: '6px',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem'
                }}
              >
                <Edit2 size={16} />
                Edit
              </motion.button>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
              {[
                { icon: <User size={20} />, label: 'Full Name', value: userInfo.name },
                { icon: <Mail size={20} />, label: 'Email Address', value: userInfo.email },
                { icon: <Phone size={20} />, label: 'Phone Number', value: userInfo.phone },
                { icon: <MapPin size={20} />, label: 'Address', value: userInfo.address },
              ].map((item, index) => (
                <div key={index} style={{ display: 'flex', alignItems: 'flex-start', gap: '1rem' }}>
                  <div style={{
                    width: '40px',
                    height: '40px',
                    borderRadius: '8px',
                    background: '#f3f4f6',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: '#6b7280'
                  }}>
                    {item.icon}
                  </div>
                  <div>
                    <p style={{ fontSize: '0.875rem', color: '#6b7280' }}>{item.label}</p>
                    <p style={{ fontWeight: 500 }}>{item.value}</p>
                  </div>
                </div>
              ))}
            </div>
          </motion.div>
        </div>
      </main>
    </div>
  );
};

export default Profile;
