package com.fashionsystem.fashion_system.payroll;

import com.fashionsystem.fashion_system.payroll.PayrollDtos.RecordDetail;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.RecordRow;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Phiếu lương cá nhân: mọi nhân viên đăng nhập, chỉ phiếu của chính mình và đã phát hành. */
@RestController
@RequestMapping("/api/payslips")
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class PayslipController {
    private final PayrollAccess access;
    private final PayslipService payslips;

    public PayslipController(PayrollAccess access, PayslipService payslips) {
        this.access = access;
        this.payslips = payslips;
    }

    @GetMapping
    public List<RecordRow> mine() {
        return payslips.mine(access.currentUserId());
    }

    @GetMapping("/{id}")
    public RecordDetail detail(@PathVariable UUID id) {
        return payslips.mineDetail(access.currentUserId(), id);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        return PayrollController.file(payslips.minePdf(access.currentUserId(), id), "phieu-luong.pdf", MediaType.APPLICATION_PDF);
    }
}
