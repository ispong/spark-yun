import { defineConfig } from 'orval'

const openApiUrl = process.env.SPARK_YUN_OPENAPI_URL || 'http://localhost:8080/swagger-ui/api-docs'
const openApiToken = process.env.SPARK_YUN_OPENAPI_TOKEN || ''

export default defineConfig({
    sparkYun: {
        input: {
            target: openApiUrl,
            override: {
                transformer: (schema) => {
                    const tokenAuth = schema.components?.securitySchemes?.tokenAuth
                    if (tokenAuth && 'type' in tokenAuth && tokenAuth.type === 'apiKey' && 'scheme' in tokenAuth) {
                        delete tokenAuth.scheme
                    }

                    return schema
                }
            },
            parserOptions: {
                headers: [
                    {
                        domains: ['localhost', 'localhost:8080', '127.0.0.1', '127.0.0.1:8080'],
                        headers: {
                            authorization: openApiToken
                        }
                    }
                ]
            }
        },
        output: {
            mode: 'tags-split',
            target: 'src/app/api/generated/spark-yun.ts',
            schemas: 'src/app/api/generated/models',
            client: 'axios',
            prettier: true,
            clean: true,
            override: {
                mutator: {
                    path: 'src/app/api/orval-mutator.ts',
                    name: 'sparkYunRequest'
                }
            }
        }
    }
})
