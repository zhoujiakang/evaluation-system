import JSONBigFactory from 'json-bigint'

type RequestOptions = {
  method?: string
  params?: Record<string, any>
  data?: any
  headers?: Record<string, string>
}

type ApiResponse<T> = {
  code: number
  message: string
  data: T
}

function toPlainObject<T>(input: T): T {
  if (Array.isArray(input)) {
    return input.map((item) => toPlainObject(item)) as T
  }
  if (input && typeof input === 'object') {
    const result: Record<string, any> = {}
    Object.keys(input as Record<string, any>).forEach((key) => {
      result[key] = toPlainObject((input as Record<string, any>)[key])
    })
    return result as T
  }
  return input
}

function buildUrl(url: string, params?: Record<string, any>) {
  if (!params) return url
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.append(key, String(value))
    }
  })
  const qs = query.toString()
  return qs ? `${url}?${qs}` : url
}

export default async function request<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const JSONBig = (JSONBigFactory as any)({ storeAsString: true })
  const token = localStorage.getItem('token') || ''
  const fullUrl = buildUrl(url, options.params)
  const isFormData = options.data instanceof FormData
  const headers: Record<string, string> = {
    ...(token ? { satoken: token } : {}),
    ...(options.headers || {})
  }
  if (!isFormData) headers['Content-Type'] = 'application/json'
  const response = await fetch(fullUrl, {
    method: options.method || 'GET',
    headers,
    body: options.data === undefined ? undefined : isFormData ? options.data : JSON.stringify(options.data)
  })

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`)
  }

  const raw = await response.text()
  const json = JSONBig.parse(raw) as ApiResponse<T>
  if (json.code !== 0) {
    throw new Error(json.message || '请求失败')
  }
  return toPlainObject(json.data)
}
