SET search_path TO app;

CREATE TABLE IF NOT EXISTS enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    student_id UUID NOT NULL,
    course_id UUID NOT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'ENROLLED', -- ENROLLED / COMPLETED / DROPPED
    grade VARCHAR(10), -- Linh hoạt cho cả A / B / B+ hoặc 6.5 / 7.0
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_enrollments_student
        FOREIGN KEY (student_id) REFERENCES students(person_id),
    CONSTRAINT fk_enrollments_course
        FOREIGN KEY (course_id) REFERENCES courses(id),

    -- Business rule: 1 student - 1 course chỉ có 1 lần đăng ký (enrollment)
    CONSTRAINT uk_enrollments_student_course
        UNIQUE (student_id, course_id)
);

DROP TRIGGER IF EXISTS trg_enrollments_updated_at ON enrollments;
CREATE TRIGGER trg_enrollments_updated_at
BEFORE UPDATE ON enrollments
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();