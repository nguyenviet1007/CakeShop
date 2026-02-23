package bakery.service;

import bakery.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ProductService {
    List<Product> getAll();
    List<Product> getAllSorted(String sort);
    Product getById(Long id);
    void save(Product product, MultipartFile[] imageFiles, Long mainImageId, List<Long> deleteImageIds, String recipesJson) throws IOException;
    void delete(Long id);
    Page<Product> filter(Long id, String keyword, String category, String sort, int page, int size);
    void saveQuickInfo(Product product);

}
