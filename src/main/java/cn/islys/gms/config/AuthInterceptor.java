package cn.islys.gms.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 排除登录相关路径
        String uri = request.getRequestURI();
        if (uri.startsWith("/login") || uri.startsWith("/api/login") ||
                uri.startsWith("/api/register") || uri.startsWith("/static/") ||
                uri.startsWith("/css/") || uri.startsWith("/js/")) {
            return true;
        }

        HttpSession session = request.getSession();
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            // 未登录，重定向到登录页面
            response.sendRedirect("/login");
            return false;
        }

        return true;
    }
}