import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { loadTags } from "./hoaxApi";

export function TagList({ refreshKey }) {
  const { t } = useTranslation();
  const [tags, setTags] = useState([]);

  useEffect(() => {
    loadTags()
      .then(({ data }) => setTags(data))
      .catch(() => setTags([]));
  }, [refreshKey]);

  if (tags.length === 0) return null;

  return (
    <div className="card mb-3">
      <div className="card-header">{t("popularTags")}</div>
      <div className="card-body">
        {tags.map((tag) => (
          <Link key={tag.id} to={`/?tag=${tag.name}`} className="badge text-bg-secondary text-decoration-none me-1 mb-1">
            #{tag.name}
          </Link>
        ))}
      </div>
    </div>
  );
}
