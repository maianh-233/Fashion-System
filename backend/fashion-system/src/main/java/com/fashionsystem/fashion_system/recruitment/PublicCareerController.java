package com.fashionsystem.fashion_system.recruitment;

import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.PublicApplyResult;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.PublicPosition;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Trang tuyển dụng công khai (không cần đăng nhập). */
@RestController
@RequestMapping("/api/public/careers")
public class PublicCareerController {
    private final PublicCareerService careers;

    public PublicCareerController(PublicCareerService careers) {
        this.careers = careers;
    }

    @GetMapping("/positions")
    public List<PublicPosition> positions(@RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) UUID storeId) {
        return careers.positions(keyword, storeId);
    }

    @GetMapping("/positions/{code}")
    public PublicPosition position(@PathVariable String code) {
        return careers.position(code);
    }

    @PostMapping(value = "/positions/{code}/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PublicApplyResult apply(@PathVariable String code,
                                   @RequestParam String fullName, @RequestParam String email, @RequestParam String phone,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
                                   @RequestParam(required = false) String gender,
                                   @RequestParam(required = false) String address,
                                   @RequestParam(required = false) String currentTitle,
                                   @RequestParam(required = false) String currentCompany,
                                   @RequestParam(required = false) BigDecimal yearsExperience,
                                   @RequestParam(required = false) String skills,
                                   @RequestParam(required = false) String education,
                                   @RequestParam(required = false) String linkedinUrl,
                                   @RequestParam(required = false) String portfolioUrl,
                                   @RequestParam(required = false) String coverNote,
                                   @RequestParam(required = false) String source,
                                   @RequestParam(defaultValue = "false") Boolean consent,
                                   @RequestPart("cv") MultipartFile cv,
                                   @RequestPart(value = "coverLetter", required = false) MultipartFile coverLetter,
                                   @RequestPart(value = "portfolio", required = false) MultipartFile portfolio,
                                   HttpServletRequest request) {
        List<String> skillList = skills == null ? List.of()
                : Arrays.stream(skills.split(",")).map(String::trim).filter(s -> !s.isEmpty() && s.length() <= 60).limit(30).toList();
        var form = new PublicCareerService.ApplyForm(fullName, email, phone, dateOfBirth, limit(gender, 20), limit(address, 500),
                limit(currentTitle, 200), limit(currentCompany, 200),
                yearsExperience == null || yearsExperience.signum() < 0 || yearsExperience.compareTo(BigDecimal.valueOf(99)) > 0 ? null : yearsExperience,
                skillList, limit(education, 500), limit(linkedinUrl, 500), limit(portfolioUrl, 500), limit(coverNote, 4000),
                source, consent);
        return careers.apply(code, form, cv, coverLetter, portfolio, request.getRemoteAddr());
    }

    private static String limit(String value, int max) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }
}
