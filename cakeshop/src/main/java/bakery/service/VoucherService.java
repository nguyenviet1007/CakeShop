package bakery.service;

import bakery.entity.Voucher;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public interface VoucherService {
    Page<Voucher> getVouchersPaginated(String keyword, Boolean active,
                                       Integer minPct, Integer maxPct,
                                       BigDecimal minMaxAmt, BigDecimal maxMaxAmt,
                                       BigDecimal minOrderFrom, BigDecimal minOrderTo,
                                       java.time.LocalDateTime expireFrom, java.time.LocalDateTime expireTo,
                                       int page, int size);
    Voucher saveVoucher(Voucher voucher);
    void deleteVoucher(Long id);
    void toggleStatus(Long id);
    Voucher getById(Long id);
}