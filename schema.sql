create table teaching_evaluation.admin_permission
(
    user_id    bigint unsigned                    not null comment '被授予管理员权限的用户ID'
        primary key,
    granted_by bigint unsigned                    null comment '授权人用户ID',
    granted_at datetime default CURRENT_TIMESTAMP not null comment '授权时间'
)
    comment '管理员权限表';

create table teaching_evaluation.class
(
    id         bigint unsigned auto_increment comment '班级主键ID'
        primary key,
    class_name varchar(64)                        not null comment '班级名称',
    major      varchar(128)                       not null comment '所属专业',
    grade_id   bigint unsigned                    not null comment '所属年级ID（关联grade.id）',
    created_at datetime default CURRENT_TIMESTAMP not null comment '创建时间'
)
    comment '班级表';

create table teaching_evaluation.course
(
    id             bigint unsigned auto_increment comment '课程主键ID'
        primary key,
    grade_id       bigint unsigned                    not null comment '所属年级ID（关联grade.id）',
    course_name_cn varchar(255)                       not null comment '课程中文名',
    course_name_en varchar(255)                       not null comment '课程英文名',
    created_at     datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    evaluation_id  bigint unsigned                    null comment '测评ID（关联evaluation.id）'
)
    comment '课程表';

create index idx_course_eval_grade
    on teaching_evaluation.course (evaluation_id, grade_id);

create table teaching_evaluation.course_class
(
    id        bigint unsigned auto_increment comment '主键ID'
        primary key,
    course_id bigint unsigned not null comment '课程ID',
    class_id  bigint unsigned not null comment '班级ID',
    constraint uq_course_class
        unique (course_id, class_id)
)
    comment '课程-班级关联表';

create table teaching_evaluation.course_teacher
(
    id         bigint unsigned auto_increment comment '主键ID'
        primary key,
    course_id  bigint unsigned not null comment '课程ID',
    teacher_id bigint unsigned not null comment '老师ID',
    constraint uq_course_teacher
        unique (course_id, teacher_id)
)
    comment '课程-老师关联表';

create table teaching_evaluation.eval_task
(
    id            bigint unsigned auto_increment comment '任务主键ID'
        primary key,
    evaluation_id bigint unsigned                                         not null comment '测评ID',
    student_id    bigint unsigned                                         not null comment '学生ID',
    course_id     bigint unsigned                                         not null comment '课程ID',
    teacher_id    bigint unsigned                                         not null comment '老师ID',
    status        enum ('pending', 'completed') default 'pending'         not null comment '任务状态：pending未完成 completed已完成',
    score         decimal(4, 2)                                           null comment '总分，范围0-10',
    completed_at  datetime                                                null comment '完成时间',
    created_at    datetime                      default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at    datetime                      default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uq_eval_task_unique
        unique (evaluation_id, student_id, course_id, teacher_id)
)
    comment '学生评教任务表';

create index idx_eval_task_eval_student
    on teaching_evaluation.eval_task (evaluation_id, student_id);

create index idx_eval_task_eval_student_status
    on teaching_evaluation.eval_task (evaluation_id, student_id, status);

create index idx_eval_task_eval_teacher
    on teaching_evaluation.eval_task (evaluation_id, teacher_id);

create table teaching_evaluation.evaluation
(
    id            bigint unsigned auto_increment comment '测评主键ID'
        primary key,
    name          varchar(128)                                                    not null comment '测评名称',
    academic_year varchar(32)                                                     not null comment '学年，例如2025-2026',
    term          varchar(32)                                                     not null comment '学期，例如2',
    status        enum ('draft', 'published', 'closed') default 'draft'           not null comment '状态：draft草稿 published正式发布 closed关闭评分',
    published_at  datetime                                                        null comment '正式发布时间',
    closed_at     datetime                                                        null comment '关闭评分时间',
    created_by    bigint unsigned                                                 not null comment '创建人用户ID',
    created_at    datetime                              default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at    datetime                              default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uq_eval
        unique (academic_year, term)
)
    comment '测评批次表';

create table teaching_evaluation.grade
(
    id         bigint unsigned auto_increment comment '年级主键ID'
        primary key,
    grade_name varchar(32)                        not null comment '年级名称，例如2025级',
    created_at datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    constraint grade_name
        unique (grade_name)
)
    comment '年级表';

