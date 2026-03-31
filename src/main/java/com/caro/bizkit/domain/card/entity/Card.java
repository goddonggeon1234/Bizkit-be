package com.caro.bizkit.domain.card.entity;




import com.caro.bizkit.common.entity.BaseTimeEntity;
import com.caro.bizkit.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import com.github.f4b6a3.uuid.UuidCreator;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "card")
@SQLDelete(sql = "UPDATE card SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("")
public class Card extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 명함 생성자 (nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 36, nullable = false)
    private String uuid; // UUID v7

    @Column(length = 30, nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(length = 15)
    private String phoneNumber;

    @Column(length = 15)
    private String linedNumber;

    @Column(length = 255, nullable = false)
    private String company;

    @Column(length = 100)
    private String position;

    @Column(length = 255)
    private String department;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Column(nullable = false)
    private Boolean isProgress;

    @Column(length = 500)
    private String qrImageKey;

    @Column(length = 500)
    private String aiImageKey;

    @Column(length = 500)
    private String description;

    public static Card create(
            User user,
            String uuid,
            String name,
            String email,
            String phoneNumber,
            String linedNumber,
            String company,
            String position,
            String department,
            LocalDate startDate,
            LocalDate endDate,
            String aiImageKey
    ) {
        Card card = new Card();
        card.user = user;
        card.uuid = uuid;
        card.name = name;
        card.email = email;
        card.phoneNumber = phoneNumber;
        card.linedNumber = linedNumber;
        card.company = company;
        card.position = position;
        card.department = department;
        card.startDate = startDate;
        card.endDate = endDate;
        card.isProgress = (endDate == null);
        card.aiImageKey = aiImageKey;
        return card;
    }

    public static String newUuid() {
        return UuidCreator.getTimeOrderedEpoch().toString();
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateEmail(String email) {
        this.email = email;
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

    public void updatePosition(String position) {
        this.position = position;
    }

    public void updateDepartment(String department) {
        this.department = department;
    }

    public void updateStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void updateEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void updateIsProgress(Boolean isProgress) {
        this.isProgress = isProgress;
    }

    public void updateQrImageKey(String qrImageKey) {
        this.qrImageKey = qrImageKey;
    }

    public void updateAiImageKey(String aiImageKey) {
        this.aiImageKey = aiImageKey;
    }

    public void updateDescription(String description) {
        this.description = description;
    }
}
