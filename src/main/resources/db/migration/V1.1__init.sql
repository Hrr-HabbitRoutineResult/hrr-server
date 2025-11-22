-- --------------------------------------------------------
-- 테이블 생성 (CREATE TABLE)
-- --------------------------------------------------------

create table badge (created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), primary key (id)) engine=InnoDB;
create table badge_condition (created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), primary key (id)) engine=InnoDB;
create table challenge (current_participants integer not null, is_public bit not null, is_viewer_mode bit not null, like_count integer, max_participants integer not null, verify_end_time time(6) not null, verify_start_time time(6) not null, created_at datetime(6) not null, id bigint not null auto_increment, start_date datetime(6) not null, updated_at datetime(6), description varchar(255) not null, image_url varchar(255), password varchar(255), title varchar(255) not null, category enum ('ALL','CAREER','HABIT','HEALTH','HOBBY','STUDY') not null, rule TEXT, status enum ('FINISHED','ONGOING','UPCOMING') not null, verification_method enum ('PHOTO','TEXT') not null, primary key (id)) engine=InnoDB;
create table challenge_day_join (challenge_id bigint not null, created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), day_of_week enum ('FRIDAY','MONDAY','SATURDAY','SUNDAY','THURSDAY','TUESDAY','WEDNESDAY') not null, primary key (id)) engine=InnoDB;
create table challenge_embedding (challenge_id bigint not null, created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), challenge_embedding tinyblob, challenge_text tinytext not null, primary key (id)) engine=InnoDB;
create table challenge_keyword (created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), primary key (id)) engine=InnoDB;
create table challenge_like (created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), primary key (id)) engine=InnoDB;
create table challenge_wait (created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), primary key (id)) engine=InnoDB;
create table comment (created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), primary key (id)) engine=InnoDB;
create table comment_like (created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), primary key (id)) engine=InnoDB;
create table comment_report (created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), primary key (id)) engine=InnoDB;
create table dm_conversation (created_at datetime(6) not null, id bigint not null auto_increment, last_message_id bigint, updated_at datetime(6), user1_id bigint not null, user2_id bigint not null, primary key (id)) engine=InnoDB;
create table dm_conversation_participant (is_blocked bit not null, is_muted bit not null, conversation_id bigint not null, created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), user_id bigint not null, primary key (id)) engine=InnoDB;
create table dm_message (conversation_id bigint not null, created_at datetime(6) not null, id bigint not null auto_increment, sender_id bigint not null, updated_at datetime(6), client_message_uuid varchar(64), content tinytext, delivery_status enum ('DELIVERED','FAILED','SENT') not null, message_type enum ('IMAGE','LINK','TEXT') not null, primary key (id)) engine=InnoDB;
create table dm_message_image (height integer, width integer, created_at datetime(6) not null, filesize_bytes bigint, id bigint not null auto_increment, message_id bigint not null, updated_at datetime(6), mimetype varchar(100), origin_filename varchar(255), s3_key varchar(255) not null, primary key (id)) engine=InnoDB;
create table dm_message_link (created_at datetime(6) not null, id bigint not null auto_increment, message_id bigint not null, updated_at datetime(6), thumbnail_url varchar(512), url varchar(2048) not null, title varchar(255), primary key (id)) engine=InnoDB;
create table dm_read (conversation_id bigint not null, created_at datetime(6) not null, id bigint not null auto_increment, last_read_message_id bigint, read_at datetime(6), updated_at datetime(6), user_id bigint not null, primary key (id)) engine=InnoDB;
create table dm_report (conversation_id bigint, created_at datetime(6) not null, id bigint not null auto_increment, message_id bigint, reported_user_id bigint not null, reporter_id bigint not null, updated_at datetime(6), custom_reason varchar(200), admin_comment varchar(500), reason enum ('ABUSIVE_LANGUAGE','ILLEGAL_CONTENT_SHARE','OTHER','PERSONAL_INFO_REQUEST','SEXUAL_OR_OBSCENE','SPAM_OR_SCAM') not null, status enum ('IN_REVIEW','PENDING','REJECTED','RESOLVED') not null, primary key (id)) engine=InnoDB;
create table fcm_token (is_active bit not null, created_at datetime(6) not null, id bigint not null auto_increment, registered_at datetime(6) not null, updated_at datetime(6), user_id bigint not null, token varchar(512) not null, primary key (id)) engine=InnoDB;
create table follow (created_at datetime(6) not null, follower_id bigint not null, following_id bigint not null, id bigint not null auto_increment, updated_at datetime(6), primary key (id)) engine=InnoDB;
create table keyword (created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), primary key (id)) engine=InnoDB;
create table kickout (created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), primary key (id)) engine=InnoDB;
create table notification_delivery (is_read bit not null, created_at datetime(6) not null, event_id bigint not null, id bigint not null auto_increment, read_at datetime(6), receiver_id bigint not null, updated_at datetime(6), primary key (id)) engine=InnoDB;
create table notification_event (actor_id bigint, context_id bigint, created_at datetime(6) not null, id bigint not null auto_increment, target_id bigint not null, type_id bigint not null, updated_at datetime(6), title varchar(100) not null, message varchar(255) not null, category enum ('BADGE','CHALLENGE','FOLLOW','VERIFICATION') not null, context_type enum ('BADGE','CHALLENGE','COMMENT','USER','VERIFICATION'), target_type enum ('BADGE','CHALLENGE','COMMENT','USER','VERIFICATION') not null, primary key (id)) engine=InnoDB;
create table notification_setting (settings bit not null, created_at datetime(6) not null, id bigint not null auto_increment, type_id bigint not null, updated_at datetime(6), user_id bigint not null, primary key (id)) engine=InnoDB;
create table notification_type (default_enabled bit not null, is_mandatory bit not null, created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), type_name varchar(50) not null, primary key (id)) engine=InnoDB;
create table random_mission (created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), title varchar(15) not null, content varchar(30) not null, category enum ('ALL','CAREER','HABIT','HEALTH','HOBBY','STUDY') not null, primary key (id)) engine=InnoDB;
create table recommendation_result (cosine_score float(23), ranking integer not null, challenge_id bigint not null, created_at datetime(6) not null, favor_id bigint not null, id bigint not null auto_increment, updated_at datetime(6), primary key (id)) engine=InnoDB;
create table `round` (created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), primary key (id)) engine=InnoDB;
create table scrap (created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), primary key (id)) engine=InnoDB;
create table term (is_required bit not null, created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), title varchar(100) not null, description TEXT not null, primary key (id)) engine=InnoDB;
create table user (is_public bit not null, created_at datetime(6) not null, deleted_at datetime(6), follower_count bigint not null, following_count bigint not null, id bigint not null auto_increment, kakao_id bigint, points bigint not null, updated_at datetime(6), phone_number varchar(15), nickname varchar(20) not null, password varchar(225), profile_image varchar(225), email varchar(255) , level enum ('BRONZE','CHALLENGER','GOLD','MASTER','SILVER') not null, login_status enum ('EXISTING','NEW') not null, role enum ('ADMIN','USER'), status enum ('ACTIVE','DELETED','INACTIVE','SUSPENDED'), primary key (id)) engine=InnoDB;
create table user_block (blocked_id bigint not null, blocker_id bigint not null, created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), primary key (id)) engine=InnoDB;
create table user_challenge (verification_count integer not null, verification_uncount integer not null, warn_count integer not null, challenge_id bigint not null, created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), user_id bigint not null, role enum ('CHALLENGER','OWNER') default 'CHALLENGER' not null, verification_status enum ('FINISHED','PENDING') not null, primary key (id)) engine=InnoDB;
create table user_favor (created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), user_id bigint not null, age_group enum ('FIFTIES_PLUS','FORTIES','TEENS','THIRTIES','TWENTIES') not null, gender enum ('FEMALE','MALE') not null, goal enum ('BUILD_EXERCISE_HABIT','ENJOY_HOBBY_TOGETHER','EXAM_CAREER_PREP','FIND_NEW_HOBBY','FOCUS_ON_MYSELF','HEALTHY_DAY','KEEP_GOING') not null, job enum ('EMPLOYEE','ETC','HOMEMAKER','JOB_SEEKER','STUDENT_MIDDLE_HIGH','STUDENT_UNIVERSITY') not null, primary key (id)) engine=InnoDB;
create table user_favor_available_time (user_favor_id bigint not null, available_time enum ('AFTERNOON','EARLY_MORNING','EVENING','LATE_NIGHT','LUNCH','MORNING','NIGHT') not null, primary key (user_favor_id, available_time)) engine=InnoDB;
create table user_favor_category (user_favor_id bigint not null, category enum ('ALL','CAREER','HABIT','HEALTH','HOBBY','STUDY') not null, primary key (user_favor_id, category)) engine=InnoDB;
create table user_favor_embedding (created_at datetime(6) not null, favor_id bigint not null, id bigint not null auto_increment, updated_at datetime(6), favor_embedding tinyblob, favor_text tinytext not null, primary key (id)) engine=InnoDB;
create table user_mission (date date not null, is_completed bit not null, id bigint not null auto_increment, mission_id bigint, user_id bigint not null, primary key (id)) engine=InnoDB;
create table user_report (created_at datetime(6) not null, id bigint not null auto_increment, reported_id bigint not null, reporter_id bigint not null, updated_at datetime(6), reason enum ('ABUSIVE_LANGUAGE','ILLEGAL_CONTENT_SHARE','OTHER','PERSONAL_INFO_REQUEST','SEXUAL_OR_OBSCENE','SPAM_OR_SCAM') not null, reason_text TEXT, status enum ('IN_REVIEW','PENDING','REJECTED','RESOLVED') not null, primary key (id)) engine=InnoDB;
create table user_term (is_agreed bit not null, agreed_at datetime(6) not null, id bigint not null auto_increment, term_id bigint not null, user_id bigint not null, primary key (id)) engine=InnoDB;
create table user_badge (created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), primary key (id)) engine=InnoDB;
create table user_badge_condition (created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), primary key (id)) engine=InnoDB;
create table verification (created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), primary key (id)) engine=InnoDB;
create table verification_like (created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), primary key (id)) engine=InnoDB;
create table verification_report (created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6), primary key (id)) engine=InnoDB;

