package cn.islys.gms.controller;

import cn.islys.gms.service.MarketService;
import cn.islys.gms.service.WalletService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/market")
public class MarketController {

    @Autowired
    private MarketService marketService;

    @Autowired
    private WalletService walletService;

    /**
     * 市场页面
     */
    @GetMapping("")
    public String marketPage() {
        return "market";
    }

    /**
     * 获取所有物品类型
     */
    @GetMapping("/types")
    @ResponseBody
    public Map<String, Object> getItemTypes() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<String> types = marketService.getAllItemTypes();
            response.put("success", true);
            response.put("data", types);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取类型失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 根据类型获取该类型下的所有物品名称
     */
    @GetMapping("/items/names/{type}")
    @ResponseBody
    public Map<String, Object> getItemNamesByType(@PathVariable String type) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<String> itemNames = marketService.getItemNamesByType(type);
            response.put("success", true);
            response.put("data", itemNames);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取物品名称失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 根据物品名称生成市场物品（支持排序）
     */
    @GetMapping("/items/{itemName}")
    @ResponseBody
    public Map<String, Object> getMarketItems(
            @PathVariable String itemName,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                response.put("success", false);
                response.put("message", "请先登录");
                return response;
            }

            // 获取市场物品
            List<Map<String, Object>> marketItems = marketService.generateMarketItemsByName(itemName);

            // 排序
            if (sortBy != null && sortOrder != null) {
                marketItems = marketService.sortMarketItems(marketItems, sortBy, sortOrder);
            }

            // 获取用户金钱
            Integer money = walletService.getUserWallet(userId).getMoney();

            response.put("success", true);
            response.put("money", money);
            response.put("data", marketItems);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取市场物品失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 购买物品
     */
    @PostMapping("/purchase")
    @ResponseBody
    public Map<String, Object> purchaseItem(@RequestBody Map<String, Object> purchaseData, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                response.put("success", false);
                response.put("message", "请先登录");
                return response;
            }

            Long itemId = Long.valueOf(purchaseData.get("itemId").toString());
            Integer currentDurability = purchaseData.get("currentDurability") != null ?
                    Integer.valueOf(purchaseData.get("currentDurability").toString()) : null;
            Integer maxDurability = purchaseData.get("maxDurability") != null ?
                    Integer.valueOf(purchaseData.get("maxDurability").toString()) : null;
            Integer purchasePrice = Integer.valueOf(purchaseData.get("purchasePrice").toString());
            Integer quantity = purchaseData.get("quantity") != null ?
                    Integer.valueOf(purchaseData.get("quantity").toString()) : null;

            boolean success = marketService.purchaseItem(userId, itemId, currentDurability, maxDurability, purchasePrice, quantity);

            if (success) {
                // 返回更新后的金钱
                Integer money = walletService.getUserWallet(userId).getMoney();
                response.put("success", true);
                response.put("money", money);
                response.put("message", "购买成功");
            } else {
                response.put("success", false);
                response.put("message", "金钱不足，无法购买");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "购买失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 获取用户当前金钱
     */
    @GetMapping("/money")
    @ResponseBody
    public Map<String, Object> getUserMoney(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                response.put("success", false);
                response.put("message", "请先登录");
                return response;
            }

            Integer money = walletService.getUserWallet(userId).getMoney();
            response.put("success", true);
            response.put("money", money);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取金钱失败: " + e.getMessage());
        }
        return response;
    }
}