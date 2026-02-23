package bakery.service;

import bakery.entity.Product;
import bakery.entity.ProductImage;
import bakery.entity.Recipe;
import bakery.repository.ProductRepository;
import bakery.service.ProductService;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final String UPLOAD_DIR = "uploads/";

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<Product> getAll() {
        return productRepository.findAll();
    }

    @Override
    public List<Product> getAllSorted(String sort) {
        if (sort == null || sort.isBlank()) {
            return productRepository.findAll();
        }
        return switch (sort) {
            case "price_asc" -> productRepository.findAll(Sort.by(Sort.Direction.ASC, "price"));
            case "price_desc" -> productRepository.findAll(Sort.by(Sort.Direction.DESC, "price"));
            default -> productRepository.findAll();
        };
    }

    @Override
    public Product getById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public void save(Product product, MultipartFile[] imageFiles, Long mainImageId, List<Long> deleteImageIds, String recipesJson) throws IOException {

        // 1. CHECK TRÙNG TÊN
        if (product.getName() != null && !product.getName().trim().isEmpty()) {
            String trimmedName = product.getName().trim();
            List<Product> existingProducts = productRepository.findByNameIgnoreCase(trimmedName);

            boolean isDuplicate = false;
            if (product.getProductId() == null) {
                isDuplicate = !existingProducts.isEmpty();
            } else {
                isDuplicate = existingProducts.stream()
                        .anyMatch(existing -> !existing.getProductId().equals(product.getProductId()));
            }

            if (isDuplicate) {
                throw new IllegalArgumentException("Tên bánh '" + trimmedName + "' đã tồn tại. Vui lòng chọn tên khác.");
            }
        }

        // === [MỚI] DỊCH CHUỖI JSON THÀNH DANH SÁCH RECIPE ===
        List<Recipe> parsedRecipes = new ArrayList<>();
        if (recipesJson != null && !recipesJson.trim().isEmpty() && !recipesJson.equals("[]")) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                parsedRecipes = mapper.readValue(recipesJson, new TypeReference<List<Recipe>>() {});
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Lỗi đọc dữ liệu công thức nguyên liệu!");
            }
        }
        Set<Long> ingredientIds = new HashSet<>();
        for (Recipe r : parsedRecipes) {
            // 1. Kiểm tra số lượng phải lớn hơn 0
            if (r.getAmount() == null || r.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Lỗi công thức: Định lượng nguyên liệu phải lớn hơn 0.");
            }

            // 2. Kiểm tra trùng lặp nguyên liệu
            Long ingId = r.getIngredient().getIngredientId();
            if (ingredientIds.contains(ingId)) {
                throw new IllegalArgumentException("Lỗi công thức: Có nguyên liệu bị chọn trùng lặp nhiều lần. Vui lòng gộp lại thành một dòng.");
            }
            ingredientIds.add(ingId);
        }

        Product savedProduct;

        // 2. LƯU SẢN PHẨM & CÔNG THỨC
        if (product.getProductId() != null) {
            // === CASE EDIT (CẬP NHẬT) ===
            Product existingProduct = getById(product.getProductId()); // Hàm getById của bạn
            if (existingProduct == null) {
                throw new RuntimeException("Product not found");
            }

            // Merge thông tin cơ bản
            existingProduct.setName(product.getName());
            existingProduct.setDescription(product.getDescription());
            existingProduct.setPrice(product.getPrice());
            existingProduct.setCategory(product.getCategory());

            // Cập nhật Recipes từ form JSON
            if (existingProduct.getRecipes() != null) {
                existingProduct.getRecipes().clear(); // Xóa sạch công thức cũ trong Database
            } else {
                existingProduct.setRecipes(new ArrayList<>());
            }

            // Thêm công thức mới vào
            for (Recipe recipe : parsedRecipes) {
                recipe.setProduct(existingProduct); // Bắt buộc gán cha
                existingProduct.getRecipes().add(recipe);
            }

            savedProduct = productRepository.save(existingProduct);

        } else {
            // === CASE ADD NEW (THÊM MỚI) ===
            for (Recipe recipe : parsedRecipes) {
                recipe.setProduct(product); // Bắt buộc gán cha
            }
            product.setRecipes(parsedRecipes);
            savedProduct = productRepository.save(product);
        }

        // 3. XỬ LÝ ẢNH (Giữ nguyên của bạn)
        if (savedProduct.getImages() == null) {
            savedProduct.setImages(new ArrayList<>());
        }

        if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
            savedProduct.getImages().removeIf(img -> img.getImageId() != null && deleteImageIds.contains(img.getImageId()));
        }

        productRepository.save(savedProduct);

        List<ProductImage> newImages = new ArrayList<>();
        if (imageFiles != null && imageFiles.length > 0) {
            // Đảm bảo UPLOAD_DIR đã được định nghĩa trong class (ví dụ: private static final String UPLOAD_DIR = "uploads/";)
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            for (MultipartFile file : imageFiles) {
                if (!file.isEmpty()) {
                    String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                    Path filePath = uploadPath.resolve(fileName);
                    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                    ProductImage newImage = new ProductImage();
                    newImage.setImageUrl("/uploads/" + fileName);
                    newImage.setIsMain(false);
                    newImage.setProduct(savedProduct);
                    newImages.add(newImage);
                }
            }
        }

        if (!newImages.isEmpty()) {
            savedProduct.getImages().addAll(newImages);
            productRepository.save(savedProduct);
        }

        savedProduct.getImages().forEach(img -> img.setIsMain(false));

        if (mainImageId != null) {
            savedProduct.getImages().stream()
                    .filter(img -> img.getImageId() != null && img.getImageId().equals(mainImageId))
                    .findFirst()
                    .ifPresent(img -> img.setIsMain(true));
        } else if (!savedProduct.getImages().isEmpty()) {
            savedProduct.getImages().get(0).setIsMain(true);
        }

        productRepository.save(savedProduct);
    }

    @Override
    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    @Override
    // ⚠️ QUAN TRỌNG: Kiểu trả về phải là Interface 'Page', KHÔNG để 'PageImpl'
    public Page<Product> filter(Long id, String keyword, String category, String sort, int page, int size) {

        // 1. Xử lý tìm theo ID (Sửa lại logic này để tránh lỗi Incompatible types)
        if (id != null) {
            return productRepository.findById(id)
                    .map(p -> {
                        // Tìm thấy -> Trả về Page chứa 1 sản phẩm
                        List<Product> list = List.of(p);
                        return (Page<Product>) new PageImpl<>(list);
                    })
                    .orElseGet(() -> {
                        // Không tìm thấy -> Trả về Page rỗng đúng kiểu Product
                        return Page.empty(PageRequest.of(0, size));
                    });
        }

        // 2. Xử lý Sắp xếp
        Sort sorting = Sort.unsorted();
        if ("price_asc".equals(sort)) {
            sorting = Sort.by("price").ascending();
        } else if ("price_desc".equals(sort)) {
            sorting = Sort.by("price").descending();
        } else {
            // Mặc định sắp xếp ID giảm dần
            sorting = Sort.by("productId").descending();
        }

        // 3. Tạo Pageable
        Pageable pageable = PageRequest.of(page, size, sorting);

        // 4. Gọi Repository
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasCategory = category != null && !category.isBlank();

        if (hasKeyword && hasCategory) {
            return productRepository.findByNameContainingIgnoreCaseAndCategory(keyword, category, pageable);
        } else if (hasKeyword) {
            return productRepository.findByNameContainingIgnoreCase(keyword, pageable);
        } else if (hasCategory) {
            return productRepository.findByCategory(category, pageable);
        } else {
            return productRepository.findAll(pageable);
        }
    }
    @Override
    @Transactional
    public void saveQuickInfo(Product product) {
        // Hàm này chỉ lưu Product thuần túy (không đụng vào Ảnh hay Recipes)
        // Spring Data JPA sẽ tự động nhận diện thay đổi và update Database
        productRepository.save(product);
    }


}