create table teaching_evaluation.indicator
(
    id           bigint unsigned auto_increment comment '指标主键ID'
        primary key,
    teacher_type enum ('CN', 'INTL')                  not null comment '适用老师类型：CN中方 INTL外方',
    level        tinyint                              not null comment '指标层级：1一级 2二级',
    parent_id    bigint unsigned                      null comment '父级指标ID，一级为空，二级指向一级',
    name         varchar(128)                         not null comment '指标名称',
    name_en      varchar(512)                         not null comment '指标英文名称',
    weight       decimal(5, 2)                        null comment '指标权重',
    sort_order   int        default 0                 not null comment '排序值，越小越靠前',
    is_active    tinyint(1) default 1                 not null comment '是否启用：1启用 0停用',
    locked_at    datetime                             null comment '锁定时间（测评发布后写入，程序据此限制修改）',
    created_at   datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at   datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '全局指标表（一级/二级同表）';

create index idx_indicator_type_active_level_sort
    on teaching_evaluation.indicator (teacher_type, is_active, level, sort_order);

create table teaching_evaluation.score
(
    id            bigint unsigned auto_increment comment '分数主键ID'
        primary key,
    submission_id bigint unsigned                    not null comment '提交记录ID',
    evaluation_id bigint unsigned                    not null comment '测评ID',
    task_id       bigint unsigned                    not null comment '评教任务ID',
    indicator_id  bigint unsigned                    not null comment '指标ID',
    score         decimal(4, 2)                      not null comment '分值，范围0-10',
    created_at    datetime default CURRENT_TIMESTAMP not null comment '创建时间'
)
    comment '评教分数明细表';

create index idx_score_eval_task
    on teaching_evaluation.score (evaluation_id, task_id);

create index idx_score_submission
    on teaching_evaluation.score (submission_id);

create table teaching_evaluation.score_submission
(
    id                bigint unsigned auto_increment comment '提交主键ID'
        primary key,
    evaluation_id     bigint unsigned                                 not null comment '测评ID',
    task_id           bigint unsigned                                 not null comment '评教任务ID',
    submitter_user_id bigint unsigned                                 not null comment '提交人用户ID',
    submit_mode       enum ('student', 'admin_proxy', 'admin_makeup') not null comment '提交方式：student学生 admin_proxy管理员代填 admin_makeup管理员补录',
    submitted_at      datetime default CURRENT_TIMESTAMP              not null comment '提交时间',
    remark            varchar(255)                                    null comment '备注',
    constraint uq_submission_task
        unique (task_id)
)
    comment '评教提交记录表';

create index idx_submission_eval_task
    on teaching_evaluation.score_submission (evaluation_id, task_id);

create index idx_submission_task
    on teaching_evaluation.score_submission (task_id);

create table teaching_evaluation.student
(
    id         bigint unsigned auto_increment comment '学生主键ID'
        primary key,
    user_id    bigint unsigned                    null comment '关联用户ID',
    student_no varchar(32)                        not null comment '学号',
    class_id   bigint unsigned                    not null comment '班级ID（关联class.id）',
    created_at datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint student_no
        unique (student_no),
    constraint user_id
        unique (user_id)
)
    comment '学生信息表';

create index idx_student_class
    on teaching_evaluation.student (class_id);

create table teaching_evaluation.teacher
(
    id           bigint unsigned auto_increment comment '老师主键ID'
        primary key,
    user_id      bigint unsigned                    null comment '关联用户ID',
    teacher_type enum ('CN', 'INTL')                not null comment '老师类型：CN中方 INTL外方',
    created_at   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint user_id
        unique (user_id)
)
    comment '老师信息表';

create index idx_teacher_type
    on teaching_evaluation.teacher (teacher_type);

create table teaching_evaluation.user
(
    id            bigint unsigned auto_increment comment '用户ID'
        primary key,
    username      varchar(64)                          not null comment '登录账号',
    name          varchar(64)                          not null comment '姓名',
    password_hash varchar(255)                         not null comment '密码哈希',
    role          enum ('student', 'teacher')          not null comment '基础角色：学生/老师',
    is_active     tinyint(1) default 1                 not null comment '是否启用：1启用 0停用',
    created_at    datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at    datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint username
        unique (username)
)
    comment '系统用户表';
