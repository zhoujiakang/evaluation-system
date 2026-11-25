// @ts-ignore
/* eslint-disable */
import request from "@/api/request";

/** 此处后端没有提供注释 GET /api/student/evaluations/options */
export async function options(options?: { [key: string]: any }) {
  return request<Record<string, any>[]>("/api/student/evaluations/options", {
    method: "GET",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /api/student/evaluations/${param0}/progress */
export async function progress(
  params: API.progressParams,
  options?: { [key: string]: any }
) {
  const { evaluationId: param0, ...queryParams } = params;
  return request<Record<string, any>>(
    `/api/student/evaluations/${param0}/progress`,
    {
      method: "GET",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 GET /api/student/evaluations/course-tasks */
export async function courseTasks(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.tasksParams,
  options?: { [key: string]: any }
) {
  return request<Record<string, any>[]>("/api/student/evaluations/course-tasks", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /api/student/evaluations/tasks/${param0}/start */
export async function start(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.startParams,
  options?: { [key: string]: any }
) {
  const { taskId: param0, ...queryParams } = params;
  return request<Record<string, any>>(
    `/api/student/evaluations/tasks/${param0}/start`,
    {
      method: "POST",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /api/student/evaluations/tasks/${param0}/submit */
export async function submit(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.submitParams,
  body: API.StudentSubmitRequest,
  options?: { [key: string]: any }
) {
  const { taskId: param0, ...queryParams } = params;
  return request<Record<string, any>>(
    `/api/student/evaluations/tasks/${param0}/submit`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      params: { ...queryParams },
      data: body,
      ...(options || {}),
    }
  );
}
