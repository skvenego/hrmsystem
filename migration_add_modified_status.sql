-- Migration to add MODIFIED status to leaves table
-- Update status column to VARCHAR(30) to accommodate all leave statuses
-- Update comment to include MODIFIED status

USE hrmsystem;

-- Update the status column to be larger and update the comment
ALTER TABLE leaves 
MODIFY COLUMN status VARCHAR(30) DEFAULT 'PENDING' 
COMMENT 'PENDING, APPROVED, REJECTED, CANCELLED, MODIFIED';
