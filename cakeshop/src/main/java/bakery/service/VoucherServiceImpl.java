package bakery.service;

import bakery.entity.Voucher;
import bakery.repository.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class VoucherServiceImpl implements VoucherService {

    @Autowired
    private VoucherRepository voucherRepository;

    @Override
    public Page<Voucher> getVouchersPaginated(String keyword, Boolean active,
                                              Integer minPct, Integer maxPct,
                                              BigDecimal minMaxAmt, BigDecimal maxMaxAmt,
                                              BigDecimal minOrderFrom, BigDecimal minOrderTo,
                                              LocalDateTime expireFrom, LocalDateTime expireTo,
                                              int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        String kw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        return voucherRepository.advancedSearch(kw, active, minPct, maxPct, minMaxAmt, maxMaxAmt,
                minOrderFrom, minOrderTo, expireFrom, expireTo, pageable);
    }

    @Override
    @Transactional
    public Voucher saveVoucher(Voucher voucher) {
        // Form gửi lên là chữ thường, ta ép thành chữ IN HOA cho đẹp (VD: sale50 -> SALE50)
        voucher.setCode(voucher.getCode().trim().toUpperCase());

        if (voucher.getId() == null) { // THÊM MỚI
            if (voucherRepository.existsByCodeIgnoreCase(voucher.getCode())) {
                throw new IllegalArgumentException("Mã Voucher '" + voucher.getCode() + "' đã tồn tại!");
            }
        } else { // CẬP NHẬT
            Voucher existing = voucherRepository.findById(voucher.getId()).orElseThrow();
            // Nếu đổi mã code, kiểm tra xem mã mới có bị trùng không
            if (!existing.getCode().equalsIgnoreCase(voucher.getCode()) &&
                    voucherRepository.existsByCodeIgnoreCase(voucher.getCode())) {
                throw new IllegalArgumentException("Mã Voucher '" + voucher.getCode() + "' đã tồn tại!");
            }
        }
        return voucherRepository.save(voucher);
    }

    @Override
    @Transactional
    public void deleteVoucher(Long id) {
        voucherRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        Voucher v = voucherRepository.findById(id).orElseThrow();
        v.setActive(!v.getActive());
        voucherRepository.save(v);
    }

    @Override
    public Voucher getById(Long id) {
        return voucherRepository.findById(id).orElse(null);
    }
}