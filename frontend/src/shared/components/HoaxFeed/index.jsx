import { useCallback, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Alert } from "@/shared/components/Alert";
import { Spinner } from "@/shared/components/Spinner";
import { loadHoaxes } from "./api";
import { HoaxItem } from "./HoaxItem";

/**
 * Sayfalı hoax listesi. "Daha fazla" ile sonraki sayfa mevcut listenin sonuna eklenir.
 * Hem ana sayfada (tüm akış / etikete göre) hem profil sayfasında (kullanıcının hoax'ları) kullanılır.
 */
export function HoaxFeed({ userId, tag }) {
  const { t } = useTranslation();
  const [hoaxes, setHoaxes] = useState([]);
  const [page, setPage] = useState({ number: 0, last: true });
  const [apiProgress, setApiProgress] = useState(false);
  const [error, setError] = useState();

  const load = useCallback(
    async (pageNumber) => {
      setApiProgress(true);
      setError();
      try {
        const { data } = await loadHoaxes({ page: pageNumber, userId, tag });
        setHoaxes((previous) => (pageNumber === 0 ? data.content : [...previous, ...data.content]));
        setPage({ number: data.number, last: data.last });
      } catch (axiosError) {
        setError(axiosError.response?.data?.message || t("genericError"));
      } finally {
        setApiProgress(false);
      }
    },
    [userId, tag, t]
  );

  useEffect(() => {
    load(0);
  }, [load]);

  const onDeleteSuccess = (id) => {
    setHoaxes((previous) => previous.filter((hoax) => hoax.id !== id));
  };

  return (
    <div className="card mb-3">
      <div className="card-header fs-5">{t("hoaxes")}</div>
      <ul className="list-group list-group-flush">
        {hoaxes.map((hoax) => (
          <HoaxItem key={hoax.id} hoax={hoax} onDeleteSuccess={onDeleteSuccess} />
        ))}
        {!apiProgress && !error && hoaxes.length === 0 && (
          <li className="list-group-item text-center text-muted">{t("noHoaxes")}</li>
        )}
      </ul>
      <div className="card-footer text-center">
        {error && <Alert styleType="danger">{error}</Alert>}
        {apiProgress && <Spinner />}
        {!apiProgress && !page.last && (
          <button className="btn btn-outline-secondary btn-sm" onClick={() => load(page.number + 1)}>
            {t("loadMore")}
          </button>
        )}
      </div>
    </div>
  );
}
