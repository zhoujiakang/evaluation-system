declare namespace API {
  type AdminRegisterRequest = {
    username: string;
    name: string;
    password: string;
    role: string;
  };

  type ApiResponseListMapStringObject = {
    code?: number;
    message?: string;
    data?: Record<string, any>[];
  };

  type ApiResponseLoginResponse = {
    code?: number;
    message?: string;
    data?: LoginResponse;
  };

  type ApiResponseLong = {
    code?: number;
    message?: string;
    data?: number;
  };

  type ApiResponseMapStringObject = {
    code?: number;
    message?: string;
    data?: Record<string, any>;
  };

  type ApiResponsePageResultEvaluationUpsertRequest = {
    code?: number;
    message?: string;
    data?: PageResultEvaluationUpsertRequest;
  };

  type ApiResponsePageResultIndicatorUpsertRequest = {
    code?: number;
    message?: string;
    data?: PageResultIndicatorUpsertRequest;
  };

  type ApiResponsePageResultMapStringObject = {
    code?: number;
    message?: string;
    data?: PageResultMapStringObject;
  };

  type ApiResponseVoid = {
    code?: number;
    message?: string;
    data?: Record<string, any>;
  };

  type coursesParams = {
    page?: number;
    size?: number;
    evaluationId?: number;
    major?: string;
    grade?: string;
  };

  type classesParams = {
    page?: number;
    size?: number;
    className?: string;
    major?: string;
    grade?: string;
  };

  type ClassUpsertRequest = {
    className: string;
    major: string;
    grade?: string;
  };

  type createClassParams = Record<string, never>;

  type updateClassParams = {
    id: number;
  };

  type deleteClassParams = {
    id: number;
  };

  type CourseUpsertRequest = {
    grade: string;
    major: string;
    courseNameCn: string;
    courseNameEn: string;
  };

  type deleteCourseParams = {
    id: number;
  };

  type deleteEvaluationParams = {
    id: number;
  };

  type deleteIndicatorParams = {
    id: number;
  };

  type deleteStudentParams = {
    id: number;
  };

  type deleteTeacherParams = {
    id: number;
  };

  type evaluationsParams = {
    page?: number;
    size?: number;
    status?: string;
  };

  type EvaluationUpsertRequest = {
    name: string;
    academicYear: string;
    term: string;
    status: string;
  };

  type indicatorsParams = {
    page?: number;
    size?: number;
    teacherType?: string;
    level?: number;
    parentId?: number;
  };

  type IndicatorUpsertRequest = {
    teacherType: string;
    level: number;
    parentId?: number;
    name: string;
    sortOrder: number;
    isActive: number;
  };

  type Item = {
    indicatorId: number;
    score: number;
  };

  type LoginRequest = {
    username: string;
    password: string;
  };

  type LoginResponse = {
    token?: string;
    userId?: number;
    username?: string;
    name?: string;
    side?: string;
  };

  type PageResultEvaluationUpsertRequest = {
    total?: number;
    records?: EvaluationUpsertRequest[];
  };

  type PageResultIndicatorUpsertRequest = {
    total?: number;
    records?: IndicatorUpsertRequest[];
  };

  type PageResultMapStringObject = {
    total?: number;
    records?: Record<string, any>[];
  };

  type ResetPasswordRequest = {
    newPassword: string;
  };

  type resetStudentPasswordParams = {
    id: number;
  };

  type resetTeacherPasswordParams = {
    id: number;
  };

  type progressParams = {
    evaluationId: number;
  };

  type startParams = {
    taskId: number;
  };

  type studentsParams = {
    page?: number;
    size?: number;
    className?: string;
    enrollYear?: number;
    studentNo?: string;
    name?: string;
  };

  type StudentSubmitRequest = {
    scores: Item[];
    remark?: string;
  };

  type StudentUpdateRequest = {
    username: string;
    name: string;
    studentNo: string;
    className: string;
    major: string;
    enrollYear: number;
  };

  type StudentUpsertRequest = {
    username: string;
    name: string;
    password: string;
    studentNo: string;
    className: string;
    major: string;
    enrollYear: number;
  };

  type submitParams = {
    taskId: number;
  };

  type tasksParams = {
    evaluationId?: number;
  };

  type teachersParams = {
    page?: number;
    size?: number;
    teacherType?: string;
    name?: string;
  };

  type TeacherUpdateRequest = {
    username: string;
    name: string;
    teacherType: string;
  };

  type TeacherUpsertRequest = {
    username: string;
    name: string;
    password: string;
    teacherType: string;
  };

  type updateCourseParams = {
    id: number;
  };

  type updateEvaluationParams = {
    id: number;
  };

  type updateIndicatorParams = {
    id: number;
  };

  type updateStudentParams = {
    id: number;
  };

  type updateTeacherParams = {
    id: number;
  };
}
