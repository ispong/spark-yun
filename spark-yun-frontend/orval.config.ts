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
            target: 'src/app/type/app.ts',
            schemas: 'src/app/type/models',
            client: 'axios',
            clean: true,
            override: {
                mutator: {
                    path: 'src/app/api/orval-mutator.ts',
                    name: 'appRequest'
                }
            }
        }
    }
})
