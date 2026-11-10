package com.fashionsystem.fashion_system.marketing;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Soạn email khuyến mãi. Mọi email có lý do khách nhận được và link hủy đăng ký một bước (trang hủy của cửa hàng online,
 * xác thực bằng token riêng của khách) — yêu cầu của NĐ 91/2020 với thư điện tử quảng cáo. Nội dung do nhân viên nhập được
 * escape HTML.
 */
@Component
class MarketingEmailComposer {
    private final String siteUrl;

    MarketingEmailComposer(@Value("${marketing.site-url:http://localhost:5173}") String siteUrl) {
        this.siteUrl = siteUrl.endsWith("/") ? siteUrl.substring(0, siteUrl.length() - 1) : siteUrl;
    }

    record Email(String subject, String html) {}

    String unsubscribeUrl(UUID token) {
        return siteUrl + "/unsubscribe?token=" + token;
    }

    Email compose(String title, String message, String actionUrl, UUID unsubscribeToken) {
        String safeTitle = HtmlUtils.htmlEscape(title);
        String safeMessage = HtmlUtils.htmlEscape(message).replace("\n", "<br>");
        String button = actionUrl == null ? "" : """
                <p style="margin:24px 0"><a href="%s" style="background:#b45309;color:#fff;padding:12px 20px;border-radius:8px;\
                text-decoration:none;font-weight:600">Xem ưu đãi</a></p>""".formatted(HtmlUtils.htmlEscape(siteUrl + actionUrl));
        String html = """
                <!doctype html><html lang="vi"><body style="margin:0;background:#f7f3ee;font-family:Arial,sans-serif;color:#27272a">
                <div style="max-width:560px;margin:0 auto;padding:24px">
                <p style="letter-spacing:3px;font-size:12px;color:#92400e;margin:0 0 16px">LUNARIA BOUTIQUE</p>
                <div style="background:#fff;border-radius:16px;padding:24px">
                <h1 style="font-size:20px;margin:0 0 12px">%s</h1>
                <p style="font-size:15px;line-height:1.6;margin:0">%s</p>
                %s
                </div>
                <p style="font-size:12px;color:#71717a;line-height:1.5;margin-top:16px">Bạn nhận email này vì đã đồng ý nhận tin \
                khuyến mãi từ Lunaria trong tài khoản của mình. <a href="%s" style="color:#92400e">Hủy đăng ký nhận email</a> \
                bất cứ lúc nào; bạn vẫn nhận email về đơn hàng.</p>
                </div></body></html>""".formatted(safeTitle, safeMessage, button, HtmlUtils.htmlEscape(unsubscribeUrl(unsubscribeToken)));
        return new Email(title, html);
    }
}
