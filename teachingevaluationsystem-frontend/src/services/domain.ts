import request from '@/api/request'
import {
  classes as classesApi,
  createClass as createClassApi,
  updateClass as updateClassApi,
  deleteClass as deleteClassApi,
  students as studentsApi,
  createStudent as createStudentApi,
  updateStudent as updateStudentApi,
  deleteStudent as deleteStudentApi,
  teachers as teachersApi,
  createTeacher as createTeacherApi,
  updateTeacher as updateTeacherApi,
  deleteTeacher as deleteTeacherApi,
  courses as coursesApi,
  createCourse as createCourseApi,
  updateCourse as updateCourseApi,
  deleteCourse as deleteCourseApi,
  evaluations as evaluationsApi,
  createEvaluation as createEvaluationApi,
  updateEvaluation as updateEvaluationApi,
  deleteEvaluation as deleteEvaluationApi,
  indicators as indicatorsApi,
  createIndicator as createIndicatorApi,
  updateIndicator as updateIndicatorApi,
  deleteIndicator as deleteIndicatorApi
} from '@/api/adminDomainController'

export type Paged<T> = { current: number; pages: number; total: number; records: T[] }
export type PageQuery = { page?: number; size?: number }
export type AdminClass = { id: number; className: string; major: string; grade: string }
export type AdminStudent = { id: number; userId: number; username: string; name: string; studentNo: string; className: string; major: string; grade: string }
export type AdminTeacher = { id: number; userId: number; username: string; name: string; teacherType: string }
export type AdminUser = {
  userId: number
  username: string
  name: string
  role: string
  isAdmin: boolean
  grantedBy: number | null
  grantedAt: string | null
}
export type AdminCourse = { id: number; grade: string; courseNameCn: string; courseNameEn: string; classIds: number[]; teacherIds: number[] }
export type AdminEvaluation = { id: number; name: string; academicYear: string; term: string; status: string }
export type AdminScore = {
  taskId: number
  evaluationId: number
  evaluationName: string
  studentId: number
  studentNo: string
  studentName: string
  courseId: number
  courseNameCn: string
  teacherId: number
  teacherName: string
  teacherType: string
  taskStatus: string
  totalScore: number | null
  submittedAt: string | null
  submitMode: string | null
}
export type TeacherScoreStat = {
  teacherId: number
  teacherName: string
  teacherType: string
  submissionCount: number
  totalScoreSum: number
  averageScore: number
}
export type StudentEvaluationStatus = {
  studentId: number
  studentNo: string
  studentName: string
  className: string
  major: string
  grade: string
  totalTasks: number
  completedTasks: number
  pendingTasks: number
  completionRate: number
}
export type AdminIndicator = { id: string | number; teacherType: string; level: number; parentId: string | number | null; name: string; nameEn: string | null; weight: number | null; sortOrder: number; isActive: number }
export type StudentImportResult = {
  totalRows: number
  successRows: number
  skippedRows: number
  createdUsers: number
  createdStudents: number
  updatedStudents: number
  createdGrades: number
  createdClasses: number
}
export type EvaluationCourseImportResult = {
  totalRows: number
  successRows: number
  skippedRows: number
  createdCourses: number
  linkedCourses: number
  createdTeachers: number
  linkedTeacherRelations: number
  linkedClassRelations: number
}
export type EvaluationCreateImportResult = {
  evaluationId: number
  importResult: EvaluationCourseImportResult
}

export type ClassPayload = { className: string; major: string; grade: string }
export type StudentCreatePayload = { username: string; name: string; password: string; studentNo: string; className: string; major: string; grade: string }
export type StudentUpdatePayload = Omit<StudentCreatePayload, 'password'>
export type TeacherCreatePayload = { username: string; name: string; password: string; teacherType: string }
export type TeacherUpdatePayload = Omit<TeacherCreatePayload, 'password'>
export type CoursePayload = { evaluationId?: number | null; grade: string; major?: string; courseNameCn: string; courseNameEn: string; classIds: number[]; teacherIds: number[] }
export type EvaluationPayload = { name: string; academicYear: string; term: string; status: string }
export type IndicatorPayload = { teacherType: string; level: number; parentId?: number | null; name: string; nameEn?: string | null; weight?: number | null; sortOrder: number; isActive: number }

