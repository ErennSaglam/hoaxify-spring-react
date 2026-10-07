import http from "@/lib/http";

export function createHoax(body) {
  return http.post("/api/v1/hoaxes", body);
}

export function loadTags() {
  return http.get("/api/v1/tags");
}

export function loadTopUsers(limit = 5) {
  return http.get("/api/v1/stats/top-users", { params: { limit } });
}
