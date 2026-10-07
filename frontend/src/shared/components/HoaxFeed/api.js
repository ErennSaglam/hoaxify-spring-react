import http from "@/lib/http";

// userId verilirse o kullanıcının hoax'ları, tag verilirse o etiketi taşıyanlar, ikisi de yoksa tüm akış
export function loadHoaxes({ page = 0, userId, tag } = {}) {
  const url = userId ? `/api/v1/users/${userId}/hoaxes` : "/api/v1/hoaxes";
  return http.get(url, { params: { page, size: 5, tag: tag || undefined } });
}

export function deleteHoax(id) {
  return http.delete(`/api/v1/hoaxes/${id}`);
}
