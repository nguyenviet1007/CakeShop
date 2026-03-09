package bakery.controller;


import bakery.entity.Category;
import bakery.entity.Order;
import bakery.entity.Product;
import bakery.entity.User;
import bakery.repository.CategoryRepository;
import bakery.repository.OrderRepository;
import bakery.repository.ProductRepository;
import bakery.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class HomeController {

    @Autowired private ProductRepository productRepository;

    @Autowired private CategoryRepository categoryRepository;

    @Autowired private UserRepository userRepository;

    @Autowired private OrderRepository orderRepository;

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
    public String orders(HttpSession session, Model model){

        User user = (User) session.getAttribute("user");

        List<Order> orders = orderRepository.findByUserId(user.getId());

        model.addAttribute("orders", orders);

        return "orders";
    }
    @PostMapping("/update")
    public String updateProfile(@ModelAttribute("customer") User formUser,
                                Authentication authentication) {

        String username = authentication.getName();

        User user = userRepository.findByUsername(username).orElseThrow();

        // chỉ update các field cho phép
        user.setName(formUser.getName());
        user.setEmail(formUser.getEmail());
        user.setPhone(formUser.getPhone());
        user.setAddress(formUser.getAddress());

        userRepository.save(user);

        return "redirect:/account/profile?success";
    }
}
