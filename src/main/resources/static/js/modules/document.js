// HRM System - Document Module
// Corresponds to: Employee document management

function loadDocuments() {
    // This would fetch documents from API
    console.log('Loading documents...');
    // Mock data for now
    const mockDocuments = [
        { id: 1, name: 'Resume.pdf', type: 'Resume', employee: 'John Doe', uploadDate: '2026-01-15' },
        { id: 2, name: 'Contract.pdf', type: 'Contract', employee: 'Jane Smith', uploadDate: '2026-01-10' }
    ];
    renderDocumentsTable(mockDocuments);
}

function renderDocumentsTable(documents) {
    const tbody = document.getElementById('documentsList');
    if (!tbody) return;
    
    tbody.innerHTML = documents.map(doc => `
        <tr>
            <td>${doc.name}</td>
            <td>${doc.type}</td>
            <td>${doc.employee}</td>
            <td>${formatDate(doc.uploadDate)}</td>
            <td>
                <button onclick="downloadDocument(${doc.id})">Download</button>
                <button onclick="deleteDocument(${doc.id})">Delete</button>
            </td>
        </tr>
    `).join('');
}

function handleFileSelect(event) {
    const files = event.target.files;
    const selectedFilesDiv = document.getElementById('selectedFiles');
    if (!selectedFilesDiv) return;
    
    selectedFilesDiv.innerHTML = '';
    
    if (files.length === 0) {
        selectedFilesDiv.innerHTML = '<p class="no-files">No files selected</p>';
        return;
    }
    
    const fileList = document.createElement('div');
    fileList.className = 'file-list';
    
    Array.from(files).forEach((file, index) => {
        const fileItem = document.createElement('div');
        fileItem.className = 'file-item';
        
        const maxSize = 20 * 1024 * 1024;
        if (file.size > maxSize) {
            fileItem.className += ' error';
            fileItem.innerHTML = `
                <span class="file-name">${file.name}</span>
                <span class="file-error">File too large (>20MB)</span>
            `;
        } else {
            fileItem.innerHTML = `
                <span class="file-name">${file.name}</span>
                <span class="file-size">${formatFileSize(file.size)}</span>
            `;
        }
        
        fileList.appendChild(fileItem);
    });
    
    selectedFilesDiv.appendChild(fileList);
}

function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

async function uploadDocument(file, employeeId, documentType) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('employeeId', employeeId);
    formData.append('documentType', documentType);
    
    try {
        const response = await fetch('/api/documents/upload', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            },
            body: formData
        });
        
        if (response.ok) {
            showSuccess('Document uploaded successfully');
            loadDocuments();
        } else {
            alert('Failed to upload document');
        }
    } catch (error) {
        console.error('Upload document error:', error);
    }
}

function downloadDocument(docId) {
    showSuccess(`Downloading document ${docId}`);
    
    const link = document.createElement('a');
    link.href = `/api/documents/${docId}/download`;
    link.download = `document-${docId}.pdf`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

function deleteDocument(docId) {
    if (!confirm('Are you sure you want to delete this document?')) return;
    
    apiCall(`/api/documents/${docId}`, { method: 'DELETE' })
        .then(response => {
            if (response.ok) {
                showSuccess(`Document ${docId} deleted`);
                loadDocuments();
            }
        })
        .catch(error => console.error('Delete document error:', error));
}

function addDocumentToTable(name, type, employeeName, employeeEmail) {
    const tbody = document.getElementById('documentsList');
    if (!tbody) return;
    
    const row = tbody.insertRow(0);
    row.innerHTML = `
        <td>${name}</td>
        <td>${type}</td>
        <td>${employeeName}</td>
        <td>${new Date().toLocaleDateString()}</td>
        <td>
            <button onclick="downloadDocument(0)">Download</button>
            <button onclick="deleteDocument(0)">Delete</button>
        </td>
    `;
}

function processEmployeeDocuments(employeeData, files) {
    files.forEach(file => {
        const maxSize = 20 * 1024 * 1024;
        if (file.size <= maxSize) {
            addDocumentToTable(file.name, 'Other', 
                `${employeeData.firstName} ${employeeData.lastName}`, 
                employeeData.email);
        }
    });
}

function refreshDocuments() {
    showSuccess('Refreshing documents...');
    loadDocuments();
}

function searchDocuments() {
    const searchTerm = document.getElementById('documentSearch')?.value.toLowerCase() || '';
    const typeFilter = document.getElementById('documentTypeFilter')?.value || '';
    
    const rows = document.querySelectorAll('#documentsList tr');
    
    rows.forEach(row => {
        const text = row.textContent.toLowerCase();
        const matchesSearch = !searchTerm || text.includes(searchTerm);
        const matchesType = !typeFilter || text.includes(typeFilter.toLowerCase());
        
        row.style.display = matchesSearch && matchesType ? '' : 'none';
    });
}

// Exports
window.loadDocuments = loadDocuments;
window.handleFileSelect = handleFileSelect;
window.uploadDocument = uploadDocument;
window.downloadDocument = downloadDocument;
window.deleteDocument = deleteDocument;
window.addDocumentToTable = addDocumentToTable;
window.processEmployeeDocuments = processEmployeeDocuments;
window.refreshDocuments = refreshDocuments;
window.searchDocuments = searchDocuments;
