import { motion } from 'framer-motion';
import { User, Bell, Shield, Palette, Globe, Save } from 'lucide-react';
import Sidebar from '../components/Sidebar';
import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';

const Settings = () => {
  const { user, setAuth } = useAuth();
  const [notifications, setNotifications] = useState({
    email: true,
    push: true,
    sms: false,
    leaveUpdates: true,
    payrollUpdates: true
  });
  const [employee, setEmployee] = useState(null);
  const [profileForm, setProfileForm] = useState({ name: '', email: '', phone: '' });
  const [isSavingProfile, setIsSavingProfile] = useState(false);

  const loadEmployee = async () => {
    try {
      const token = localStorage.getItem('token');
      let data = null;

      if (user?.employeeId) {
        const byIdRes = await fetch(`/api/employees/${user.employeeId}`, {
          headers: { Authorization: `Bearer ${token}` }
        });
        if (byIdRes.ok) {
          data = await byIdRes.json();
        }
      }

      if (!data && user?.email) {
        const allRes = await fetch('/api/employees', {
          headers: { Authorization: `Bearer ${token}` }
        });
        if (allRes.ok) {
          const list = await allRes.json();
          data = (Array.isArray(list) ? list : []).find(
            (e) => (e?.email || '').toLowerCase() === user.email.toLowerCase()
          );
        }
      }

      if (!data) throw new Error('Failed to load settings profile');
      setEmployee(data);
      setProfileForm({
        name: `${data.firstName || ''} ${data.lastName || ''}`.trim(),
        email: data.email || '',
        phone: data.phone || ''
      });
    } catch (e) {
      console.error(e);
    }
  };

  useEffect(() => {
    loadEmployee();
  }, [user?.employeeId, user?.email]);

  const saveProfile = async () => {
    if (!employee?.id) {
      alert('Profile is still loading. Please wait and try again.');
      return;
    }
    try {
      setIsSavingProfile(true);
      const token = localStorage.getItem('token');
      const parts = (profileForm.name || '').trim().split(/\s+/).filter(Boolean);
      const payload = {
        id: employee.id,
        employeeId: employee.employeeId,
        firstName: parts[0] || employee.firstName || 'User',
        lastName: parts.slice(1).join(' ') || employee.lastName || 'User',
        email: profileForm.email || employee.email,
        phone: profileForm.phone || '',
        designation: employee.designation,
        joiningDate: employee.joiningDate,
        salary: employee.salary,
        status: employee.status || 'ACTIVE',
        address: employee.address || '',
        departmentId: employee.departmentId,
        departmentName: employee.departmentName
      };

      const res = await fetch(`/api/employees/${employee.id}`, {
        method: 'PUT',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
      });
      if (!res.ok) throw new Error(await res.text());
      const updated = await res.json();
      setEmployee(updated);
      setProfileForm({
        name: `${updated.firstName || ''} ${updated.lastName || ''}`.trim(),
        email: updated.email || '',
        phone: updated.phone || ''
      });

      const savedUser = JSON.parse(localStorage.getItem('user') || '{}');
      const nextUser = {
        ...savedUser,
        id: savedUser.id,
        employeeId: updated.id,
        username: `${updated.firstName || ''} ${updated.lastName || ''}`.trim(),
        email: updated.email || savedUser.email
      };
      setAuth(token, nextUser);

      alert('Settings profile saved permanently.');
    } catch (e) {
      alert('Save failed: ' + e.message);
    } finally {
      setIsSavingProfile(false);
    }
  };

  const toggleNotification = (key) => {
    setNotifications(prev => ({ ...prev, [key]: !prev[key] }));
  };

  return (
    <div className="dashboard">
      <Sidebar />
      <main className="main-content">
        <div className="dashboard-header">
          <div>
            <h1 className="dashboard-title">Settings</h1>
            <p style={{ color: '#6b7280', marginTop: '0.25rem' }}>Manage your account preferences</p>
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {/* Profile Settings */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            style={{
              background: 'white',
              borderRadius: '12px',
              padding: '1.5rem',
              boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)'
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
              <User size={20} style={{ color: '#4f46e5' }} />
              <h3 style={{ fontSize: '1.125rem', fontWeight: 600 }}>Profile Settings</h3>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', color: '#6b7280', marginBottom: '0.5rem' }}>Full Name</label>
                <input type="text" value={profileForm.name} onChange={(e) => setProfileForm((p) => ({ ...p, name: e.target.value }))} style={{
                  width: '100%',
                  padding: '0.75rem',
                  border: '1px solid #e5e7eb',
                  borderRadius: '8px',
                  fontSize: '0.875rem'
                }} />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', color: '#6b7280', marginBottom: '0.5rem' }}>Email</label>
                <input type="email" value={profileForm.email} onChange={(e) => setProfileForm((p) => ({ ...p, email: e.target.value }))} style={{
                  width: '100%',
                  padding: '0.75rem',
                  border: '1px solid #e5e7eb',
                  borderRadius: '8px',
                  fontSize: '0.875rem'
                }} />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', color: '#6b7280', marginBottom: '0.5rem' }}>Phone</label>
                <input type="tel" value={profileForm.phone} onChange={(e) => setProfileForm((p) => ({ ...p, phone: e.target.value }))} style={{
                  width: '100%',
                  padding: '0.75rem',
                  border: '1px solid #e5e7eb',
                  borderRadius: '8px',
                  fontSize: '0.875rem'
                }} />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', color: '#6b7280', marginBottom: '0.5rem' }}>Language</label>
                <select style={{
                  width: '100%',
                  padding: '0.75rem',
                  border: '1px solid #e5e7eb',
                  borderRadius: '8px',
                  fontSize: '0.875rem',
                  background: 'white'
                }}>
                  <option>English</option>
                  <option>Spanish</option>
                  <option>French</option>
                </select>
              </div>
            </div>
          </motion.div>

          {/* Notification Settings */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            style={{
              background: 'white',
              borderRadius: '12px',
              padding: '1.5rem',
              boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)'
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
              <Bell size={20} style={{ color: '#4f46e5' }} />
              <h3 style={{ fontSize: '1.125rem', fontWeight: 600 }}>Notification Settings</h3>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {[
                { key: 'email', label: 'Email Notifications', desc: 'Receive updates via email' },
                { key: 'push', label: 'Push Notifications', desc: 'Receive push notifications' },
                { key: 'sms', label: 'SMS Notifications', desc: 'Receive updates via SMS' },
                { key: 'leaveUpdates', label: 'Leave Updates', desc: 'Get notified about leave requests' },
                { key: 'payrollUpdates', label: 'Payroll Updates', desc: 'Get notified about payroll' },
              ].map((item) => (
                <div key={item.key} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <p style={{ fontWeight: 500 }}>{item.label}</p>
                    <p style={{ fontSize: '0.875rem', color: '#6b7280' }}>{item.desc}</p>
                  </div>
                  <button
                    onClick={() => toggleNotification(item.key)}
                    style={{
                      width: '48px',
                      height: '24px',
                      borderRadius: '12px',
                      border: 'none',
                      background: notifications[item.key] ? '#4f46e5' : '#e5e7eb',
                      cursor: 'pointer',
                      position: 'relative',
                      transition: 'all 0.2s'
                    }}
                  >
                    <div style={{
                      width: '20px',
                      height: '20px',
                      borderRadius: '50%',
                      background: 'white',
                      position: 'absolute',
                      top: '2px',
                      left: notifications[item.key] ? '26px' : '2px',
                      transition: 'all 0.2s'
                    }} />
                  </button>
                </div>
              ))}
            </div>
          </motion.div>

          {/* Save Button */}
          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={saveProfile}
            disabled={isSavingProfile}
            style={{
              padding: '0.75rem 2rem',
              background: 'linear-gradient(135deg, #4f46e5, #06b6d4)',
              color: 'white',
              border: 'none',
              borderRadius: '8px',
              fontWeight: 600,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '0.5rem',
              width: 'fit-content',
              marginLeft: 'auto',
              opacity: isSavingProfile ? 0.7 : 1
            }}
          >
            <Save size={18} />
            {isSavingProfile ? 'Saving...' : 'Save Changes'}
          </motion.button>
        </div>
      </main>
    </div>
  );
};

export default Settings;
