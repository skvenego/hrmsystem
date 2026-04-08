// Clear any cached form data and try fresh
localStorage.clear();
sessionStorage.clear();

// Also clear any form fields that might be stuck
document.getElementById('email').value = '';
document.getElementById('firstName').value = '';
document.getElementById('lastName').value = '';