export type ClassQuery = PageQuery & { className?: string; major?: string; grade?: string }
export type StudentQuery = PageQuery & { className?: string; enrollYear?: number | null; studentNo?: string; name?: string }
export type TeacherQuery = PageQuery & { teacherType?: string; name?: string }
export type CourseQuery = PageQuery & { evaluationId?: number | null; grade?: string }
export type EvaluationQuery = PageQuery & { status?: string }
export type IndicatorQuery = { teacherType?: string; level?: number | null; parentId?: number | null }
export type AdminUserQuery = PageQuery & { role?: string; keyword?: string; isAdmin?: number | null }
export type ScoreQuery = PageQuery & { evaluationId?: number | null; studentNo?: string; studentName?: string; taskStatus?: string }
export type StudentEvaluationStatusQuery = PageQuery & { studentNo?: string; studentName?: string; className?: string }
export type TeacherScoreStatQuery = PageQuery & { teacherType?: string; teacherName?: string }

function asPaged<T>(data: unknown): Paged<T> {
  return data as Paged<T>
}

export async function getClasses(query: ClassQuery = {}) {
  return asPaged<AdminClass>(await classesApi({ page: query.page ?? 1, size: query.size ?? 10, ...query }))
}

export function createClass(payload: ClassPayload) {
  return createClassApi(payload as API.ClassUpsertRequest)
}

export function updateClass(id: number, payload: ClassPayload) {
  return updateClassApi({ id }, payload as API.ClassUpsertRequest)
}

export function deleteClass(id: number) {
  return deleteClassApi({ id })
}

export async function getStudents(query: StudentQuery = {}) {
  const params: API.studentsParams = {
    page: query.page ?? 1,
    size: query.size ?? 10,
    className: query.className || undefined,
    studentNo: query.studentNo || undefined,
    name: query.name || undefined
  }
  if (query.enrollYear !== null && query.enrollYear !== undefined) params.enrollYear = query.enrollYear
  return asPaged<AdminStudent>(await studentsApi(params))
}

export function createStudent(payload: StudentCreatePayload) {
  return createStudentApi(payload as unknown as API.StudentUpsertRequest)
}

export function updateStudent(id: number, payload: StudentUpdatePayload) {
  return updateStudentApi({ id }, payload as unknown as API.StudentUpdateRequest)
}

export function deleteStudent(id: number) {
  return deleteStudentApi({ id })
}

export function importStudents(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request<StudentImportResult>('/api/admin/students/import', { method: 'POST', data: formData })
}

export async function getTeachers(query: TeacherQuery = {}) {
  return asPaged<AdminTeacher>(await teachersApi({ page: query.page ?? 1, size: query.size ?? 10, ...query }))
}

export function createTeacher(payload: TeacherCreatePayload) {
  return createTeacherApi(payload as unknown as API.TeacherUpsertRequest)
}

export function updateTeacher(id: number, payload: TeacherUpdatePayload) {
  return updateTeacherApi({ id }, payload as API.TeacherUpdateRequest)
}

export function deleteTeacher(id: number) {
  return deleteTeacherApi({ id })
}

export async function getCourses(query: CourseQuery = {}) {
  const params: API.coursesParams = { page: query.page ?? 1, size: query.size ?? 10, grade: query.grade || undefined }
  if (query.evaluationId !== null && query.evaluationId !== undefined) params.evaluationId = query.evaluationId
  return asPaged<AdminCourse>(await coursesApi(params))
}

export function createCourse(payload: CoursePayload) {
  return createCourseApi(payload as API.CourseUpsertRequest)
}

export function updateCourse(id: number, payload: CoursePayload) {
  return updateCourseApi({ id }, payload as API.CourseUpsertRequest)
}

export function deleteCourse(id: number) {
  return deleteCourseApi({ id })
}

export async function getEvaluations(query: EvaluationQuery = {}) {
  return asPaged<AdminEvaluation>(await evaluationsApi({ page: query.page ?? 1, size: query.size ?? 10, ...query }))
}

export function createEvaluation(payload: EvaluationPayload) {
  return createEvaluationApi(payload as API.EvaluationUpsertRequest)
}

export function updateEvaluation(id: number, payload: EvaluationPayload) {
  return updateEvaluationApi({ id }, payload as API.EvaluationUpsertRequest)
}

export function deleteEvaluation(id: number) {
  return deleteEvaluationApi({ id })
}

export function publishEvaluation(id: number) {
  return request<void>(`/api/admin/evaluations/${id}/publish`, { method: 'POST' })
}

export function closeEvaluation(id: number) {
  return request<void>(`/api/admin/evaluations/${id}/close`, { method: 'POST' })
}

