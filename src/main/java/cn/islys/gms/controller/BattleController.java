package cn.islys.gms.controller;

import cn.islys.gms.entity.UserStats;
import cn.islys.gms.service.BattleService;
import cn.islys.gms.service.RankingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/battle")
public class BattleController {

    @Autowired
    private BattleService battleService;

    @Autowired
    private RankingService rankingService;

    /**
     * 战斗页面
     */
    @GetMapping("")
    public String battlePage() {
        return "battle";
    }

    /**
     * 排行榜页面
     */
    @GetMapping("/ranking")
    public String rankingPage() {
        return "ranking";
    }

    /**
     * 获取战斗装备
     */
    @GetMapping("/equipment")
    @ResponseBody
    public Map<String, Object> getBattleEquipment(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                response.put("success", false);
                response.put("message", "请先登录");
                return response;
            }

            Map<String, Object> equipment = battleService.getBattleEquipment(userId);
            response.putAll(equipment);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取战斗装备失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 获取用户战斗数据
     */
    @GetMapping("/user/stats")
    @ResponseBody
    public Map<String, Object> getUserStats(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                response.put("success", false);
                response.put("message", "请先登录");
                return response;
            }

            UserStats userStats = battleService.getUserStats(userId);
            if (userStats != null) {
                response.put("success", true);
                response.put("stats", userStats);
                response.put("rankTier", userStats.getRankTier());
                response.put("rankSubTier", userStats.getRankSubTier());
                response.put("performanceGrade", userStats.getPerformanceGrade());
                response.put("winLossRecord", "W" + userStats.getWins() + "/L" + userStats.getLosses());
            } else {
                response.put("success", false);
                response.put("message", "用户数据不存在");
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取用户数据失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 开始战斗
     */
    @PostMapping("/start")
    @ResponseBody
    public Map<String, Object> startBattle(
            @RequestBody Map<String, Object> battleData,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                response.put("success", false);
                response.put("message", "请先登录");
                return response;
            }

            String mode = (String) battleData.get("mode");
            List<Long> rangedWeaponIds = (List<Long>) battleData.get("rangedWeaponIds");
            Long meleeWeaponId = Long.valueOf(battleData.get("meleeWeaponId").toString());
            Long armorId = battleData.get("armorId") != null ?
                    Long.valueOf(battleData.get("armorId").toString()) : null;

            // 验证必须选择近战武器
            if (meleeWeaponId == null) {
                response.put("success", false);
                response.put("message", "必须选择一把近战武器");
                return response;
            }

            // 验证模式
            if (!"QUICK".equals(mode) && !"RANKED".equals(mode) && !"TOURNAMENT".equals(mode)) {
                response.put("success", false);
                response.put("message", "无效的战斗模式");
                return response;
            }

            Map<String, Object> battleResult = battleService.startBattle(
                    userId, mode, rangedWeaponIds, meleeWeaponId, armorId);

            if (Boolean.TRUE.equals(battleResult.get("success"))) {
                // 将战斗上下文存入session
                session.setAttribute("battleContext", battleResult.get("context"));
                session.setAttribute("battleId", battleResult.get("battleId"));

                response.put("success", true);
                response.put("battleId", battleResult.get("battleId"));
                response.put("context", battleResult.get("context"));
                response.put("message", "战斗开始！");
            } else {
                response.put("success", false);
                response.put("message", battleResult.get("message"));
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "开始战斗失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 执行战斗回合
     */
    @PostMapping("/turn")
    @ResponseBody
    public Map<String, Object> executeTurn(
            @RequestBody Map<String, Object> turnData,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                response.put("success", false);
                response.put("message", "请先登录");
                return response;
            }

            Long battleId = (Long) session.getAttribute("battleId");
            BattleService.BattleContext context =
                    (BattleService.BattleContext) session.getAttribute("battleContext");

            if (battleId == null || context == null) {
                response.put("success", false);
                response.put("message", "战斗未开始或已结束");
                return response;
            }

            Integer targetIndex = (Integer) turnData.get("targetIndex");
            String weaponType = (String) turnData.get("weaponType");

            Map<String, Object> turnResult = battleService.executeBattleTurn(
                    battleId, context, targetIndex, weaponType);

            if (Boolean.TRUE.equals(turnResult.get("success"))) {
                // 更新session中的战斗上下文
                session.setAttribute("battleContext", turnResult.get("context"));

                response.put("success", true);
                response.put("battleEnded", turnResult.get("battleEnded"));
                response.put("context", turnResult.get("context"));

                // 如果战斗结束，进行结算
                if (Boolean.TRUE.equals(turnResult.get("battleEnded"))) {
                    BattleService.BattleResult battleResult =
                            (BattleService.BattleResult) turnResult.get("battleResult");

                    Map<String, Object> finishResult = battleService.finishBattle(
                            userId, context, battleResult);

                    // 清理session中的战斗数据
                    session.removeAttribute("battleContext");
                    session.removeAttribute("battleId");

                    response.put("battleResult", finishResult);
                }
            } else {
                response.put("success", false);
                response.put("message", turnResult.get("message"));
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "执行战斗回合失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 投降结束战斗
     */
    @PostMapping("/surrender")
    @ResponseBody
    public Map<String, Object> surrenderBattle(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                response.put("success", false);
                response.put("message", "请先登录");
                return response;
            }

            BattleService.BattleContext context =
                    (BattleService.BattleContext) session.getAttribute("battleContext");

            if (context == null) {
                response.put("success", false);
                response.put("message", "没有进行中的战斗");
                return response;
            }

            // 创建投降结果
            BattleService.BattleResult battleResult = new BattleService.BattleResult();
            battleResult.setResult("LOSE");
            battleResult.setPlayerKills(0);
            battleResult.setPlayerDeaths(1);
            battleResult.setDurationSeconds(0);

            // 结算战斗
            Map<String, Object> finishResult = battleService.finishBattle(
                    userId, context, battleResult);

            // 清理session
            session.removeAttribute("battleContext");
            session.removeAttribute("battleId");

            response.put("success", true);
            response.put("message", "已投降");
            response.put("battleResult", finishResult);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "投降失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 获取战斗记录
     */
    @GetMapping("/records")
    @ResponseBody
    public Map<String, Object> getBattleRecords(
            @RequestParam(defaultValue = "10") Integer limit,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                response.put("success", false);
                response.put("message", "请先登录");
                return response;
            }

            List<Map<String, Object>> records = battleService.getBattleRecords(userId, limit);
            response.put("success", true);
            response.put("records", records);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取战斗记录失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 获取排行榜
     */
    @GetMapping("/ranking/list")
    @ResponseBody
    public Map<String, Object> getRankingList() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, Object>> rankingList = rankingService.getRankingList();
            response.put("success", true);
            response.put("data", rankingList);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取排行榜失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 获取用户排名
     */
    @GetMapping("/ranking/user")
    @ResponseBody
    public Map<String, Object> getUserRanking(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                response.put("success", false);
                response.put("message", "请先登录");
                return response;
            }

            Map<String, Object> userRanking = rankingService.getUserRanking(userId);
            response.put("success", true);
            response.put("data", userRanking);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取用户排名失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 刷新排行榜机器人数据
     */
    @PostMapping("/ranking/refresh-bots")
    @ResponseBody
    public Map<String, Object> refreshRankingBots() {
        Map<String, Object> response = new HashMap<>();
        try {
            rankingService.refreshRankingBots();
            response.put("success", true);
            response.put("message", "排行榜机器人数据已刷新");

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "刷新排行榜机器人失败: " + e.getMessage());
        }
        return response;
    }
}