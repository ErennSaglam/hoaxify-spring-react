import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { Button } from "@/shared/components/Button";
import { ProfileImage } from "@/shared/components/ProfileImage";
import { useAuthState } from "@/shared/state/context";
import { deleteHoax } from "./api";

export function HoaxItem({ hoax, onDeleteSuccess }) {
  const { t, i18n } = useTranslation();
  const authState = useAuthState();
  const [apiProgress, setApiProgress] = useState(false);
  const [error, setError] = useState();

  const isOwner = authState.id === hoax.user.id;

  const onClickDelete = async () => {
    if (!confirm(t("deleteHoaxConfirm"))) return;
    setApiProgress(true);
    setError();
    try {
      await deleteHoax(hoax.id);
      onDeleteSuccess(hoax.id);
    } catch (axiosError) {
      // 403: başkasının hoax'ı, 401: oturum düşmüş vb. -> backend'in mesajını göster
      setError(axiosError.response?.data?.message || t("genericError"));
      setApiProgress(false);
    }
  };

  return (
    <li className="list-group-item">
      <div className="d-flex align-items-center">
        <Link to={`/user/${hoax.user.id}`} className="text-decoration-none d-flex align-items-center">
          <ProfileImage width={32} image={hoax.user.image} />
          <span className="ms-2 fw-semibold">{hoax.user.username}</span>
        </Link>
        <small className="text-muted ms-2">{new Date(hoax.createdAt).toLocaleString(i18n.language)}</small>
        {isOwner && (
          <div className="ms-auto">
            <Button styleType="outline-danger btn-sm" apiProgress={apiProgress} onClick={onClickDelete}>
              {t("delete")}
            </Button>
          </div>
        )}
      </div>
      <p className="mt-2 mb-1" style={{ whiteSpace: "pre-wrap" }}>
        {hoax.content}
      </p>
      <div>
        {hoax.tags.map((tag) => (
          <Link key={tag.id} to={`/?tag=${tag.name}`} className="badge text-bg-light text-decoration-none me-1">
            #{tag.name}
          </Link>
        ))}
      </div>
      {error && <div className="text-danger small mt-1">{error}</div>}
    </li>
  );
}
