import { defineConfig } from 'orval'
import {appRequest} from "./src/app/api/orval-mutator";

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
            target: 'src/app/api/generated/app.ts',
            schemas: 'src/app/api/generated/models',
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
