import { requestAdmin } from "../http/adminSession.js";
import { queryString } from "../http/query.js";

const PATH = "/api/admin/dashboard";

/** API trang tổng quan. Bỏ trống storeId: Global xem toàn hệ thống, nhân viên cửa hàng xem cửa hàng mình. */
export const dashboardApi = {
  STORES_PATH: `${PATH}/stores`,
  overview: (storeId, options = {}) => requestAdmin(`${PATH}${queryString({ storeId })}`, options),
};
