# Field Name Consistency Fixes

## Backend DTO (PayrollDTO.java) Field Names:
- `providentFund` (not `pf`)
- `tax` (not `incomeTax`)
- `insurance` (not `esi` or `otherDeduction`)
- `otherDeductions` (plural with 's')
- `totalDeductions` (plural with 's')

## Files Fixed:

### 1. payroll.js
- Fixed display fields: `p.providentFund`, `p.tax`, `p.insurance`
- Fixed form input names: `providentFund`, `tax`, `insurance`
- Fixed form data extraction
- Fixed payslip update data mapping

### 2. payslips.html
- Fixed edit form inputs: `providentFund`, `tax`, `otherDeductions`
- Fixed preview modal: `p.providentFund`, `p.tax`, `p.insurance`
- Fixed PDF template: `p.providentFund`, `p.tax`, `p.insurance`, `p.otherDeductions`

### 3. my-payslips.html
- Fixed deductions section: `p.providentFund`, `p.tax`, `p.insurance`

### 4. accountant-approvals.html
- Already correct: `p.providentFund`, `p.tax`, `p.insurance`

## Result:
All pages now show consistent data from backend DTO fields.
