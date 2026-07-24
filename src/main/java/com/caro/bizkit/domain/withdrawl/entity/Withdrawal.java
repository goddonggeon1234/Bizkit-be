package com.caro.bizkit.domain.withdrawl.entity;


import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "withdrawal")
public class Withdrawal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 150, nullable = false)
    private String content;

    public static Withdrawal create(String content) {
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.content = content;
        return withdrawal;
    }
}
