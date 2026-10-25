import { requestAdmin } from "../http/adminSession.js";
import { queryString } from "../http/query.js";

const PATH = "/api/admin/statistics";

/** API báo cáo thống kê. Bỏ trống storeId: Global xem toàn hệ thống, nhân viên cửa hàng xem cửa hàng mình. */
export const statisticsApi = {
  STORES_PATH: `${PATH}/stores`,
  report: ({ storeId, period, orderType }, options = {}) =>
    requestAdmin(`${PATH}${queryString({ storeId, period, orderType })}`, options),
};
