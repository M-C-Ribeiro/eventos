package com.eventhub.tenant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "organizacoes")
@Getter
@Setter
@NoArgsConstructor
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 120)
    private String name;

    @Column(name = "identificador", nullable = false, unique = true, length = 60)
    private String slug;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Tenant(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }
}
