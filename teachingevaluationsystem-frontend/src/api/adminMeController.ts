// @ts-ignore
/* eslint-disable */
import request from "@/api/request";

/** 此处后端没有提供注释 GET /api/admin/me */
export async function me(options?: { [key: string]: any }) {
  return request<Record<string, any>>("/api/admin/me", {
    method: "GET",
    ...(options || {}),
  });
}
