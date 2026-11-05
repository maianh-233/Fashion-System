import { PublicApiError, requestPublic } from "../http/publicRequest.js";
import { API_BASE_URL } from "../http/query.js";

const PATH = "/api/public/careers";

/** Trang tuyển dụng công khai: vị trí đang tuyển và nộp hồ sơ (không cần đăng nhập). */
export const careersApi = {
  positions: ({ keyword, storeId } = {}, options = {}) => {
    const query = new URLSearchParams();
    if (keyword?.trim()) query.set("keyword", keyword.trim());
    if (storeId) query.set("storeId", storeId);
    const text = query.toString();
    return requestPublic(`${PATH}/positions${text ? `?${text}` : ""}`, options);
  },
  position: (code, options = {}) => requestPublic(`${PATH}/positions/${encodeURIComponent(code)}`, options),
  /** form: FormData gồm các trường hồ sơ + file cv (bắt buộc), coverLetter, portfolio. */
  apply: async (code, form) => {
    let response;
    try {
      response = await fetch(`${API_BASE_URL}${PATH}/positions/${encodeURIComponent(code)}/apply`, {
        method: "POST", body: form, headers: { Accept: "application/json" },
      });
    } catch {
      throw new PublicApiError("Không kết nối được máy chủ. Vui lòng thử lại.", 0);
    }
    const data = await response.json().catch(() => null);
    if (!response.ok) throw new PublicApiError(data?.message || "Không gửi được hồ sơ, vui lòng thử lại.", response.status);
    return data;
  },
};
