import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import http from "@/lib/http";

/** Hoax sayısı veritabanındaki hoaxify_user_hoax_count fonksiyonundan (SimpleJdbcCall) geliyor */
export function UserSummary({ userId }) {
  const { t, i18n } = useTranslation();
  const [summary, setSummary] = useState();

  useEffect(() => {
    http
      .get(`/api/v1/users/${userId}/summary`)
      .then(({ data }) => setSummary(data))
      .catch(() => setSummary());
  }, [userId]);

  if (!summary) return null;

  return (
    <div className="text-muted small mt-2">
      {t("hoaxCount", { count: summary.hoaxCount })}
      {summary.lastHoaxAt && (
        <> · {t("lastHoax")}: {new Date(summary.lastHoaxAt).toLocaleString(i18n.language)}</>
      )}
    </div>
  );
}
