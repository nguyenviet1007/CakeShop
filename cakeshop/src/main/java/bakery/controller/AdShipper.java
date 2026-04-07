package bakery.controller;

import bakery.entity.Role;
import bakery.entity.ShippingFee;
import bakery.entity.User;
import bakery.repository.FeeRepository;
import bakery.repository.OrderRepository;
import bakery.repository.RoleRepository;
import bakery.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Controller
@RequestMapping("/admin/shippers")
public class AdShipper {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private FeeRepository feeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 🔹 1. Danh sách shipper
    @GetMapping("")
    public String getShippers(
            Model model,
            @PageableDefault(size = 5) Pageable pageable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {

        Role shipperRole = roleRepository.findByName("SHIPPER");

        Page<User> shipperPage;

        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        boolean hasStatus = status != null && !status.equals("all") && !status.trim().isEmpty();

        if (hasKeyword || hasStatus) {

            boolean active = "active".equals(status);

            shipperPage = userRepository.searchShippers(
                    shipperRole,
                    hasKeyword ? keyword : "",
                    hasStatus ? active : null,
                    pageable
            );

        } else {
            shipperPage = userRepository.findByRolesContaining(shipperRole, pageable);
        }

        model.addAttribute("shippers", shipperPage.getContent());
        model.addAttribute("currentPage", shipperPage.getNumber());
        model.addAttribute("totalPages", shipperPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("size", pageable.getPageSize());

        return "shipper-list";
    }

    // 🔹 2. Form thêm shipper
    @GetMapping("/create")
    public String createForm(Model model){
        model.addAttribute("user", new User());
        return "shipper-create";
    }

    // 🔹 3. Lưu shipper
    @PostMapping("/save")
    public String saveShipper(@ModelAttribute User user, Model model){

        //  kiểm tra username trùng
        if (userRepository.existsByUsername(user.getUsername())) {
            model.addAttribute("errorUsername", "Username đã tồn tại");
            model.addAttribute("user", user); // giữ lại dữ liệu đã nhập
            return "shipper-create";
        }

        Role shipperRole = roleRepository.findByName("SHIPPER");

        if (shipperRole == null) {
            throw new RuntimeException("Role SHIPPER not found in database");
        }

        //  mã hóa mật khẩu
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        //  gán role
        user.setRoles(Set.of(shipperRole));

        //  đảm bảo active
        user.setActive(true);

        //  lưu
        userRepository.save(user);

        return "redirect:/admin/shippers?success";
    }

    // 🔹 4. Xóa / disable shipper
    @GetMapping("/delete/{id}")
    public String deleteShipper(@PathVariable Long id){

        User user = userRepository.findById(id).get(); // giữ style cũ

        // check đã có đơn chưa
        boolean hasOrders = orderRepository.existsByShipper(user);

        if (hasOrders) {
            //  đã có đơn → disable
            user.setActive(false);
            userRepository.save(user);
        } else {
            //  chưa có đơn → xóa
            userRepository.delete(user);
        }

        return "redirect:/admin/shippers?deleted";
    }
    @GetMapping("/fee")
    public String feePage(Model model) {

        ShippingFee fee = feeRepository.findTopByOrderByIdDesc();

        if (fee == null) {
            fee = ShippingFee.builder()
                    .deliveredFee(0.0)
                    .failedFee(0.0)
                    .build();
        }

        model.addAttribute("fee", fee);
        return "fee";
    }
    @PostMapping("/fee/save")
    public String saveFee(@RequestParam double deliveredFee,
                          @RequestParam double failedFee) {

        //  lấy fee hiện tại
        ShippingFee fee = feeRepository.findTopByOrderByIdDesc();

        if (fee == null) {

            fee = new ShippingFee();
        }


        fee.setDeliveredFee(deliveredFee);
        fee.setFailedFee(failedFee);

        feeRepository.save(fee);

        return "redirect:/admin/shippers/fee?feeSaved";
    }
}