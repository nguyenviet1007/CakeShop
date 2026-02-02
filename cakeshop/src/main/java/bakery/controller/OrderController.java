package bakery.controller;

import bakery.model.Order;
import bakery.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/orders")
    public String viewHomePage(Model model) {
        model.addAttribute("listOrders", orderService.getAllOrders());
        model.addAttribute("order", new Order());
        return "templates/home";
    }

    @PostMapping("/save")
    public String saveOrder(@ModelAttribute("order") Order order) {
        orderService.saveOrder(order);
        return "redirect:/orders";
    }

    @GetMapping("/edit/{id}")
    public String showFormForUpdate(@PathVariable(value = "id") Long id, Model model) {
        Order order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        model.addAttribute("listOrders", orderService.getAllOrders());
        return "home";
    }

    @GetMapping("/delete/{id}")
    public String deleteOrder(@PathVariable(value = "id") Long id) {
        orderService.deleteOrder(id);
        return "redirect:/";
    }
}