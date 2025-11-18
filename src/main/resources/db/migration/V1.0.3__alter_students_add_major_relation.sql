SET search_path TO app;

ALTER TABLE students
ADD COLUMN IF NOT EXISTS major_id UUID;

ALTER TABLE students
ADD CONSTRAINT fk_students_major
FOREIGN KEY (major_id)
REFERENCES majors(id);