-- Script nay duoc chay MOT LAN ngay sau khi PostgreSQL container khoi dong,
-- TRUOC KHI Hibernate tao bang. Can thiet vi entity Person co:
--   @Table(name = "people", schema = "app")
-- -> Schema "app" phai ton tai truoc khi Hibernate chay ddl-auto=create-drop.
CREATE SCHEMA IF NOT EXISTS app;
