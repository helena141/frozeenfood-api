package com.lezzat.b2b2c.frozenfood.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;



import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE customers SET deleted_at = CURRENT_TIMESTAMP WHERE id_user = ?")
@SQLRestriction("deleted_at IS NULL")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_user", updatable = false, nullable = false)
    private UUID idUser;

    @Column(name = "nama", nullable = false, length = 150)
    private String nama;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "no_telp", nullable = false, length = 25)
    private String noTelp;

    @Column(name = "alamat", columnDefinition = "TEXT", nullable = false)
    private String alamat;

    // Tipe akun sangat krusial: 'B2C', 'B2B', 'ADMIN'
    @Column(name = "tipe_akun", nullable = false, length = 20)
    private String tipeAkun;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at", insertable = false, updatable = false)
    private LocalDateTime deletedAt;

@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
@Column(nullable = false, length = 60)
private String password;

}