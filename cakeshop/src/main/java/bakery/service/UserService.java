package bakery.service;

import bakery.entity.User;

public interface UserService {
    User login(String identifier, String password);
    User register(User user);
    User processOAuthPostLogin(String email, String name);
}
