SET search_path TO app;

INSERT INTO majors (major_code, major_name, description)
VALUES
    ('IT', 'Công nghệ Thông tin', 'Tập trung vào lập trình phần mềm, mạng máy tính, cơ sở dữ liệu, trí tuệ nhân tạo và phát triển web'),
    ('KTPM', 'Kỹ thuật Phần mềm', 'Chuyên sâu về quy trình phát triển phần mềm, thiết kế, kiểm thử, DevOps và bảo trì hệ thống'),
    ('AI', 'Trí tuệ Nhân tạo', 'Học máy, học sâu, thị giác máy tính, xử lý ngôn ngữ tự nhiên và khoa học dữ liệu'),
    ('QTKD', 'Quản trị Kinh doanh', 'Quản lý, marketing, kế toán, tài chính và vận hành doanh nghiệp'),
    ('KHDL', 'Khoa học Dữ liệu', 'Dữ liệu lớn, thống kê, mô hình dự đoán, phân tích kinh doanh và khai phá dữ liệu'),
    ('KTMT', 'Kỹ thuật Máy tính', 'Phần cứng, hệ thống nhúng, IoT, thiết kế điện tử và vi xử lý'),
    ('ANM', 'An ninh Mạng', 'Bảo mật hệ thống, điều tra số, an toàn thông tin, phòng chống tấn công mạng và quản trị an ninh')
ON CONFLICT DO NOTHING;
