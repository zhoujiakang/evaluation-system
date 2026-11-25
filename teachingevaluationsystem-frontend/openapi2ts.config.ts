export default {
  schemaPath: 'http://localhost:8080/v3/api-docs',
  serversPath: './sr',
  requestLibPath: "import request from '@/api/request'",
  requestImportStatement: "import request from '@/api/request'",
  dataFields: ['data'],
  hook: {
    customType: (schemaObject, namespace, originGetType) => {
      if (!schemaObject) return originGetType(schemaObject, namespace)
      const format = (schemaObject as { format?: string }).format
      const type = (schemaObject as { type?: string }).type
      if ((type === 'integer' || type === 'number') && (format === 'int64' || format === 'long')) {
        return 'string'
      }
      return originGetType(schemaObject, namespace)
    }
  }
}