-- --------------------------------------------------------
-- 유니크 제약 조건 및 인덱스 (Unique Constraints and Indices)
-- --------------------------------------------------------

alter table challenge_embedding add constraint UK5m3qn5ujci83yaw7eub8jsh67 unique (challenge_id);
alter table dm_conversation add constraint UKgyekbj306oiuguqrlsov72s7g unique (user1_id, user2_id);
create index ix_dm_part_conv_user on dm_conversation_participant (conversation_id, user_id);
alter table dm_conversation_participant add constraint UKihadt389yxiypdv7n03hl904o unique (conversation_id, user_id);
create index ix_dm_msg_conv_id on dm_message (conversation_id, id);
alter table dm_message add constraint UK354wytg1iqmyy6oiff39ma53m unique (client_message_uuid);
alter table dm_read add constraint UK6qkc48fb9gecn3e8t9rew1lx9 unique (conversation_id, user_id);
alter table fcm_token add constraint UK79w68xjk1osmdqgq1t2vwn7ut unique (user_id, token);
create index idx_notification_setting_user on notification_setting (user_id);
create index idx_notification_setting_type on notification_setting (type_id);
alter table notification_setting add constraint UKev1a7kujo9j1v9ovoan454vv0 unique (user_id, type_id);
alter table notification_type add constraint UKn05ghx0xbdma61jdn6o9s76oi unique (type_name);
alter table user add constraint UK4tp32nb01jmfcirpipti37lfs unique (kakao_id);
alter table user_favor_embedding add constraint UKek9xfdt74om8mi0q50o431c8s unique (favor_id);
alter table user_mission add constraint UK_user_mission_date unique (user_id, date);

