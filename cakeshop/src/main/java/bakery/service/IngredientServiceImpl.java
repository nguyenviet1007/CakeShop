package bakery.service;

import bakery.dto.request.IngredientDTO;
import bakery.entity.Ingredient;
import bakery.entity.IngredientStock;
import bakery.entity.IngredientStockHistory;
import bakery.entity.StockIn;
import bakery.repository.IngredientRepository;
import bakery.repository.IngredientStockHistoryRepository;
import bakery.repository.IngredientStockRepository;
import bakery.repository.StockInRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class IngredientServiceImpl implements IngredientService {

    private final IngredientRepository ingredientRepo;
    private final IngredientStockRepository stockRepo;
    private final IngredientStockHistoryRepository historyRepo;
    private final StockInRepository stockInRepo; // THÊM DÒNG NÀY

    public IngredientServiceImpl(IngredientRepository ingredientRepo,
                                 IngredientStockRepository stockRepo,
                                 IngredientStockHistoryRepository historyRepo,
                                 StockInRepository stockInRepo) { // THÊM VÀO ĐÂY
        this.ingredientRepo = ingredientRepo;
        this.stockRepo = stockRepo;
        this.historyRepo = historyRepo;
        this.stockInRepo = stockInRepo; // THÊM VÀO ĐÂY
    }

    @Override
    public List<Ingredient> findAll() {
        return ingredientRepo.findAll();
    }

    @Override
    public Ingredient findById(Long id) {
        return ingredientRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nguyên liệu id: " + id));
    }

    @Override
    public void deleteById(Long id) {
        // 1. Xóa tất cả lịch sử kho liên quan đến nguyên liệu này trước
        historyRepo.deleteByIngredientId(id);

        // 2. Sau đó mới xóa nguyên liệu (Entity Ingredient có CascadeType.ALL với Stock nên Stock sẽ tự bay theo)
        ingredientRepo.deleteById(id);
    }

    // --- 1. TẠO MỚI ---
    @Override
    public void importNewIngredient(String name, String unit, BigDecimal minQuantity) {
        if (minQuantity == null || minQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ngưỡng cảnh báo phải lớn hơn 0");
        }
        if (ingredientRepo.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Tên nguyên liệu '" + name + "' đã tồn tại!");
        }

        Ingredient ing = new Ingredient();
        ing.setName(name);
        ing.setUnit(unit);
        ingredientRepo.save(ing);

        IngredientStock stock = new IngredientStock();
        stock.setIngredient(ing);
        stock.setQuantity(BigDecimal.ZERO);
        stock.setMinQuantity(minQuantity);
        stockRepo.save(stock);

        // Lưu lịch sử
        createHistoryRecord(ing.getIngredientId(), BigDecimal.ZERO, BigDecimal.ZERO, minQuantity, minQuantity, "Khởi tạo nguyên liệu mới");
    }

    // --- 2. CẬP NHẬT THÔNG TIN ---
    @Override
    public void updateIngredient(Ingredient ingredient) {
        // 1. Tìm bản ghi gốc đang có trong Database (với số lượng tồn kho đúng)
        Ingredient existing = ingredientRepo.findById(ingredient.getIngredientId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nguyên liệu"));

        IngredientStock stock = existing.getStock();

        // 2. Lưu lại giá trị cũ để ghi lịch sử
        BigDecimal oldQty = stock.getQuantity();
        BigDecimal oldMin = stock.getMinQuantity();

        // 3. Validate tên (giữ nguyên logic của bạn)
        if (!existing.getName().equalsIgnoreCase(ingredient.getName())) {
            if (ingredientRepo.existsByNameIgnoreCase(ingredient.getName())) {
                throw new IllegalArgumentException("Tên nguyên liệu '" + ingredient.getName() + "' đã tồn tại!");
            }
        }

        // 4. CHỐT CHẶN TẠI ĐÂY: Chỉ cập nhật Name và Unit
        existing.setName(ingredient.getName());
        existing.setUnit(ingredient.getUnit());

        // 5. CHỐT CHẶN KHO: Chỉ cập nhật minQuantity nếu có gửi lên
        BigDecimal newMinQuantity = oldMin;
        if (ingredient.getStock() != null && ingredient.getStock().getMinQuantity() != null) {
            newMinQuantity = ingredient.getStock().getMinQuantity();
            if (newMinQuantity.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Ngưỡng cảnh báo không được nhỏ hơn 0!");
            }
        }

        // QUAN TRỌNG: Tuyệt đối không lấy quantity từ object 'ingredient' truyền vào
        // newQuantity LUÔN LUÔN = oldQty (Giá trị hiện tại trong DB)
        BigDecimal newQuantity = oldQty;

        // 6. Cập nhật vào đối tượng quản lý bởi Hibernate
        stock.setMinQuantity(newMinQuantity);
        stock.setQuantity(newQuantity); // Đảm bảo ghi lại số lượng cũ, không phải 0

        // 7. Lưu (Hibernate sẽ chỉ update những gì thay đổi)
        ingredientRepo.save(existing);
        stockRepo.save(stock);

        // 8. Lưu lịch sử
        createHistoryRecord(existing.getIngredientId(), oldQty, newQuantity, oldMin, newMinQuantity, "Cập nhật giá trị MIN");
    }

    // --- 3. NHẬP KHO (Import Stock) ---
    // Trong IngredientServiceImpl.java

    @Override
    @Transactional
    public void importStock(List<IngredientDTO> importList) {
        for (IngredientDTO dto : importList) {
            IngredientStock stock = stockRepo.findById(dto.getIngredientId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy kho cho ID: " + dto.getIngredientId()));

            BigDecimal oldQty = stock.getQuantity();
            BigDecimal rate = (dto.getConversionRate() != null) ? dto.getConversionRate() : BigDecimal.ONE;
            BigDecimal inputQty = (dto.getQuantityInput() != null) ? dto.getQuantityInput() : dto.getQuantity();
            BigDecimal addedQuantity = inputQty.multiply(rate);
            BigDecimal newQty = oldQty.add(addedQuantity);

            // 1. Cập nhật bảng tổng kho
            stock.setQuantity(newQty);
            stockRepo.save(stock);

            // 2. LƯU CHI TIẾT PHIẾU NHẬP (Để nhấn nút icon tờ hóa đơn nó hiện ra)
            StockIn stockIn = new StockIn();
            stockIn.setIngredient(stock.getIngredient()); // Lấy object Ingredient từ stock
            stockIn.setQuantityInput(inputQty);
            stockIn.setUnitName(dto.getUnitName());
            stockIn.setConversionRate(rate);
            stockIn.setQuantityBase(addedQuantity);
            stockIn.setNote(dto.getNote());
            stockIn.setCreatedAt(LocalDateTime.now());
            stockInRepo.save(stockIn); // LƯU XUỐNG DB TẠI ĐÂY

            // 3. Lưu lịch sử tổng quát (Sửa lại để không mất Min)
            createHistoryRecord(
                    dto.getIngredientId(),
                    oldQty,
                    newQty,
                    stock.getMinQuantity(), // Lấy Min hiện tại từ DB
                    stock.getMinQuantity(), // Mới nhập hàng nên Min giữ nguyên
                    "Nhập " + inputQty + " " + dto.getUnitName() + " (Quy đổi: " + rate + ")"
            );
        }
    }

    // --- 4. XUẤT KHO THỦ CÔNG (Export Stock - MỚI THÊM) ---
    @Override
    public void exportStock(List<IngredientDTO> exportList) {
        for (IngredientDTO dto : exportList) {
            IngredientStock stock = stockRepo.findById(dto.getIngredientId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy dữ liệu kho ID: " + dto.getIngredientId()));

            BigDecimal currentQty = stock.getQuantity();
            BigDecimal exportQty = dto.getQuantity();

            // Kiểm tra tồn kho
            if (currentQty.compareTo(exportQty) < 0) {
                throw new RuntimeException("Lỗi: Nguyên liệu '" + stock.getIngredient().getName() +
                        "' chỉ còn " + currentQty + ". Không đủ để xuất " + exportQty);
            }

            BigDecimal newQty = currentQty.subtract(exportQty);
            String userNote = dto.getNote() != null ? dto.getNote() : "Xuất kho thủ công";

            // Cập nhật kho
            stock.setQuantity(newQty);
            stock.setUpdatedAt(LocalDateTime.now());
            stockRepo.save(stock);

            // Lưu lịch sử
            createHistoryRecord(dto.getIngredientId(), currentQty, newQty, stock.getMinQuantity(), stock.getMinQuantity(), userNote);
        }
    }

    // --- 5. XUẤT KHO LẺ (Hàm cũ - Giữ lại cho Auto Export sau này) ---
    @Override
    public void exportIngredient(Long ingredientId, BigDecimal amount) throws Exception {
        Ingredient ing = findById(ingredientId);
        IngredientStock stock = ing.getStock();

        if (stock == null) throw new Exception("Không có thông tin tồn kho");
        if (stock.getQuantity().compareTo(amount) < 0) throw new Exception("Không đủ tồn kho");

        BigDecimal oldQty = stock.getQuantity();
        BigDecimal newQty = oldQty.subtract(amount);

        stock.setQuantity(newQty);
        stock.setUpdatedAt(LocalDateTime.now());
        stockRepo.save(stock);

        createHistoryRecord(ingredientId, oldQty, newQty, stock.getMinQuantity(), stock.getMinQuantity(), "Xuất kho tự động");
    }

    // === HÀM PHỤ TRỢ (Helper) ĐỂ GHI LỊCH SỬ ===
    private void createHistoryRecord(Long ingId, BigDecimal oldQty, BigDecimal newQty,
                                     BigDecimal oldMin, BigDecimal newMin, String note) {
        IngredientStockHistory history = new IngredientStockHistory();
        history.setIngredientId(ingId);
        history.setOldQuantity(oldQty);
        history.setNewQuantity(newQty);
        history.setOldMinQuantity(oldMin);
        history.setNewMinQuantity(newMin);
        history.setNote(note);
        history.setUpdatedBy("Admin");
        history.setUpdatedAt(LocalDateTime.now());
        historyRepo.save(history);
    }
    @Override
    public Page<Ingredient> filterAndPaginate(String keyword, String status, int page, int size) {
        List<Ingredient> list;

        // SỬA: Dùng 'ingredientRepo' thay vì 'ingredientRepository'
        if (keyword != null && !keyword.isBlank()) {
            list = ingredientRepo.searchByKeyword(keyword.trim());
        } else {
            list = ingredientRepo.findAll();
        }

        // --- Phần lọc status giữ nguyên ---
        if (status != null && !status.isEmpty() && !status.equals("all")) {
            list = list.stream().filter(i -> {
                if (i.getStock() == null) return false;

                boolean isOut = i.getStock().getQuantity().compareTo(BigDecimal.ZERO) <= 0;
                boolean isLow = i.getStock().getQuantity().compareTo(i.getStock().getMinQuantity()) <= 0 && !isOut;

                if ("out".equals(status)) return isOut;
                if ("warning".equals(status)) return isLow;
                if ("ok".equals(status)) return !isOut && !isLow;
                return true;
            }).collect(Collectors.toList());
        }

        // --- Phần phân trang thủ công giữ nguyên ---
        Pageable pageable = PageRequest.of(page, size);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());

        List<Ingredient> pageContent;
        if (start > list.size()) {
            pageContent = Collections.emptyList();
        } else {
            pageContent = list.subList(start, end);
        }

        return new PageImpl<>(pageContent, pageable, list.size());
    }

}
