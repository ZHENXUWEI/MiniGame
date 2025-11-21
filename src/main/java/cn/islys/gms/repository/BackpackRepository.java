package cn.islys.gms.repository;

import cn.islys.gms.entity.Backpack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BackpackRepository extends JpaRepository<Backpack, Long> {

    /**
     * 根据用户ID获取背包物品（按购买时间倒序）
     */
    List<Backpack> findByUserIdOrderByPurchaseTimeDesc(Long userId);

    /**
     * 根据用户ID和物品ID查找背包物品
     */
    List<Backpack> findByUserIdAndItemId(Long userId, Long itemId);

    /**
     * 根据用户ID和物品类型查找背包物品
     */
    @Query("SELECT b FROM Backpack b JOIN Item i ON b.itemId = i.id WHERE b.userId = :userId AND i.type = :type")
    List<Backpack> findByUserIdAndItemType(@Param("userId") Long userId, @Param("type") String type);

    /**
     * 根据用户ID和物品名称模糊查询
     */
    @Query("SELECT b FROM Backpack b JOIN Item i ON b.itemId = i.id WHERE b.userId = :userId AND i.name LIKE %:name%")
    List<Backpack> findByUserIdAndItemNameContaining(@Param("userId") Long userId, @Param("name") String name);

    /**
     * 根据用户ID、物品类型和名称查询
     */
    @Query("SELECT b FROM Backpack b JOIN Item i ON b.itemId = i.id WHERE b.userId = :userId AND i.type = :type AND i.name LIKE %:name%")
    List<Backpack> findByUserIdAndItemTypeAndItemNameContaining(@Param("userId") Long userId, @Param("type") String type, @Param("name") String name);

    /**
     * 获取用户背包中存在的物品类型
     */
    @Query("SELECT DISTINCT i.type FROM Backpack b JOIN Item i ON b.itemId = i.id WHERE b.userId = :userId")
    List<String> findDistinctItemTypesByUserId(@Param("userId") Long userId);
}