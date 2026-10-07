import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Alert } from "@/shared/components/Alert";
import { Button } from "@/shared/components/Button";
import { createHoax } from "./hoaxApi";

/** "java, #spring react" -> ["java", "#spring", "react"] */
function parseTags(text) {
  return text
    .split(/[\s,]+/)
    .map((tag) => tag.trim())
    .filter(Boolean);
}

/**
 * Backend etiket hatalarını "tags" veya "tags[0]" gibi anahtarlarla döner;
 * hepsini tek bir "tags" alan hatasına topluyoruz.
 */
function toFieldErrors(validationErrors = {}) {
  const errors = {};
  Object.entries(validationErrors).forEach(([field, message]) => {
    const key = field.startsWith("tags") ? "tags" : field;
    errors[key] = errors[key] || message;
  });
  return errors;
}

export function HoaxSubmit({ onSuccess }) {
  const { t } = useTranslation();
  const [content, setContent] = useState("");
  const [tags, setTags] = useState("");
  const [apiProgress, setApiProgress] = useState(false);
  const [errors, setErrors] = useState({});
  const [generalError, setGeneralError] = useState();
  const [success, setSuccess] = useState(false);

  const onSubmit = async (event) => {
    event.preventDefault();
    setApiProgress(true);
    setErrors({});
    setGeneralError();
    setSuccess(false);
    try {
      await createHoax({ content, tags: parseTags(tags) });
      setContent("");
      setTags("");
      setSuccess(true);
      onSuccess();
    } catch (axiosError) {
      if (axiosError.response?.data?.status === 400) {
        setErrors(toFieldErrors(axiosError.response.data.validationErrors));
      } else {
        setGeneralError(axiosError.response?.data?.message || t("genericError"));
      }
    } finally {
      setApiProgress(false);
    }
  };

  return (
    <form className="card mb-3" onSubmit={onSubmit}>
      <div className="card-body">
        <div className="mb-2">
          <textarea
            className={errors.content ? "form-control is-invalid" : "form-control"}
            rows={3}
            placeholder={t("hoaxPlaceholder")}
            value={content}
            onChange={(event) => {
              setContent(event.target.value);
              setErrors((last) => ({ ...last, content: undefined }));
              setSuccess(false);
            }}
          />
          <div className="invalid-feedback">{errors.content}</div>
        </div>
        <div className="mb-2">
          <input
            className={errors.tags ? "form-control is-invalid" : "form-control"}
            placeholder={t("tagsPlaceholder")}
            value={tags}
            onChange={(event) => {
              setTags(event.target.value);
              setErrors((last) => ({ ...last, tags: undefined }));
            }}
          />
          <div className="invalid-feedback">{errors.tags}</div>
        </div>
        {generalError && <Alert styleType="danger">{generalError}</Alert>}
        {success && <Alert>{t("hoaxCreated")}</Alert>}
        <div className="text-end">
          <small className="text-muted me-2">{content.length}/1000</small>
          <Button apiProgress={apiProgress} disabled={!content.trim()} type="submit">
            {t("share")}
          </Button>
        </div>
      </div>
    </form>
  );
}
