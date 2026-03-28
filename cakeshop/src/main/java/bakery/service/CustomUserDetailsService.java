package bakery.service;

import bakery.entity.User;
import bakery.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository repo;

    public CustomUserDetailsService(UserRepository repo){
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = repo.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));

        var authorities =
                user.getRoles()
                        .stream()
                        .map(role ->
                                new SimpleGrantedAuthority(
                                        "ROLE_" + role.getName().toUpperCase()
                                ))
                        .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getActive(),   // 🔥 CHỈ ACTIVE MỚI LOGIN
                true,
                true,
                true,
                authorities
        );
    }
}