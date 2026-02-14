drop table if exists boards, regular_schedules, schedules;

create table boards
(
  board_id INTEGER,
  board_name VARCHAR,
  staffs JSON,
  primary key (board_id)
);

create table regular_schedules (
id SERIAL,
board_id INTEGER,
week INTEGER,
day_of_week INTEGER,
works JSON,
primary key (board_id, week, day_of_week));

create table schedules (
id SERIAL,
board_id INTEGER,
week INTEGER,
day_of_week INTEGER,
schedule_date DATE,
works JSON,
staffs JSON,
primary key (board_id, schedule_date));
