import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { ProfileImage } from "@/shared/components/ProfileImage";
import { loadTopUsers } from "./hoaxApi";

/** Veri backend'de DAO (düz SQL + RowMapper) ile hazırlanıyor: GET /api/v1/stats/top-users */
export function TopUsers({ refreshKey }) {
  const { t } = useTranslation();
  const [users, setUsers] = useState([]);

  useEffect(() => {
    loadTopUsers()
      .then(({ data }) => setUsers(data))
      .catch(() => setUsers([]));
  }, [refreshKey]);

  if (users.length === 0) return null;

  return (
    <div className="card mb-3">
      <div className="card-header">{t("topUsers")}</div>
      <ul className="list-group list-group-flush">
        {users.map((user) => (
          <li key={user.userId} className="list-group-item d-flex align-items-center">
            <Link to={`/user/${user.userId}`} className="text-decoration-none d-flex align-items-center">
              <ProfileImage width={24} image={user.image} />
              <span className="ms-2">{user.username}</span>
            </Link>
            <span className="badge text-bg-primary ms-auto">{user.hoaxCount}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
