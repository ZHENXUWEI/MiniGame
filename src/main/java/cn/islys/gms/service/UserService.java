package cn.islys.gms.service;

import cn.islys.gms.entity.User;
import cn.islys.gms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * 用户登录
     */
    public User login(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsernameAndStatus(username, 1);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // 简单密码验证（实际项目中应该使用加密验证）
            if (user.getPassword().equals(password)) {
                // 更新最后登录时间
                user.setLastLoginTime(LocalDateTime.now());
                userRepository.save(user);
                return user;
            }
        }
        return null;
    }

    /**
     * 根据ID获取用户
     */
    public User getUserById(Long userId) {
        return userRepository.findActiveUserById(userId).orElse(null);
    }

    /**
     * 用户注册
     */
    @Transactional
    public User register(String username, String password, String nickname) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User(username, password, nickname);
        return userRepository.save(user);
    }

    /**
     * 验证用户是否存在且状态正常
     */
    public boolean validateUser(Long userId) {
        return userRepository.findActiveUserById(userId).isPresent();
    }
}