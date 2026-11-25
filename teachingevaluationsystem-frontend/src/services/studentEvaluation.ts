import { courseTasks, options, progress, start, submit } from '@/api/studentEvaluationController'

export type StudentTask = {
  id: number
  evaluationId: number
  evaluationName: string
  courseId: number
  courseNameCn: string
  courseNameEn: string
  teacherId: number
  teacherName: string
  teacherType: string
  status: 'pending' | 'completed'
}

export type Indicator = {
  id: number
  level: number
  parentId: number | null
  name: string
}

export type TaskDetail = {
  task: StudentTask
  indicators: Indicator[]
}

export type StudentCourseTeacherTask = {
  taskId: number
  teacherId: number
  teacherName: string
  teacherType: string
  status: 'pending' | 'completed'
  completedAt?: string | null
}

export type StudentCourseTask = {
  courseId: number
  courseNameCn: string
  courseNameEn: string
  teachers: StudentCourseTeacherTask[]
}

export type StudentEvaluationOption = {
  evaluationId: number
  evaluationName: string
}

export type StudentEvaluationProgress = {
  evaluationId: number
  evaluationName: string
  total: number
  completed: number
}

export function getEvaluationOptions() {
  return options() as Promise<StudentEvaluationOption[]>
}

export function getEvaluationProgress(evaluationId: number) {
  return progress({ evaluationId }) as Promise<StudentEvaluationProgress>
}

export function getCourseTasks(evaluationId: number) {
  return courseTasks({ evaluationId }) as Promise<StudentCourseTask[]>
}

export function startTask(taskId: number) {
  return start({ taskId }) as Promise<TaskDetail>
}

export function submitTask(taskId: number, payload: { scores: Array<{ indicatorId: number; score: number }>; remark?: string }) {
  return submit({ taskId }, payload as API.StudentSubmitRequest) as unknown as Promise<void>
}
