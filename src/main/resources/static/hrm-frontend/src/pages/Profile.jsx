import { motion } from 'framer-motion';
import { User, Mail, Phone, MapPin, Briefcase, Calendar, Edit2 } from 'lucide-react';
import Sidebar from '../components/Sidebar';
import { useAuth } from '../context/AuthContext';
import { useEffect, useState } from 'react';

const Profile = () => {
  const { user, setAuth } = useAuth();
  const [isEditing, setIsEditing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [employee, setEmployee] = useState(null);
  const [form, setForm] = useState({ name: '', email: '', phone: '', address: '' });

  const loadEmployee = async () => {
    try {
      const token = localStorage.getItem('token');
      if (!token) return;
      let data = null;

      // Prefer employeeId from login token payload, fallback to email lookup.
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

      if (data) {
        setEmployee(data);
        setForm({
          name: `${data.firstName || ''} ${data.lastName || ''}`.trim(),
          email: data.email || user?.email || '',
          phone: data.phone || '',
          address: data.address || ''
        });
      } else {
        // Graceful fallback: show available auth data without throwing error in console.
        setEmployee(null);
        setForm((prev) => ({
          ...prev,
          name: user?.username || prev.name || '',
          email: user?.email || prev.email || ''
        }));
      }
    } catch (e) {
      console.error('Profile load warning:', e?.message || e);
      setEmployee(null);
      setForm((prev) => ({
        ...prev,
        name: user?.username || prev.name || '',
        email: user?.email || prev.email || ''
      }));
    }
  };

  useEffect(() => {
    loadEmployee();
  }, [user?.employeeId, user?.email]);

  const handleSave = async () => {
    try {
      setIsSaving(true);
      const token = localStorage.getItem('token');
      if (!token) {
        throw new Error('Please login again');
      }

      let targetEmployee = employee;
      // If employee record is not yet loaded, resolve it by email before saving.
      if (!targetEmployee?.id) {
        const allRes = await fetch('/api/employees', {
          headers: { Authorization: `Bearer ${token}` }
        });
        if (!allRes.ok) throw new Error('Could not load employee record');
        const list = await allRes.json();
        const resolved = (Array.isArray(list) ? list : []).find(
          (e) => (e?.email || '').toLowerCase() === (form.email || user?.email || '').toLowerCase()
        );
        if (!resolved?.id) {
          throw new Error('Employee profile link not found. Please contact admin once.');
        }
        targetEmployee = resolved;
        setEmployee(resolved);
      }

      const parts = (form.name || '').trim().split(/\s+/).filter(Boolean);
      const firstName = parts[0] || targetEmployee.firstName || 'User';
      const lastName = parts.slice(1).join(' ') || targetEmployee.lastName || 'User';

      // Send full DTO so validation passes and old record is replaced with new values.
      const payload = {
        id: targetEmployee.id,
        employeeId: targetEmployee.employeeId,
        firstName,
        lastName,
        email: form.email || targetEmployee.email,
        phone: form.phone || '',
        designation: targetEmployee.designation,
        joiningDate: targetEmployee.joiningDate,
        salary: targetEmployee.salary,
        status: targetEmployee.status || 'ACTIVE',
        address: form.address || '',
        departmentId: targetEmployee.departmentId,
        departmentName: targetEmployee.departmentName
      };

      const res = await fetch(`/api/employees/${targetEmployee.id}`, {
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
      setForm({
        name: `${updated.firstName || ''} ${updated.lastName || ''}`.trim(),
        email: updated.email || '',
        phone: updated.phone || '',
        address: updated.address || ''
      });

      // Keep auth cache + context in sync for permanent display across pages
      const savedUser = JSON.parse(localStorage.getItem('user') || '{}');
      const nextUser = {
        ...savedUser,
        id: savedUser.id,
        employeeId: updated.id,
        username: `${updated.firstName || ''} ${updated.lastName || ''}`.trim(),
        email: updated.email || savedUser.email
      };
      setAuth(token, nextUser);

      setIsEditing(false);
      alert('Profile saved permanently.');
    } catch (e) {
      alert('Save failed: ' + e.message);
    } finally {
      setIsSaving(false);
    }
  };

  const userInfo = {
    name: form.name || user?.username || 'John Doe',
    email: form.email || user?.email || 'john@example.com',
    phone: form.phone || '-',
    address: form.address || '-',
    department: employee?.departmentName || 'Information Technology',
    position: employee?.designation || 'Employee',
    joinDate: employee?.joiningDate || '-',
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
              {!isEditing ? (
                <motion.button
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => setIsEditing(true)}
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
              ) : (
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <motion.button
                    whileHover={{ scale: 1.03 }}
                    whileTap={{ scale: 0.97 }}
                    onClick={() => {
                      setIsEditing(false);
                      setForm({
                        name: `${employee?.firstName || ''} ${employee?.lastName || ''}`.trim(),
                        email: employee?.email || '',
                        phone: employee?.phone || '',
                        address: employee?.address || ''
                      });
                    }}
                    disabled={isSaving}
                    style={{
                      padding: '0.5rem 0.9rem',
                      background: '#f3f4f6',
                      color: '#374151',
                      border: '1px solid #e5e7eb',
                      borderRadius: '6px',
                      cursor: isSaving ? 'not-allowed' : 'pointer'
                    }}
                  >
                    Cancel
                  </motion.button>
                  <motion.button
                    whileHover={{ scale: 1.03 }}
                    whileTap={{ scale: 0.97 }}
                    onClick={handleSave}
                    disabled={isSaving}
                    style={{
                      padding: '0.5rem 0.9rem',
                      background: '#4f46e5',
                      color: 'white',
                      border: 'none',
                      borderRadius: '6px',
                      cursor: isSaving ? 'not-allowed' : 'pointer',
                      opacity: isSaving ? 0.7 : 1
                    }}
                  >
                    {isSaving ? 'Saving...' : 'Save'}
                  </motion.button>
                </div>
              )}
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
                    {isEditing ? (
                      <input
                        className="form-input"
                        value={
                          item.label === 'Full Name'
                            ? form.name
                            : item.label === 'Email Address'
                            ? form.email
                            : item.label === 'Phone Number'
                            ? form.phone
                            : form.address
                        }
                        onChange={(e) => {
                          const v = e.target.value;
                          if (item.label === 'Full Name') setForm((p) => ({ ...p, name: v }));
                          if (item.label === 'Email Address') setForm((p) => ({ ...p, email: v }));
                          if (item.label === 'Phone Number') setForm((p) => ({ ...p, phone: v }));
                          if (item.label === 'Address') setForm((p) => ({ ...p, address: v }));
                        }}
                        style={{ padding: '0.4rem 0.5rem', minWidth: '220px' }}
                      />
                    ) : (
                      <p style={{ fontWeight: 500 }}>{item.value}</p>
                    )}
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
