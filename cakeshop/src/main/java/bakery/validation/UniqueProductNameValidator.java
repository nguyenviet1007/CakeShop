package bakery.validation;

import bakery.entity.Product;
import bakery.repository.ProductRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component  // <-- Thêm dòng này để Spring biết và quản lý bean
public class UniqueProductNameValidator implements ConstraintValidator<UniqueProductName, Product> {

    @Autowired
    private ProductRepository productRepository;

    @Override
    public boolean isValid(Product product, ConstraintValidatorContext context) {
        if (product == null || product.getName() == null || product.getName().isBlank()) {
            return true;
        }

        // Tìm sản phẩm khác có cùng tên (ignore case)
        return productRepository.findByNameIgnoreCase(product.getName())
                .stream()
                .noneMatch(p -> !p.getProductId().equals(product.getProductId()));
    }
}
