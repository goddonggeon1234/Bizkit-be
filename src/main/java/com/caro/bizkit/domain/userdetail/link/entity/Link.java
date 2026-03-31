package com.caro.bizkit.domain.userdetail.link.entity;


import com.caro.bizkit.common.entity.BaseTimeEntity;
import com.caro.bizkit.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "link")
@SQLDelete(sql = "UPDATE link SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
public class Link extends BaseTimeEntity { // BaseEntity 활용 (created_at, updated_at, deleted_at)
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 100)
    private String title;

    @Column(length = 2048, nullable = false)
    private String link;

    public static Link create(
            User user,
            String title,
            String link
    ) {
        Link linkEntity = new Link();
        linkEntity.user = user;
        linkEntity.title = title;
        linkEntity.link = link;
        return linkEntity;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateLink(String link) {
        this.link = link;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
