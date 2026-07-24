package com.caro.bizkit.domain.user.entity;


import com.caro.bizkit.common.entity.BaseTimeEntity;
import com.caro.bizkit.domain.auth.entity.Account;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users") // SQL의 users 테이블과 매핑
@SQLDelete(sql = "UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
public class User extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 100)
    private String phoneNumber;

    @Column(length = 15)
    private String linedNumber;

    @Column(length = 20)
    private String company;

    @Column(length = 20)
    private String department;

    @Column(length = 20)
    private String position;

    @Column(length = 500)
    private String profileImageKey;

    @Column(columnDefinition = "TEXT")
    private String description;




    public static User create(Account account, String name, String email) {
        User user = new User();
        user.account = account;
        user.name = name;
        user.email = email;
        return user;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updatePhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void updateLinedNumber(String linedNumber) {
        this.linedNumber = linedNumber;
    }

    public void updateCompany(String company) {
        this.company = company;
    }

    public void updateDepartment(String department) {
        this.department = department;
    }

    public void updatePosition(String position) {
        this.position = position;
    }

    public void updateProfileImageKey(String profileImageKey) {
        this.profileImageKey = profileImageKey;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void markDeleted() {
        this.deletedAt = LocalDateTime.now();
        this.email = "deleted" + getEmail()+ UUID.randomUUID().toString().replaceAll("-", "");
        this.name = "deleted" + getName()+ UUID.randomUUID().toString().replaceAll("-", "");
        this.phoneNumber = "deleted" + getPhoneNumber()+ UUID.randomUUID().toString().replaceAll("-", "");
    }



}
