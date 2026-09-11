package com.maillite.mailliteapi.config;

import com.maillite.mailliteapi.user.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * O JwtAuthenticationFilter carrega o UserDetails via UserDetailServiceImpl, que retorna
 * diretamente a entidade User (ela mesma implementa UserDetails) — então o principal da
 * Authentication já É o User autenticado, sem precisar de outra consulta ao banco aqui.
 */
@Component
public class CurrentUserProvider {

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }
}