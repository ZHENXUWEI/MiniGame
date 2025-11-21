package cn.islys.gms.repository;

import cn.islys.gms.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    // 根据类型查找
    List<Item> findByType(String type);

    // 根据名称模糊搜索
    List<Item> findByNameContaining(String name);

    // 根据类型和名称搜索
    List<Item> findByTypeAndNameContaining(String type, String name);

    // 新增：按精确名称查找
    Optional<Item> findByName(String name);

    @Query("SELECT DISTINCT i.type FROM Item i")
    List<String> findDistinctTypes();

    // 新增：获取类型下的所有物品名称
    @Query("SELECT i.name FROM Item i WHERE i.type = :type")
    List<String> findItemNamesByType(@Param("type") String type);
}