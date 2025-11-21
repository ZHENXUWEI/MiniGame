package cn.islys.gms.controller;

import cn.islys.gms.entity.Backpack;
import cn.islys.gms.entity.UserWallet;
import cn.islys.gms.service.BackpackService;
import cn.islys.gms.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/backpack")
public class BackpackController {

    @Autowired
    private BackpackService backpackService;

    @Autowired
    private WalletService walletService;

    /**
     * 背包页面
     */
    @GetMapping("")
    public String backpackPage() {
        return "backpack";
    }

    /**
     * 获取背包信息和用户金钱
     */
    @GetMapping("/info")
    @ResponseBody
    public Map<String, Object> getBackpackInfo(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        Map<String, Object> response = new HashMap<>();
        try {
            Integer userId = 1; // 默认用户ID

            // 获取用户金钱
            UserWallet wallet = walletService.getUserWallet(userId);

            // 获取背包物品
            List<Backpack> backpackItems;
            if (type != null && !type.isEmpty() && keyword != null && !keyword.isEmpty()) {
                backpackItems = backpackService.searchBackpackByTypeAndKeyword(userId, type, keyword);
            } else if (type != null && !type.isEmpty()) {
                backpackItems = backpackService.getBackpackByType(userId, type);
            } else if (keyword != null && !keyword.isEmpty()) {
                backpackItems = backpackService.searchBackpackByKeyword(userId, keyword);
            } else {
                backpackItems = backpackService.getUserBackpack(userId);
            }

            response.put("success", true);
            response.put("money", wallet.getMoney());
            response.put("data", backpackItems);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取背包信息失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 出售物品
     */
    @PostMapping("/sell/{backpackId}")
    @ResponseBody
    public Map<String, Object> sellItem(@PathVariable Long backpackId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Integer userId = 1; // 默认用户ID
            boolean success = backpackService.sellItem(userId, backpackId);

            if (success) {
                // 返回更新后的金钱
                UserWallet wallet = walletService.getUserWallet(userId);
                response.put("success", true);
                response.put("money", wallet.getMoney());
                response.put("message", "物品出售成功");
            } else {
                response.put("success", false);
                response.put("message", "出售失败");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "出售失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 获取所有物品类型（用于筛选）
     */
    @GetMapping("/types")
    @ResponseBody
    public Map<String, Object> getBackpackItemTypes() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<String> types = backpackService.getBackpackItemTypes(1);
            response.put("success", true);
            response.put("data", types);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取类型失败: " + e.getMessage());
        }
        return response;
    }
}