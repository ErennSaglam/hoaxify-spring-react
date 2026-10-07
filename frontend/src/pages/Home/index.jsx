import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useSearchParams } from "react-router-dom";
import { Alert } from "@/shared/components/Alert";
import { HoaxFeed } from "@/shared/components/HoaxFeed";
import { useAuthState } from "@/shared/state/context";
import { HoaxSubmit } from "./components/HoaxSubmit";
import { TagList } from "./components/TagList";
import { TopUsers } from "./components/TopUsers";
import { UserList } from "./components/UserList";

export function Home() {
  const { t } = useTranslation();
  const authState = useAuthState();
  const [searchParams] = useSearchParams();
  const tag = searchParams.get("tag");
  // Yeni hoax paylaşılınca akış ve etiket listesi yeniden yüklensin diye artırılan sayaç
  const [refreshKey, setRefreshKey] = useState(0);

  return (
    <div className="row">
      <div className="col-lg-8">
        {authState.id > 0 && <HoaxSubmit onSuccess={() => setRefreshKey((key) => key + 1)} />}
        {tag && (
          <Alert styleType="info">
            {t("filteredByTag")} <strong>#{tag}</strong> <Link to="/">({t("clearFilter")})</Link>
          </Alert>
        )}
        <HoaxFeed key={`${tag}-${refreshKey}`} tag={tag} />
      </div>
      <div className="col-lg-4">
        <TopUsers refreshKey={refreshKey} />
        <TagList refreshKey={refreshKey} />
        <UserList />
      </div>
    </div>
  );
}
