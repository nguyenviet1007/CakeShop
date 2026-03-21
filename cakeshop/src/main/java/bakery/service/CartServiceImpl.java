package bakery.service;


import bakery.entity.Cart;
import bakery.entity.DailyStock;
import bakery.entity.Product;
import bakery.entity.User;
import bakery.repository.CartRepository;
import bakery.repository.DailyStockRepository;
import bakery.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CartServiceImpl {
    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DailyStockRepository dailyStockRepository;

    // Lấy danh sách để đổ vào Mockup
    public List<Cart> findByUserId(Long userId) {
        return cartRepository.findByUserId(userId);
    }

    // Xóa sản phẩm khỏi giỏ hàng
    public void removeFromCart(Long cartId) {
        if (!cartRepository.existsById(cartId)) {
            throw new RuntimeException("Không tìm thấy sản phẩm trong giỏ để xóa");
        }
        cartRepository.deleteById(cartId);
    }

    // Xóa sạch giỏ hàng
    public void clearCart(Long userId) {
        cartRepository.deleteByUserId(userId);
    }

    // Logic cho icon Giỏ hàng ở trang chủ
    public void addToCart(Long userId, Long productId, Integer quantity) {
        // Kiểm tra userId không được null
        if (userId == null) {
            throw new IllegalArgumentException("User ID không được để trống");
        }

        // Kiểm tra quantity phải lớn hơn 0
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Số lượng phải là số dương lớn hơn 0");
        }

        // Kiểm tra sản phẩm có tồn tại không
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        DailyStock stock = dailyStockRepository.findByProductAndDate(product, LocalDate.now())
                .orElseThrow(() -> new RuntimeException("Sản phẩm hiện không có trong kho"));

        // Tìm xem trong giỏ đã có sản phẩm này chưa
        Optional<Cart> existingCart = cartRepository.findByUserIdAndProductProductId(userId, productId);

        if (existingCart.isPresent()) {
            // Nếu có rồi thì cộng dồn số lượng
            Cart cart = existingCart.get();
            cart.setQuantity(cart.getQuantity() + quantity);
            cartRepository.save(cart);
        } else {
            // Nếu chưa có thì tạo mới record
            Cart newCart = new Cart();

            // Thiết lập User
            User user = new User();
            user.setId(userId);

            newCart.setUser(user);
            newCart.setProduct(product);
            newCart.setQuantity(quantity);

            // Lấy % giảm giá, nếu null thì mặc định là 0 để tránh lỗi
            int discountPercent = (product.getDiscountPercent() != null) ? product.getDiscountPercent() : 0;

            // Tính hệ số nhân
            BigDecimal multiplier = BigDecimal.valueOf(100 - discountPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            // Lưu giá tại thời điểm thêm vào
            newCart.setPrice(product.getPrice().multiply(multiplier));

            cartRepository.save(newCart);
        }
    }
    // Logic cho nút + và - trong mockup giỏ hàng
    public void updateQuantity(Long cartId, Integer newQuantity) {
        if (newQuantity <= 0) {
            cartRepository.deleteById(cartId);
            return;
        }

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

        // Kiểm tra tồn kho thực tế
        DailyStock stock = dailyStockRepository.findByProductAndDate(cart.getProduct(), LocalDate.now())
                .orElseThrow(() -> new RuntimeException("Sản phẩm hiện không có trong kho"));

        if (stock.getAvailableQuantity() < newQuantity) {
            throw new RuntimeException("Kho chỉ còn " + stock.getAvailableQuantity() + " sản phẩm");
        }

        cart.setQuantity(newQuantity);
        cartRepository.save(cart);
    }

    public List<Cart> getCartItems(Long userId) {
        return cartRepository.findByUserId(userId);
    }
}