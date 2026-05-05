-- Delete stale April 2026 payroll data to force regeneration
DELETE FROM payroll WHERE month = 4 AND year = 2026;

-- Verify deletion
SELECT 'April 2026 payroll records deleted. Now restart and regenerate payroll.' as message;
