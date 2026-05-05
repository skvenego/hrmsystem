-- Migration to update attendance status column to accommodate all status values
-- Update status column to VARCHAR(30) and update comment to include all statuses

USE hrmsystem;

-- Update the status column to be larger and update the comment
ALTER TABLE attendance 
MODIFY COLUMN status VARCHAR(30) DEFAULT 'PRESENT' 
COMMENT 'PRESENT, ABSENT, HALF_DAY, LATE, ON_LEAVE, PENDING, HOLIDAY';
