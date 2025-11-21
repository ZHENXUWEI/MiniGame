package cn.islys.gms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "backpack")
public class Backpack {
    // Getter和Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId = 1;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "current_durability")
    private Integer currentDurability;

    @Column(name = "max_durability")
    private Integer maxDurability;

    @Column(name = "purchase_price", nullable = false)
    private Integer purchasePrice;

    @Column(name = "purchase_time")
    private LocalDateTime purchaseTime;

    @Column(name = "quantity")
    private Integer quantity = 1;

    // 关联物品信息
    @Transient
    private Item item;

    // 构造器
    public Backpack() {}

    // 计算出售价格
    public Integer getSellPrice() {
        if (item == null) return 0;

        if (currentDurability == null || maxDurability == null) {
            // 子弹类物品，按原价出售
            return item.getValue();
        }

        // 武器类物品，按耐久比例计算价格
        double durabilityRatio = (double) currentDurability / maxDurability;
        return (int) (item.getValue() * durabilityRatio);
    }

    // 获取耐久状态
    public String getDurabilityStatus() {
        if (currentDurability == null || maxDurability == null) {
            return "消耗品";
        }

        double ratio = (double) currentDurability / maxDurability;
        if (ratio >= 0.85) {
            return "崭新出产";
        } else if (ratio >= 0.60) {
            return "略有磨损";
        } else if (ratio >= 0.15) {
            return "破损不堪";
        } else {
            return "无法使用";
        }
    }
}
