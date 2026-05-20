-- Test May 2026 calculation for Anubhav (Employee 16)
-- Expected: 7.5 earned (5 months × 1.5)

-- Check employee joining date
SELECT id, first_name, last_name, joining_date, probation_period_months 
FROM employees 
WHERE id = 16;

-- Check leaves for Anubhav
SELECT id, start_date, end_date, total_days, paid_days, unpaid_days, status
FROM leaves 
WHERE employee_id = 16 AND status = 'APPROVED'
ORDER BY start_date;

-- Manual calculation:
-- Joined: Oct 5, 2025
-- Probation: 3 months (ended Jan 5, 2026)
-- May 2026: 5 months since probation ended
-- Earned: 5 × 1.5 = 7.5

-- Used Leaves by end of May 2026:
-- Leave 4: Apr 26-27 (2 paid) - ends before May 31
-- Leave 5: Apr 29-May 2 (3 paid, 1 unpaid) - ends May 2, counts
-- Leave 6: May 4-6 (0.5 paid, 2.5 unpaid) - ends May 6, counts
-- Total Used: 5.5

-- Remaining: 7.5 - 5.5 = 2.0

-- May portion of Leave 5 (Apr 29-May 2):
-- May 1: Paid (day 3 of leave)
-- May 2: Unpaid (day 4 of leave)
-- May Paid: 1.0, May Unpaid: 1.0

-- May Leave 6 (May 4-6):
-- May 4: Paid (day 1)
-- May 5: Unpaid (day 2)
-- May 6: Unpaid (day 3)
-- May Paid: 0.5, May Unpaid: 2.5

-- Total May Paid: 1.0 + 0.5 = 1.5
-- Total May Unpaid: 1.0 + 2.5 = 3.5
