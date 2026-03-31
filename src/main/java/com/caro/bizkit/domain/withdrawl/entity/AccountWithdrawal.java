package com.caro.bizkit.domain.withdrawl.entity;



import com.caro.bizkit.domain.auth.entity.Account;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "account_withdrawal")
public class AccountWithdrawal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reason_id", nullable = false)
    private Withdrawal withdrawal;

    public static AccountWithdrawal create(Account account, Withdrawal withdrawal) {
        AccountWithdrawal accountWithdrawal = new AccountWithdrawal();
        accountWithdrawal.account = account;
        accountWithdrawal.withdrawal = withdrawal;
        return accountWithdrawal;
    }


}
