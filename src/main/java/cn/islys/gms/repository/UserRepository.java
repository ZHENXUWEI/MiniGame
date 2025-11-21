package cn.islys.gms.repository;

import cn.islys.gms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndStatus(String username, Integer status);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.id = :userId AND u.status = 1")
    Optional<User> findActiveUserById(@Param("userId") Long userId);
}