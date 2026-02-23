package bakery.service;

import bakery.dto.request.IngredientDTO;
import bakery.entity.Ingredient;
import bakery.entity.IngredientStock;
import bakery.entity.IngredientStockHistory;
import bakery.repository.IngredientRepository;
import bakery.repository.IngredientStockHistoryRepository;
import bakery.repository.IngredientStockRepository;
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


    public IngredientServiceImpl(IngredientRepository ingredientRepo,
                                 IngredientStockRepository stockRepo,
                                 IngredientStockHistoryRepository historyRepo) {
        this.ingredientRepo = ingredientRepo;
        this.stockRepo = stockRepo;
        this.historyRepo = historyRepo;
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
        Ingredient existing = findById(ingredient.getIngredientId());
        IngredientStock stock = existing.getStock();

        // Validate
        if (ingredient.getStock() != null && ingredient.getStock().getMinQuantity() != null) {
            if (ingredient.getStock().getMinQuantity().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Ngưỡng cảnh báo không được nhỏ hơn 0!");
            }
        }
        if (!existing.getName().equalsIgnoreCase(ingredient.getName())) {
            if (ingredientRepo.existsByNameIgnoreCase(ingredient.getName())) {
                throw new IllegalArgumentException("Tên nguyên liệu '" + ingredient.getName() + "' đã được sử dụng!");
            }
        }

        // Tính toán giá trị mới
        BigDecimal oldQty = stock.getQuantity();
        BigDecimal oldMin = stock.getMinQuantity();

        BigDecimal newQuantity = (ingredient.getStock() != null && ingredient.getStock().getQuantity() != null)
                ? ingredient.getStock().getQuantity() : oldQty;
        BigDecimal newMinQuantity = (ingredient.getStock() != null && ingredient.getStock().getMinQuantity() != null)
                ? ingredient.getStock().getMinQuantity() : (oldMin != null ? oldMin : BigDecimal.TEN);

        // Cập nhật DB
        existing.setName(ingredient.getName());
        existing.setUnit(ingredient.getUnit());
        stock.setQuantity(newQuantity);
        stock.setMinQuantity(newMinQuantity);

        ingredientRepo.save(existing);
        stockRepo.save(stock);

        // Lưu lịch sử
        createHistoryRecord(existing.getIngredientId(), oldQty, newQuantity, oldMin, newMinQuantity, "Cập nhật thông tin admin");
    }

    // --- 3. NHẬP KHO (Import Stock) ---
    @Override
    public void importStock(List<IngredientDTO> importList) {
        for (IngredientDTO dto : importList) {
            IngredientStock stock = stockRepo.findById(dto.getIngredientId())
                    .orElseThrow(() -> new RuntimeException("Chưa có dữ liệu kho cho ID: " + dto.getIngredientId()));

            BigDecimal oldQty = stock.getQuantity();
            BigDecimal importQty = dto.getQuantity();

            if (importQty == null || importQty.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal newQty = oldQty.add(importQty);

            // Cập nhật kho
            stock.setQuantity(newQty);
            stock.setUpdatedAt(LocalDateTime.now());
            stockRepo.save(stock);

            // Lưu lịch sử
            String note = "Nhập kho: +" + importQty + " " + stock.getIngredient().getUnit();
            createHistoryRecord(dto.getIngredientId(), oldQty, newQty, stock.getMinQuantity(), stock.getMinQuantity(), note);
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
