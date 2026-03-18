import { useState, useEffect } from 'react'
import { FaPlus, FaEdit, FaTrash, FaBuilding } from 'react-icons/fa'
import axios from 'axios'
import { toast } from 'react-toastify'

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

  useEffect(() => {
    fetchDepartments()
  }, [])

  const fetchDepartments = async () => {
    try {
      const response = await axios.get('/api/departments')
      setDepartments(response.data)
    } catch (error) {
      toast.error('Failed to fetch departments')
    } finally {
      setIsLoading(false)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      if (formData.id) {
        await axios.put(`/api/departments/${formData.id}`, formData)
        toast.success('Department updated successfully')
      } else {
        await axios.post('/api/departments', formData)
        toast.success('Department created successfully')
      }
      setShowModal(false)
      setFormData({ id: null, name: '', description: '', headOfDepartment: '' })
      fetchDepartments()
    } catch (error) {
      toast.error('Operation failed')
    }
  }

  const handleEdit = (dept) => {
    setFormData(dept)
    setShowModal(true)
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure?')) return
    try {
      await axios.delete(`/api/departments/${id}`)
      toast.success('Department deleted')
      fetchDepartments()
    } catch (error) {
      toast.error('Delete failed')
    }
  }

  if (isLoading) {
    return (
      <div className="page-container">
        <div className="container">
          <div className="loading-state">
            <div className="spinner" />
            <p>Loading...</p>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="page-container">
      <div className="container">
        <div className="page-header">
          <div>
            <h1>Departments</h1>
            <p>Manage company departments</p>
          </div>
          <button 
            className="btn btn-primary"
            onClick={() => {
              setFormData({ id: null, name: '', description: '', headOfDepartment: '' })
              setShowModal(true)
            }}
          >
            <FaPlus /> Add Department
          </button>
        </div>

        <div className="grid grid-cols-3">
          {departments.map((dept) => (
            <div key={dept.id} className="card">
              <div className="flex items-center gap-3" style={{ marginBottom: '1rem' }}>
                <div 
                  className="stat-icon primary"
                  style={{ width: '48px', height: '48px', fontSize: '1.25rem' }}
                >
                  <FaBuilding />
                </div>
                <div>
                  <h3 style={{ fontSize: '1.125rem' }}>{dept.name}</h3>
                  <p style={{ fontSize: '0.875rem', color: 'var(--gray-500)' }}>
                    {dept.employeeCount || 0} Employees
                  </p>
                </div>
              </div>
              <p style={{ fontSize: '0.875rem', marginBottom: '1rem' }}>
                {dept.description || 'No description'}
              </p>
              <div className="flex justify-between items-center">
                <span style={{ fontSize: '0.875rem', color: 'var(--gray-500)' }}>
                  Head: {dept.headOfDepartment || 'N/A'}
                </span>
                <div className="flex gap-2">
                  <button 
                    className="action-btn edit"
                    onClick={() => handleEdit(dept)}
                  >
                    <FaEdit />
                  </button>
                  <button 
                    className="action-btn delete"
                    onClick={() => handleDelete(dept.id)}
                  >
                    <FaTrash />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>

        {showModal && (
          <div className="modal-overlay" onClick={() => setShowModal(false)}>
            <div className="modal" onClick={(e) => e.stopPropagation()}>
              <div className="modal-header">
                <h3>{formData.id ? 'Edit Department' : 'Add Department'}</h3>
                <button className="modal-close" onClick={() => setShowModal(false)}>
                  ×
                </button>
              </div>
              <form onSubmit={handleSubmit}>
                <div className="modal-body">
                  <div className="form-group">
                    <label>Name *</label>
                    <input
                      type="text"
                      value={formData.name}
                      onChange={(e) => setFormData({...formData, name: e.target.value})}
                      required
                      className="form-input"
                    />
                  </div>
                  <div className="form-group">
                    <label>Description</label>
                    <textarea
                      value={formData.description}
                      onChange={(e) => setFormData({...formData, description: e.target.value})}
                      className="form-input"
                      rows="3"
                    />
                  </div>
                  <div className="form-group">
                    <label>Head of Department</label>
                    <input
                      type="text"
                      value={formData.headOfDepartment}
                      onChange={(e) => setFormData({...formData, headOfDepartment: e.target.value})}
                      className="form-input"
                    />
                  </div>
                </div>
                <div className="modal-footer">
                  <button 
                    type="button" 
                    className="btn btn-secondary"
                    onClick={() => setShowModal(false)}
                  >
                    Cancel
                  </button>
                  <button type="submit" className="btn btn-primary">
                    {formData.id ? 'Update' : 'Create'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default Departments
