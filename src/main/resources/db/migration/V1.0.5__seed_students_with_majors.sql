SET search_path TO app;

-- USERS
INSERT INTO users (username, email, password_hash, status)
VALUES
('truong.tuan', 'tuan.truong@example.com', 'hashed_pw_s1', 'ACTIVE'),
('pham.hoang', 'hoang.pham@example.com', 'hashed_pw_s2', 'ACTIVE'),
('do.trang', 'trang.do@example.com', 'hashed_pw_s3', 'ACTIVE'),
('nguyen.linh', 'linh.nguyen@example.com', 'hashed_pw_s4', 'ACTIVE'),
('le.hai', 'hai.le@example.com', 'hashed_pw_s5', 'ACTIVE'),
('pham.nhi', 'nhi.pham@example.com', 'hashed_pw_s6', 'ACTIVE'),
('bui.dang', 'dang.bui@example.com', 'hashed_pw_s7', 'ACTIVE'),
('ho.thai', 'thai.ho@example.com', 'hashed_pw_s8', 'ACTIVE'),
('ngo.khanh', 'khanh.ngo@example.com', 'hashed_pw_s9', 'ACTIVE'),
('vo.huyen', 'huyen.vo@example.com', 'hashed_pw_s10', 'ACTIVE'),
('quach.nam', 'nam.quach@example.com', 'hashed_pw_s11', 'ACTIVE'),
('tran.dung', 'dung.tran@example.com', 'hashed_pw_s12', 'ACTIVE'),
('pham.quynh', 'quynh.pham@example.com', 'hashed_pw_s13', 'ACTIVE'),
('dao.khoa', 'khoa.dao@example.com', 'hashed_pw_s14', 'ACTIVE'),
('le.vy', 'vy.le@example.com', 'hashed_pw_s15', 'ACTIVE'),
('ngo.minh', 'minh.ngo@example.com', 'hashed_pw_s16', 'ACTIVE'),
('hoang.yen', 'yen.hoang@example.com', 'hashed_pw_s17', 'ACTIVE'),
('phan.son', 'son.phan@example.com', 'hashed_pw_s18', 'ACTIVE'),
('vu.thao', 'thao.vu@example.com', 'hashed_pw_s19', 'ACTIVE'),
('tran.hoa', 'hoa.tran@example.com', 'hashed_pw_s20', 'ACTIVE');

-- PEOPLE (lấy user_id dựa trên email)
INSERT INTO people (full_name, dob, phone, contact_email, address, user_id)
SELECT ps.full_name, ps.dob, ps.phone, ps.contact_email, ps.address, u.id
FROM (
    VALUES
    ('Truong Tuan', DATE '2004-02-14', '0916001001', 'tuan.truong@example.com', 'Hai Chau'),
    ('Pham Hoang', DATE '2005-11-21', '0916001002', 'hoang.pham@example.com', 'Thanh Khe'),
    ('Do Trang', DATE '2003-04-03', '0916001003', 'trang.do@example.com', 'Cam Le'),
    ('Nguyen Linh', DATE '2004-12-10', '0916001004', 'linh.nguyen@example.com', 'Lien Chieu'),
    ('Le Hai', DATE '2006-05-25', '0916001005', 'hai.le@example.com', 'Hai Chau'),
    ('Pham Nhi', DATE '2005-01-18', '0916001006', 'nhi.pham@example.com', 'Hai Chau'),
    ('Bui Dang', DATE '2004-09-09', '0916001007', 'dang.bui@example.com', 'Son Tra'),
    ('Ho Thai', DATE '2003-10-20', '0916001008', 'thai.ho@example.com', 'Hai Chau'),
    ('Ngo Khanh', DATE '2004-07-27', '0916001009', 'khanh.ngo@example.com', 'Thanh Khe'),
    ('Vo Huyen', DATE '2005-06-15', '0916001010', 'huyen.vo@example.com', 'Lien Chieu'),
    ('Quach Nam', DATE '2004-01-07', '0916001011', 'nam.quach@example.com', 'Hai Chau'),
    ('Tran Dung', DATE '2005-03-29', '0916001012', 'dung.tran@example.com', 'Thanh Khe'),
    ('Pham Quynh', DATE '2005-08-11', '0916001013', 'quynh.pham@example.com', 'Cam Le'),
    ('Dao Khoa', DATE '2004-11-01', '0916001014', 'khoa.dao@example.com', 'Lien Chieu'),
    ('Le Vy', DATE '2006-02-23', '0916001015', 'vy.le@example.com', 'Hai Chau'),
    ('Ngo Minh', DATE '2005-09-30', '0916001016', 'minh.ngo@example.com', 'Lien Chieu'),
    ('Hoang Yen', DATE '2004-10-08', '0916001017', 'yen.hoang@example.com', 'Hai Chau'),
    ('Phan Son', DATE '2003-07-26', '0916001018', 'son.phan@example.com', 'Son Tra'),
    ('Vu Thao', DATE '2005-05-12', '0916001019', 'thao.vu@example.com', 'Hai Chau'),
    ('Tran Hoa', DATE '2004-03-19', '0916001020', 'hoa.tran@example.com', 'Cam Le')
) AS ps(full_name, dob, phone, contact_email, address)
JOIN users u ON u.email = ps.contact_email;

-- STUDENTS & gán major_id
WITH ordered_people AS (
    SELECT p.id, u.email,
           ROW_NUMBER() OVER (ORDER BY p.contact_email) AS rn
    FROM people p
    JOIN users u ON u.id = p.user_id
    WHERE u.email LIKE '%@example.com'
      AND u.email NOT IN (
          'lan.phan@example.com',
          'hoa.bui@example.com',
          'chau.nguyen@example.com',
          'an.nguyen@example.com',
          'bich.tran@example.com',
          'cuong.le@example.com',
          'diep.pham@example.com',
          'em.vo@example.com',
          'gia.do@example.com',
          'kien.hoang@example.com'
      )
    ORDER BY p.contact_email
    LIMIT 20
),
ordered_majors AS (
    SELECT id,
           ROW_NUMBER() OVER (ORDER BY major_code) AS rn
    FROM majors
    WHERE major_code IN ('IT', 'KTPM', 'AI', 'QTKD', 'KHDL', 'KTMT', 'ANM')
)
INSERT INTO students (person_id, student_code, enrollment_year, major_id)
SELECT
    op.id,
    CONCAT('STU', LPAD((op.rn + 7)::TEXT, 3, '0')), -- STU008+
    FLOOR(RANDOM() * 3) + 2022,
    om.id
FROM ordered_people op
JOIN ordered_majors om
  ON ((op.rn - 1) % (SELECT COUNT(*) FROM ordered_majors)) + 1 = om.rn;
