// @ts-ignore
/* eslint-disable */
import request from "@/api/request";

/** 此处后端没有提供注释 POST /api/student/auth/login */
export async function login(
  body: API.LoginRequest,
  options?: { [key: string]: any }
) {
  return request<API.LoginResponse>("/api/student/auth/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /api/student/auth/logout */
export async function logout(options?: { [key: string]: any }) {
  return request<Record<string, any>>("/api/student/auth/logout", {
    method: "POST",
    ...(options || {}),
  });
}
