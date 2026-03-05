package bakery.service;


import bakery.entity.Cart;
import bakery.entity.Product;
import bakery.entity.User;
import bakery.repository.CartRepository;
import bakery.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CartServiceImpl {
    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

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

    // Xóa sạch giỏ hàng (Gọi từ OrderService sau khi lưu Order thành công)
    public void clearCart(Long userId) {
        cartRepository.deleteByUserId(userId);
    }

    // Logic cho icon Giỏ hàng ở trang chủ
    public void addToCart(Long userId, Long productId, Integer quantity) {
        // 1. Kiểm tra userId không được null
        if (userId == null) {
            throw new IllegalArgumentException("User ID không được để trống");
        }

        // 2. Kiểm tra quantity phải lớn hơn 0
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Số lượng phải là số dương lớn hơn 0");
        }

        // 3. Kiểm tra sản phẩm có tồn tại không
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        // 4. Tìm xem trong giỏ đã có sản phẩm này chưa
        Optional<Cart> existingCart = cartRepository.findByUserIdAndProductProductId(userId, productId);

        if (existingCart.isPresent()) {
            // Nếu có rồi thì cộng dồn số lượng
            Cart cart = existingCart.get();
            cart.setQuantity(cart.getQuantity() + quantity);
            cartRepository.save(cart);
        } else {
            // Nếu chưa có thì tạo mới record
            Cart newCart = new Cart();

            // Thiết lập User (Chỉ cần ID để mapping Hibernate)
            User user = new User();
            user.setId(userId);

            newCart.setUser(user);
            newCart.setProduct(product);
            newCart.setQuantity(quantity);

            // Lưu giá tại thời điểm thêm vào (Tránh lỗi ép kiểu nếu dùng Double)
            newCart.setPrice(product.getPrice());

            cartRepository.save(newCart);
        }
    }

    // Logic cho nút + và - trong mockup giỏ hàng
    public void updateQuantity(Long cartId, Integer newQuantity) {
        // 1. Kiểm tra điều kiện số lượng (Validation)
        if (newQuantity == null || newQuantity <= 0) {
            throw new IllegalArgumentException("Số lượng phải là số dương lớn hơn 0");
        }

        // 2. Tìm giỏ hàng
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

        // 3. Cập nhật và lưu
        cart.setQuantity(newQuantity);
        cartRepository.save(cart);
    }

    public List<Cart> getCartItems(Long userId) {
        return cartRepository.findByUserId(userId);
    }
}