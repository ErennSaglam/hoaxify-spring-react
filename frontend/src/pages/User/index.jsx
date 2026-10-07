import { getUser } from "./api";
import { Alert } from "@/shared/components/Alert";
import { Spinner } from "@/shared/components/Spinner";
import { useRouteParamApiRequest } from "@/shared/hooks/useRouteParamApiRequest";
import { ProfileCard } from "./components/ProfileCard";
import { HoaxFeed } from "@/shared/components/HoaxFeed";

export function User() {
  const {
    apiProgress,
    data: user,
    error,
  } = useRouteParamApiRequest("id", getUser);

  return (
    <>
      {apiProgress && (
        <Alert styleType="secondary" center>
          <Spinner />
        </Alert>
      )}
      {user && <ProfileCard user={user}/>}
      {user && (
        <div className="mt-3">
          <HoaxFeed userId={user.id} />
        </div>
      )}
      {error && <Alert styleType="danger">{error}</Alert>}
    </>
  );
}
