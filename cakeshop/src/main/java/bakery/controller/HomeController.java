package bakery.controller;


import bakery.entity.*;
import bakery.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class HomeController {

    @Autowired private ProductRepository productRepository;

    @Autowired private CategoryRepository categoryRepository;

    @Autowired private UserRepository userRepository;

    @Autowired private OrderRepository orderRepository;

    @Autowired private OrderDetailRepository orderDetailRepository;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("products", productRepository.findAllWithImages());
        return "Home";
    }
    @GetMapping("/category/{id}")
    public String getProductsByCategory(@PathVariable Long id, Model model) {

        List<Product> products = productRepository.findByCategoryWithImages(id);
        List<Category> categories = categoryRepository.findAll();
        Category currentCategory = categoryRepository.findById(id).get();

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("currentCategoryName", currentCategory.getName());

        return "Home";
    }
    @GetMapping("/product/{id}/fragment")
    public String productDetailFragment(@PathVariable Long id, Model model) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        model.addAttribute("product", product);

        return "product-details :: productDetail";
    }
    @GetMapping("/account/profile")
    public String profile(HttpSession session, Model model){

        User user = (User) session.getAttribute("user");

        model.addAttribute("customer", user);

        return "profile";
    }

    @GetMapping("/account/orders")
    public String orders(@RequestParam(required = false) String status,
                         HttpSession session,
                         Model model) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }
        List<Order> orders;

        // nếu không chọn status -> lấy tất cả
        if (status == null || status.isEmpty()) {
            orders = orderRepository.findByUserId(user.getId());
        }
        // nếu có status -> lọc theo trạng thái
        else {
            orders = orderRepository.findByUserIdAndStatus(user.getId(), status);
        }

        model.addAttribute("orders", orders);
        model.addAttribute("currentStatus", status);
        return "orders";
    }
    @GetMapping("/account/orders/{id}")
    public String orderDetail(
            @PathVariable Long id,
            Model model) {

        Order order = orderRepository.findById(id).orElse(null);

        List<OrderDetail> orderDetails = orderDetailRepository.findByOrderId(id);

        model.addAttribute("order", order);
        model.addAttribute("orderDetails", orderDetails);

        return "order-detail";
    }
    @PostMapping("/update")
    public String updateProfile(@ModelAttribute("customer") User formUser,
                                HttpSession session) {

        User user = (User) session.getAttribute("user");

        if(user == null){
            return "redirect:/login";
        }

        user.setName(formUser.getName());
        user.setEmail(formUser.getEmail());
        user.setPhone(formUser.getPhone());
        user.setAddress(formUser.getAddress());

        userRepository.save(user);

        session.setAttribute("user", user);

        return "redirect:/account/profile?success";
    }
}
