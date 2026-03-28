package bakery.controller;

import bakery.entity.Role;
import bakery.entity.User;
import bakery.repository.RoleRepository;
import bakery.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Set;

@Controller
public class RegisterController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterController(UserRepository userRepository,
                              RoleRepository roleRepository,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/register")
    public String registerPage(Model model) {


        model.addAttribute("user", new User());

        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute User user, Model model) {

        // username trùng
        if (userRepository.existsByUsername(user.getUsername())) {
            model.addAttribute("usernameExists", true);
            model.addAttribute("user", user); //
            return "register";
        }

        // email trùng
        if (userRepository.existsByEmail(user.getEmail())) {
            model.addAttribute("emailExists", true);
            model.addAttribute("user", user);
            return "register";
        }

        Role role = roleRepository.findByName("CUSTOMER");
        if (role == null) {
            throw new RuntimeException("Role CUSTOMER not found");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(Set.of(role));
        user.setActive(true);

        userRepository.save(user);

        return "redirect:/login?registerSuccess";
    }
}