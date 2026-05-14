package io.github.caioeduardopereirafelix.financeapi.model.entity;

import io.github.caioeduardopereirafelix.financeapi.config.AuditingClass;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.mapping.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Entity(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends AuditingClass implements UserDetails {

    @Id
    @Column(name = "id", unique = true)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "first_name")
    private String name;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "password")
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "users_roles",
            joinColumns = @JoinColumn(name = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private List<RolesUser> roles;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public @Nullable String getPassword(){
        return password;
    }


}
