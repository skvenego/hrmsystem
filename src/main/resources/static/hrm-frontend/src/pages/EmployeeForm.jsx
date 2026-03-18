import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { FaSave, FaArrowLeft } from 'react-icons/fa'
import axios from 'axios'
import { toast } from 'react-toastify'

const EmployeeForm = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const isEditing = Boolean(id)

  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    designation: '',
    departmentId: '',
    salary: '',
    joiningDate: '',
    status: 'ACTIVE',
    address: ''
  })

  const [departments, setDepartments] = useState([])
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    fetchDepartments()
    if (isEditing) {
      fetchEmployee()
    }
  }, [id])

  const fetchDepartments = async () => {
    try {
      const response = await axios.get('/api/departments')
      setDepartments(response.data)
    } catch (error) {
      toast.error('Failed to fetch departments')
    }
  }

  const fetchEmployee = async () => {
    try {
      const response = await axios.get(`/api/employees/${id}`)
      setFormData(response.data)
    } catch (error) {
      toast.error('Failed to fetch employee')
      navigate('/employees')
    }
  }

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setIsLoading(true)

    try {
      if (isEditing) {
        await axios.put(`/api/employees/${id}`, formData)
        toast.success('Employee updated successfully')
      } else {
        await axios.post('/api/employees', formData)
        toast.success('Employee created successfully')
      }
      navigate('/employees')
    } catch (error) {
      toast.error(error.response?.data?.message || 'Operation failed')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="page-container">
      <div className="container">
        <div className="page-header">
          <div>
            <button className="btn btn-secondary btn-sm" onClick={() => navigate('/employees')}>
              <FaArrowLeft /> Back
            </button>
            <h1 style={{ marginTop: '1rem' }}>
              {isEditing ? 'Edit Employee' : 'Add New Employee'}
            </h1>
          </div>
        </div>

        <div className="card" style={{ maxWidth: '800px' }}>
          <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-2">
              <div className="form-group">
                <label>First Name *</label>
                <input
                  type="text"
                  name="firstName"
                  value={formData.firstName}
                  onChange={handleChange}
                  required
                  className="form-input"
                />
              </div>
              <div className="form-group">
                <label>Last Name *</label>
                <input
                  type="text"
                  name="lastName"
                  value={formData.lastName}
                  onChange={handleChange}
                  required
                  className="form-input"
                />
              </div>
            </div>

            <div className="grid grid-cols-2">
              <div className="form-group">
                <label>Email *</label>
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleChange}
                  required
                  className="form-input"
                />
              </div>
              <div className="form-group">
                <label>Phone</label>
                <input
                  type="tel"
                  name="phone"
                  value={formData.phone}
                  onChange={handleChange}
                  className="form-input"
                />
              </div>
            </div>

            <div className="grid grid-cols-2">
              <div className="form-group">
                <label>Department</label>
                <select
                  name="departmentId"
                  value={formData.departmentId}
                  onChange={handleChange}
                  className="form-input"
                >
                  <option value="">Select Department</option>
                  {departments.map((dept) => (
                    <option key={dept.id} value={dept.id}>
                      {dept.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label>Designation</label>
                <input
                  type="text"
                  name="designation"
                  value={formData.designation}
                  onChange={handleChange}
                  className="form-input"
                />
              </div>
            </div>

            <div className="grid grid-cols-2">
              <div className="form-group">
                <label>Salary</label>
                <input
                  type="number"
                  name="salary"
                  value={formData.salary}
                  onChange={handleChange}
                  className="form-input"
                />
              </div>
              <div className="form-group">
                <label>Joining Date *</label>
                <input
                  type="date"
                  name="joiningDate"
                  value={formData.joiningDate}
                  onChange={handleChange}
                  required
                  className="form-input"
                />
              </div>
            </div>

            <div className="form-group">
              <label>Status</label>
              <select
                name="status"
                value={formData.status}
                onChange={handleChange}
                className="form-input"
              >
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
                <option value="ON_LEAVE">On Leave</option>
              </select>
            </div>

            <div className="form-group">
              <label>Address</label>
              <textarea
                name="address"
                value={formData.address}
                onChange={handleChange}
                rows="3"
                className="form-input"
              />
            </div>

            <div className="form-actions">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => navigate('/employees')}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={isLoading}
              >
                <FaSave />
                {isLoading ? 'Saving...' : (isEditing ? 'Update' : 'Save')}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}

export default EmployeeForm
