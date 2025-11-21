package cn.islys.gms.controller;

import cn.islys.gms.entity.User;
import cn.islys.gms.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * 登录页面
     */
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    /**
     * 用户登录
     */
    @PostMapping("/api/login")
    @ResponseBody
    public Map<String, Object> login(@RequestBody Map<String, String> loginData, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = loginData.get("username");
            String password = loginData.get("password");

            User user = userService.login(username, password);
            if (user != null) {
                // 登录成功，设置session
                session.setAttribute("userId", user.getId());
                session.setAttribute("username", user.getUsername());
                session.setAttribute("nickname", user.getNickname());

                response.put("success", true);
                response.put("message", "登录成功");
                response.put("user", user);
            } else {
                response.put("success", false);
                response.put("message", "用户名或密码错误");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "登录失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 用户注册
     */
    @PostMapping("/api/register")
    @ResponseBody
    public Map<String, Object> register(@RequestBody Map<String, String> registerData) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = registerData.get("username");
            String password = registerData.get("password");
            String nickname = registerData.get("nickname");

            User user = userService.register(username, password, nickname);
            response.put("success", true);
            response.put("message", "注册成功");
            response.put("user", user);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "注册失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 用户注销
     */
    @PostMapping("/api/logout")
    @ResponseBody
    public Map<String, Object> logout(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            session.invalidate();
            response.put("success", true);
            response.put("message", "注销成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "注销失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/api/user/info")
    @ResponseBody
    public Map<String, Object> getUserInfo(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId != null) {
                User user = userService.getUserById(userId);
                if (user != null) {
                    response.put("success", true);
                    response.put("user", user);
                } else {
                    response.put("success", false);
                    response.put("message", "用户不存在");
                }
            } else {
                response.put("success", false);
                response.put("message", "未登录");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取用户信息失败: " + e.getMessage());
        }
        return response;
    }
}