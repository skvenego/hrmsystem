import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { FaPlus, FaEdit, FaTrash, FaSearch } from 'react-icons/fa'
import axios from 'axios'
import { toast } from 'react-toastify'

const Employees = () => {
  const [employees, setEmployees] = useState([])
  const [searchTerm, setSearchTerm] = useState('')
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    fetchEmployees()
  }, [])

  const fetchEmployees = async () => {
    try {
      const response = await axios.get('/api/employees')
      setEmployees(response.data)
    } catch (error) {
      toast.error('Failed to fetch employees')
    } finally {
      setIsLoading(false)
    }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this employee?')) return
    
    try {
      await axios.delete(`/api/employees/${id}`)
      toast.success('Employee deleted successfully')
      fetchEmployees()
    } catch (error) {
      toast.error('Failed to delete employee')
    }
  }

  const filteredEmployees = employees.filter(emp =>
    emp.firstName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    emp.lastName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    emp.email?.toLowerCase().includes(searchTerm.toLowerCase())
  )

  if (isLoading) {
    return (
      <div className="page-container">
        <div className="container">
          <div className="loading-state">
            <div className="spinner" />
            <p>Loading employees...</p>
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
            <h1>Employees</h1>
            <p>Manage your workforce</p>
          </div>
          <Link to="/employees/new" className="btn btn-primary">
            <FaPlus />
            Add Employee
          </Link>
        </div>

        <div className="search-filter">
          <div className="search-box">
            <FaSearch className="search-icon" />
            <input
              type="text"
              placeholder="Search employees..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
        </div>

        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Department</th>
                <th>Designation</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredEmployees.length === 0 ? (
                <tr>
                  <td colSpan="6" className="text-center">
                    <div className="empty-state">
                      <p>No employees found</p>
                    </div>
                  </td>
                </tr>
              ) : (
                filteredEmployees.map((employee) => (
                  <tr key={employee.id}>
                    <td>
                      <strong>{employee.firstName} {employee.lastName}</strong>
                    </td>
                    <td>{employee.email}</td>
                    <td>{employee.departmentName || 'N/A'}</td>
                    <td>{employee.designation || 'N/A'}</td>
                    <td>
                      <span className={`badge badge-${employee.status?.toLowerCase() === 'active' ? 'success' : 'warning'}`}>
                        {employee.status || 'Active'}
                      </span>
                    </td>
                    <td>
                      <Link
                        to={`/employees/edit/${employee.id}`}
                        className="action-btn edit"
                      >
                        <FaEdit />
                      </Link>
                      <button
                        className="action-btn delete"
                        onClick={() => handleDelete(employee.id)}
                      >
                        <FaTrash />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

export default Employees
