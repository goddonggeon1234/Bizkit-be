package com.caro.bizkit.domain.auth.entity;

import com.caro.bizkit.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "account")
@SQLDelete(sql = "UPDATE account SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
public class Account extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(length = 320, nullable = false)
    private String loginEmail;
    private LocalDateTime loggedAt;

    public static Account create(String loginEmail) {
        Account account = new Account();
        account.loginEmail = loginEmail;
        return account;
    }

    public void updateLoggedAt(LocalDateTime loggedAt) {
        this.loggedAt = loggedAt;
    }

    public void markDeleted() {
        this.deletedAt = LocalDateTime.now();
    }
}