-- --------------------------------------------------------
-- 외래 키 제약 조건 (Foreign Key Constraints)
-- --------------------------------------------------------

alter table challenge_day_join add constraint FK1ml8mmjre0edh5lkvkrvx48r2 foreign key (challenge_id) references challenge (id);
alter table challenge_embedding add constraint FKbqxq22b94k2gk2bof5qopub4y foreign key (challenge_id) references challenge (id);
alter table dm_conversation add constraint FKga5ith0fvg6v39xk25pkya64c foreign key (last_message_id) references dm_message (id);
alter table dm_conversation add constraint FK4g112ys8p80e96r8bppdnj867 foreign key (user1_id) references user (id);
alter table dm_conversation add constraint FK5otdqtnp2wipaqx97lk8atqmo foreign key (user2_id) references user (id);
alter table dm_conversation_participant add constraint FKnmvuwd0h36wytfkukqs61jyxc foreign key (conversation_id) references dm_conversation (id);
alter table dm_conversation_participant add constraint FK3dmb1n3vpjkkxp48xa6mjyxcr foreign key (user_id) references user (id);
alter table dm_message add constraint FK7dknb967ph7ub66g50d2i5vs5 foreign key (conversation_id) references dm_conversation (id);
alter table dm_message add constraint FK66arptdbp0jgvssl7naq9q4fo foreign key (sender_id) references user (id);
alter table dm_message_image add constraint FKdklffivj131x9e9pwlx7f2ubn foreign key (message_id) references dm_message (id);
alter table dm_message_link add constraint FK83fogk5ro6af5sippxhf54g0y foreign key (message_id) references dm_message (id);
alter table dm_read add constraint FKocya4qq2mlarn459p383gdppt foreign key (conversation_id) references dm_conversation (id);
alter table dm_read add constraint FKn0mpl5ck4fhte9g4q4rutkelu foreign key (last_read_message_id) references dm_message (id);
alter table dm_read add constraint FKjy265st6yb1jjsk6ei38vam7q foreign key (user_id) references user (id);
alter table dm_report add constraint FKd1lmayoglmt174mtds2em5u5s foreign key (conversation_id) references dm_conversation (id);
alter table dm_report add constraint FK3qx817dvdlc17xumn12yc7wru foreign key (message_id) references dm_message (id);
alter table dm_report add constraint FKj8bkisckgax0olfgdsnwepjxs foreign key (reported_user_id) references user (id);
alter table dm_report add constraint FKpu4vq5vhamrxnbs4g93ahpvjp foreign key (reporter_id) references user (id);
alter table fcm_token add constraint FK8u9xsmd3agc2nn80tb16ouph4 foreign key (user_id) references user (id);
alter table follow add constraint FKmow2qk674plvwyb4wqln37svv foreign key (follower_id) references user (id);
alter table follow add constraint FKqme6uru2g9wx9iysttk542esm foreign key (following_id) references user (id);
alter table notification_delivery add constraint FKbow3ovamka8ajuqgr10tvgeuc foreign key (event_id) references notification_event (id);
alter table notification_delivery add constraint FKcs92do73j82xiu7mbcrh510h8 foreign key (receiver_id) references user (id);
alter table notification_event add constraint FKnojjakewv0i6xh0kjycbo2xv6 foreign key (actor_id) references user (id);
alter table notification_event add constraint FKcobqqk27m1a09ail6ycnonykv foreign key (type_id) references notification_type (id);
alter table notification_setting add constraint FK81ru0i0vu6x5gov2qycxjgvn foreign key (type_id) references notification_type (id);
alter table notification_setting add constraint FKbwsuroqorxx1boup2snb1t1u9 foreign key (user_id) references user (id);
alter table recommendation_result add constraint FKo9msfticayvg9qlay2eqk9b7r foreign key (challenge_id) references challenge (id);
alter table recommendation_result add constraint FKhnyweot4vmauj8flt3qo3bs7s foreign key (favor_id) references user_favor (id);
alter table user_block add constraint FKccncjsehavren2hx4gmenhwim foreign key (blocked_id) references user (id);
alter table user_block add constraint FKla30ofkpxixhf1cmi2a2veban foreign key (blocker_id) references user (id);
alter table user_challenge add constraint FKhda3k82arbp1u2vi0puav0qxs foreign key (challenge_id) references challenge (id);
alter table user_challenge add constraint FK5fq8uyvewqccv8omsh2abg2d5 foreign key (user_id) references user (id);
alter table user_favor add constraint FKmp2m1ofnmrwf2irtxrglc6xvb foreign key (user_id) references user (id);
