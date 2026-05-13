package io.github.caioeduardopereirafelix.financeapi.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;

@Entity
@Data
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "roles")

public class RolesUser implements GrantedAuthority {

    @Id
    private Integer id;
    private String name;

    @Override
    public String getAuthority() {
        return name;
    }
}
