// @ts-ignore
/* eslint-disable */
import request from "@/api/request";

/** 此处后端没有提供注释 GET /api/admin/classes */
export async function classes(
  params: API.classesParams,
  options?: { [key: string]: any }
) {
  return request<API.PageResultMapStringObject>("/api/admin/classes", {
    method: "GET",
    params: {
      page: "1",
      size: "10",
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /api/admin/classes */
export async function createClass(
  body: API.ClassUpsertRequest,
  options?: { [key: string]: any }
) {
  return request<number>("/api/admin/classes", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 PUT /api/admin/classes/${param0} */
export async function updateClass(
  params: API.updateClassParams,
  body: API.ClassUpsertRequest,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<Record<string, any>>(`/api/admin/classes/${param0}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 DELETE /api/admin/classes/${param0} */
export async function deleteClass(
  params: API.deleteClassParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<Record<string, any>>(`/api/admin/classes/${param0}`, {
    method: "DELETE",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /api/admin/courses */
export async function courses(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.coursesParams,
  options?: { [key: string]: any }
) {
  return request<API.PageResultMapStringObject>("/api/admin/courses", {
    method: "GET",
    params: {
      // page has a default value: 1
      page: "1",
      // size has a default value: 10
      size: "10",

      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /api/admin/courses */
export async function createCourse(
  body: API.CourseUpsertRequest,
  options?: { [key: string]: any }
) {
  return request<number>("/api/admin/courses", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 PUT /api/admin/courses/${param0} */
export async function updateCourse(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.updateCourseParams,
  body: API.CourseUpsertRequest,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<Record<string, any>>(`/api/admin/courses/${param0}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 DELETE /api/admin/courses/${param0} */
export async function deleteCourse(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteCourseParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<Record<string, any>>(`/api/admin/courses/${param0}`, {
    method: "DELETE",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /api/admin/evaluations */
export async function evaluations(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.evaluationsParams,
  options?: { [key: string]: any }
) {
  return request<API.PageResultEvaluationUpsertRequest>(
    "/api/admin/evaluations",
    {
      method: "GET",
      params: {
        // page has a default value: 1
        page: "1",
        // size has a default value: 10
        size: "10",
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /api/admin/evaluations */
export async function createEvaluation(
  body: API.EvaluationUpsertRequest,
  options?: { [key: string]: any }
) {
  return request<number>("/api/admin/evaluations", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 PUT /api/admin/evaluations/${param0} */
export async function updateEvaluation(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.updateEvaluationParams,
  body: API.EvaluationUpsertRequest,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<Record<string, any>>(`/api/admin/evaluations/${param0}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 DELETE /api/admin/evaluations/${param0} */
export async function deleteEvaluation(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteEvaluationParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<Record<string, any>>(`/api/admin/evaluations/${param0}`, {
    method: "DELETE",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /api/admin/indicators */
export async function indicators(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.indicatorsParams,
  options?: { [key: string]: any }
) {
  return request<API.PageResultIndicatorUpsertRequest>(
    "/api/admin/indicators",
    {
      method: "GET",
      params: {
        // page has a default value: 1
        page: "1",
        // size has a default value: 10
        size: "10",

        ...params,
      },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /api/admin/indicators */
export async function createIndicator(
  body: API.IndicatorUpsertRequest,
  options?: { [key: string]: any }
) {
  return request<number>("/api/admin/indicators", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 PUT /api/admin/indicators/${param0} */
export async function updateIndicator(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.updateIndicatorParams,
  body: API.IndicatorUpsertRequest,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<Record<string, any>>(`/api/admin/indicators/${param0}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 DELETE /api/admin/indicators/${param0} */
export async function deleteIndicator(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteIndicatorParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<Record<string, any>>(`/api/admin/indicators/${param0}`, {
    method: "DELETE",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /api/admin/students */
export async function students(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.studentsParams,
  options?: { [key: string]: any }
) {
  return request<API.PageResultMapStringObject>("/api/admin/students", {
    method: "GET",
    params: {
      // page has a default value: 1
      page: "1",
      // size has a default value: 10
      size: "10",

      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /api/admin/students */
export async function createStudent(
  body: API.StudentUpsertRequest,
  options?: { [key: string]: any }
) {
  return request<number>("/api/admin/students", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 PUT /api/admin/students/${param0} */
export async function updateStudent(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.updateStudentParams,
  body: API.StudentUpdateRequest,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<Record<string, any>>(`/api/admin/students/${param0}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 DELETE /api/admin/students/${param0} */
export async function deleteStudent(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteStudentParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<Record<string, any>>(`/api/admin/students/${param0}`, {
    method: "DELETE",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 PUT /api/admin/students/${param0}/password */
export async function resetStudentPassword(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.resetStudentPasswordParams,
  body: API.ResetPasswordRequest,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<Record<string, any>>(
    `/api/admin/students/${param0}/password`,
    {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      params: { ...queryParams },
      data: body,
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 GET /api/admin/teachers */
export async function teachers(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.teachersParams,
  options?: { [key: string]: any }
) {
  return request<API.PageResultMapStringObject>("/api/admin/teachers", {
    method: "GET",
    params: {
      // page has a default value: 1
      page: "1",
      // size has a default value: 10
      size: "10",
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /api/admin/teachers */
export async function createTeacher(
  body: API.TeacherUpsertRequest,
  options?: { [key: string]: any }
) {
  return request<number>("/api/admin/teachers", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 PUT /api/admin/teachers/${param0} */
export async function updateTeacher(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.updateTeacherParams,
  body: API.TeacherUpdateRequest,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<Record<string, any>>(`/api/admin/teachers/${param0}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 DELETE /api/admin/teachers/${param0} */
export async function deleteTeacher(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteTeacherParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<Record<string, any>>(`/api/admin/teachers/${param0}`, {
    method: "DELETE",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 PUT /api/admin/teachers/${param0}/password */
export async function resetTeacherPassword(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.resetTeacherPasswordParams,
  body: API.ResetPasswordRequest,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<Record<string, any>>(
    `/api/admin/teachers/${param0}/password`,
    {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      params: { ...queryParams },
      data: body,
      ...(options || {}),
    }
  );
}
