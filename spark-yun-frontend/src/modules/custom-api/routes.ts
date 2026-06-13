import type { RouteRecordRaw } from "vue-router";

const CustomApi = () => import("./views/index.vue");

const customApiRoutes: RouteRecordRaw[] = [
  {
    path: "custom-api",
    name: "custom-api",
    component: CustomApi,
  },
];

export default customApiRoutes;