export function importEvaluationCourses(evaluationId: number, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request<EvaluationCourseImportResult>(`/api/admin/evaluations/${evaluationId}/courses/import`, {
    method: 'POST',
    data: formData
  })
}

export function createEvaluationWithCourses(payload: EvaluationPayload, file: File) {
  const formData = new FormData()
  formData.append('name', payload.name)
  formData.append('academicYear', payload.academicYear)
  formData.append('term', payload.term)
  formData.append('status', payload.status)
  formData.append('file', file)
  return request<EvaluationCreateImportResult>('/api/admin/evaluations/create-with-courses', {
    method: 'POST',
    data: formData
  })
}

export async function getIndicators(query: IndicatorQuery = {}) {
  const params: API.indicatorsParams = {
    page: 1,
    size: 500,
    teacherType: query.teacherType || undefined
  }
  if (query.level !== null && query.level !== undefined) params.level = query.level
  if (query.parentId !== null && query.parentId !== undefined) params.parentId = query.parentId
  return asPaged<AdminIndicator>(await indicatorsApi(params))
}

export function createIndicator(payload: IndicatorPayload) {
  return createIndicatorApi(payload as API.IndicatorUpsertRequest)
}

export function updateIndicator(id: string | number, payload: IndicatorPayload) {
  return updateIndicatorApi({ id: id as any }, payload as API.IndicatorUpsertRequest)
}

export function deleteIndicator(id: string | number) {
  return deleteIndicatorApi({ id: id as any })
}

export async function getAdminUsers(query: AdminUserQuery = {}) {
  const params: Record<string, any> = {
    page: query.page ?? 1,
    size: query.size ?? 10,
    role: query.role || undefined,
    keyword: query.keyword || undefined
  }
  if (query.isAdmin !== null && query.isAdmin !== undefined) params.isAdmin = query.isAdmin
  return asPaged<AdminUser>(await request('/api/admin/admin-users', { method: 'GET', params }))
}

export function grantAdminPermission(userId: number) {
  return request<void>(`/api/admin/users/${userId}/admin-permission`, { method: 'POST' })
}

export function revokeAdminPermission(userId: number) {
  return request<void>(`/api/admin/users/${userId}/admin-permission`, { method: 'DELETE' })
}

export async function getScores(query: ScoreQuery = {}) {
  const params: Record<string, any> = {
    page: query.page ?? 1,
    size: query.size ?? 10,
    studentNo: query.studentNo || undefined,
    studentName: query.studentName || undefined,
    taskStatus: query.taskStatus || undefined
  }
  if (query.evaluationId !== null && query.evaluationId !== undefined) params.evaluationId = query.evaluationId
  return asPaged<AdminScore>(await request('/api/admin/scores', { method: 'GET', params }))
}

export async function getTeacherScoreStats(evaluationId: number, query: TeacherScoreStatQuery = {}) {
  return asPaged<TeacherScoreStat>(await request(`/api/admin/evaluations/${evaluationId}/teacher-score-stats`, {
    method: 'GET',
    params: {
      page: query.page ?? 1,
      size: query.size ?? 10,
      teacherType: query.teacherType || undefined,
      teacherName: query.teacherName || undefined
    }
  }))
}

export async function getStudentEvaluationStatus(evaluationId: number, query: StudentEvaluationStatusQuery = {}) {
  return asPaged<StudentEvaluationStatus>(await request(`/api/admin/evaluations/${evaluationId}/student-evaluation-status`, {
    method: 'GET',
    params: {
      page: query.page ?? 1,
      size: query.size ?? 10,
      studentNo: query.studentNo || undefined,
      studentName: query.studentName || undefined,
      className: query.className || undefined
    }
  }))
}

async function downloadExcel(url: string, filename: string) {
  const token = localStorage.getItem('token') || ''
  const response = await fetch(url, {
    method: 'GET',
    headers: token ? { satoken: token } : {}
  })
  if (!response.ok) throw new Error(`HTTP ${response.status}`)
  const blob = await response.blob()
  const objectUrl = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = objectUrl
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(objectUrl)
}

export function exportIncompleteStudents(evaluationId: number) {
  return downloadExcel(
    `/api/admin/evaluations/${evaluationId}/student-evaluation-status/export-incomplete`,
    `incomplete_students_${evaluationId}.xlsx`
  )
}

export function exportTeacherScoreStats(evaluationId: number) {
  return downloadExcel(
    `/api/admin/evaluations/${evaluationId}/teacher-score-stats/export`,
    `teacher_score_stats_${evaluationId}.xlsx`
  )
}
