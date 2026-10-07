import { Button } from "@/shared/components/Button";
import { useAuthState } from "@/shared/state/context";
import { useState } from "react";
import { ProfileImage } from "@/shared/components/ProfileImage";
import { UserEditForm } from "./UserEditForm";
import { UserDeleteButton } from "./UserDeleteButton";
import { UserSummary } from "../UserSummary";

export function ProfileCard({ user }) {
  const authState = useAuthState();
  const [editMode, setEditMode] = useState(false);
  const [tempImage, setTempImage] = useState();
  
  const isLoggedInUser = !editMode && authState.id === user.id;

  const visibleUsername = authState.id === user.id ? authState.username : user.username;
  // Kendi profilimizde güncel bilgi auth state'te (düzenlemeden sonra sayfa yeniden yüklenmeden görünsün)
  const visibleBio = authState.id === user.id ? authState.bio : user.bio;

  return (
    <div className="card">
      <div className="card-header text-center">
        <ProfileImage width={200} tempImage={tempImage} image={user.image}/>
      </div>
      <div className="card-body text-center">
        {!editMode && <span className="fs-3 d-block">{visibleUsername}</span>}
        {!editMode && visibleBio && <p className="text-muted">{visibleBio}</p>}
        {!editMode && <UserSummary userId={user.id} />}
        {isLoggedInUser && (
          <>
            <Button onClick={() => setEditMode(true)}>Edit</Button>
            <div className="d-inline m-1"></div>
            <UserDeleteButton />
          </>
        )}
        {editMode && <UserEditForm setEditMode={setEditMode} setTempImage={setTempImage}/>}
      </div>
    </div>
  );
}
