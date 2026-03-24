import { useState, useEffect } from 'react'
import { Plus, Pencil, Trash2, Building2 } from 'lucide-react'
import { motion } from 'framer-motion'
import Sidebar from '../components/Sidebar'

const Departments = () => {
  const [departments, setDepartments] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [showModal, setShowModal] = useState(false)
  const [formData, setFormData] = useState({
    id: null,
    name: '',
    description: '',
    headOfDepartment: ''
  })

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(amount || 0)
  }

  const getAuthHeaders = () => {
    const token = localStorage.getItem('token')
    return {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  }

  useEffect(() => {
    fetchDepartments()
  }, [])

  const fetchDepartments = async () => {
    try {
      const response = await fetch('/api/departments', {
        headers: getAuthHeaders()
      })
      if (response.ok) {
        const data = await response.json()
        setDepartments(data)
      } else {
        alert('Failed to fetch departments')
      }
    } catch (error) {
      console.error('Error fetching departments:', error)
      alert('Error fetching departments')
    } finally {
      setIsLoading(false)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      const url = formData.id ? `/api/departments/${formData.id}` : '/api/departments'
      const method = formData.id ? 'PUT' : 'POST'
      
      const response = await fetch(url, {
        method: method,
        headers: getAuthHeaders(),
        body: JSON.stringify(formData)
      })

      if (response.ok) {
        alert(formData.id ? 'Department updated successfully' : 'Department created successfully')
        setShowModal(false)
        setFormData({ id: null, name: '', description: '', headOfDepartment: '' })
        fetchDepartments()
      } else {
        const error = await response.text()
        alert('Error: ' + error)
      }
    } catch (error) {
      console.error('Error saving department:', error)
      alert('Operation failed: ' + error.message)
    }
  }

  const handleEdit = (dept) => {
    setFormData({
      id: dept.id,
      name: dept.name || '',
      description: dept.description || '',
      headOfDepartment: dept.headOfDepartment || ''
    })
    setShowModal(true)
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this department?')) return
    try {
      const response = await fetch(`/api/departments/${id}`, {
        method: 'DELETE',
        headers: getAuthHeaders()
      })

      if (response.ok) {
        alert('Department deleted successfully')
        fetchDepartments()
      } else {
        const error = await response.text()
        alert('Error: ' + error)
      }
    } catch (error) {
      console.error('Error deleting department:', error)
      alert('Delete failed: ' + error.message)
    }
  }

  if (isLoading) {
    return (
      <div className="dashboard">
        <Sidebar />
        <main className="main-content">
          <div style={{ textAlign: 'center', padding: '2rem', color: '#6b7280' }}>
            Loading departments...
          </div>
        </main>
      </div>
    )
  }

  return (
    <div className="dashboard">
      <Sidebar />
      <main className="main-content">
        <div className="dashboard-header">
          <div>
            <h1 className="dashboard-title">Departments</h1>
            <p style={{ color: '#6b7280', marginTop: '0.25rem' }}>Manage company departments</p>
          </div>
          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={() => {
              setFormData({ id: null, name: '', description: '', headOfDepartment: '' })
              setShowModal(true)
            }}
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
            <Plus size={16} />
            Add Department
          </motion.button>
        </div>

        {departments.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '2rem', color: '#6b7280', background: 'white', borderRadius: '12px' }}>
            <Building2 size={48} style={{ marginBottom: '1rem', opacity: 0.5 }} />
            <p>No departments found.</p>
            <p>Click "Add Department" to create your first department.</p>
          </div>
        ) : (
          <motion.div 
            style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '1.5rem' }}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
          >
            {departments.map((dept, index) => (
              <motion.div 
                key={dept.id}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.1 }}
                style={{
                  background: 'white',
                  borderRadius: '12px',
                  padding: '1.5rem',
                  boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1rem' }}>
                  <div style={{
                    width: '48px',
                    height: '48px',
                    borderRadius: '12px',
                    background: 'linear-gradient(135deg, #4f46e5, #06b6d4)',
                    color: 'white',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '1.25rem'
                  }}>
                    <Building2 />
                  </div>
                  <div>
                    <h3 style={{ fontSize: '1.125rem', fontWeight: 600 }}>{dept.name}</h3>
                    <p style={{ fontSize: '0.875rem', color: '#6b7280' }}>
                      {dept.employeeCount || 0} Employees
                    </p>
                  </div>
                </div>
                <p style={{ fontSize: '0.875rem', marginBottom: '1rem', color: '#4b5563' }}>
                  {dept.description || 'No description'}
                </p>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontSize: '0.875rem', color: '#6b7280' }}>
                    Head: {dept.headOfDepartment || 'N/A'}
                  </span>
                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <motion.button 
                      whileHover={{ scale: 1.1 }}
                      whileTap={{ scale: 0.9 }}
                      onClick={() => handleEdit(dept)}
                      style={{
                        padding: '0.5rem',
                        background: '#f3f4f6',
                        border: 'none',
                        borderRadius: '6px',
                        cursor: 'pointer',
                        color: '#f59e0b'
                      }}
                      title="Edit"
                    >
                      <Pencil size={16} />
                    </motion.button>
                    <motion.button 
                      whileHover={{ scale: 1.1 }}
                      whileTap={{ scale: 0.9 }}
                      onClick={() => handleDelete(dept.id)}
                      style={{
                        padding: '0.5rem',
                        background: '#f3f4f6',
                        border: 'none',
                        borderRadius: '6px',
                        cursor: 'pointer',
                        color: '#ef4444'
                      }}
                      title="Delete"
                    >
                      <Trash2 size={16} />
                    </motion.button>
                  </div>
                </div>
              </motion.div>
            ))}
          </motion.div>
        )}

        {showModal && (
          <div style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: 'rgba(0, 0, 0, 0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000
          }}>
            <motion.div 
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              style={{
                background: 'white',
                borderRadius: '12px',
                padding: '2rem',
                width: '450px',
                maxHeight: '90vh',
                overflow: 'auto'
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                <h3 style={{ fontSize: '1.25rem', fontWeight: 600 }}>
                  {formData.id ? 'Edit Department' : 'Add Department'}
                </h3>
                <button 
                  onClick={() => setShowModal(false)}
                  style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '1.5rem', color: '#6b7280' }}
                >
                  ×
                </button>
              </div>
              <form onSubmit={handleSubmit}>
                <div style={{ marginBottom: '1rem' }}>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>Name *</label>
                  <input
                    type="text"
                    value={formData.name}
                    onChange={(e) => setFormData({...formData, name: e.target.value})}
                    required
                    style={{
                      width: '100%',
                      padding: '0.75rem',
                      border: '1px solid #e5e7eb',
                      borderRadius: '8px',
                      fontSize: '0.875rem'
                    }}
                  />
                </div>
                <div style={{ marginBottom: '1rem' }}>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>Description</label>
                  <textarea
                    value={formData.description}
                    onChange={(e) => setFormData({...formData, description: e.target.value})}
                    rows="3"
                    style={{
                      width: '100%',
                      padding: '0.75rem',
                      border: '1px solid #e5e7eb',
                      borderRadius: '8px',
                      fontSize: '0.875rem',
                      resize: 'vertical'
                    }}
                  />
                </div>
                <div style={{ marginBottom: '1.5rem' }}>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>Head of Department</label>
                  <input
                    type="text"
                    value={formData.headOfDepartment}
                    onChange={(e) => setFormData({...formData, headOfDepartment: e.target.value})}
                    style={{
                      width: '100%',
                      padding: '0.75rem',
                      border: '1px solid #e5e7eb',
                      borderRadius: '8px',
                      fontSize: '0.875rem'
                    }}
                  />
                </div>
                <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end' }}>
                  <button 
                    type="button"
                    onClick={() => setShowModal(false)}
                    style={{
                      padding: '0.75rem 1.5rem',
                      border: '1px solid #e5e7eb',
                      borderRadius: '8px',
                      background: 'white',
                      cursor: 'pointer',
                      fontWeight: 500
                    }}
                  >
                    Cancel
                  </button>
                  <button 
                    type="submit"
                    style={{
                      padding: '0.75rem 1.5rem',
                      background: 'linear-gradient(135deg, #4f46e5, #06b6d4)',
                      color: 'white',
                      border: 'none',
                      borderRadius: '8px',
                      cursor: 'pointer',
                      fontWeight: 500
                    }}
                  >
                    {formData.id ? 'Update' : 'Create'}
                  </button>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </main>
    </div>
  )
}

export default Departments
