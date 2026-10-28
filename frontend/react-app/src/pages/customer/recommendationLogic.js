import { orderCards } from "./wishlistLogic.js";

/** Id của hai nhóm gợi ý, bỏ trùng, để lấy thẻ sản phẩm trong một lần gọi. */
export function recommendationIds(result) {
  return [...new Set([...(result?.boughtTogether || []), ...(result?.similar || [])])];
}

/** Chia thẻ đã lấy về lại hai nhóm, giữ thứ tự gợi ý và bỏ sản phẩm không còn hiển thị. */
export function splitRecommendations(result, cards = []) {
  return {
    boughtTogether: orderCards(cards, result?.boughtTogether || []),
    similar: orderCards(cards, result?.similar || []),
  };
}
