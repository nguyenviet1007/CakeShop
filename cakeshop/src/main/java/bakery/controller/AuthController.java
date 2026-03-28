package bakery.controller;

import bakery.entity.User;
import bakery.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login(){
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(String username, String password){

        if(username == null || password == null){
            return "login";
        }

        if(username.equals("shipper")
                && password.equals("123")){
            return "home";
        }

        return "login";
    }

}
