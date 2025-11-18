SET search_path TO app;

CREATE TABLE IF NOT EXISTS majors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    major_code VARCHAR(50) NOT NULL UNIQUE,
    major_name VARCHAR(150) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DROP TRIGGER IF EXISTS trg_majors_updated_at ON majors;
CREATE TRIGGER trg_majors_updated_at
BEFORE UPDATE ON majors
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();