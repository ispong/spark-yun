import { defineConfig } from 'orval'

export default defineConfig({
    app: {
        input: {
            target: 'http://localhost:8080/swagger-ui/api-docs',
            parserOptions: {
                headers: [
                    {
                        domains: ['localhost:8080'],
                        headers: {
                            authorization: 'Basic YWRtaW46YWRtaW4xMjM='
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
