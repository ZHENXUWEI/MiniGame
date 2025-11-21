package cn.islys.gms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "user_wallet")
public class UserWallet {
    // Getter和Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "money", nullable = false)
    private Integer money = 10000;

    @Column(name = "updated_time")
    private LocalDateTime updatedTime;

    public UserWallet() {}

}
