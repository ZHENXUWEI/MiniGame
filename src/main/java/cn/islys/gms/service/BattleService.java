package cn.islys.gms.service;

import cn.islys.gms.entity.*;
import cn.islys.gms.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class BattleService {

    @Autowired
    private UserStatsRepository userStatsRepository;

    @Autowired
    private BattleRecordRepository battleRecordRepository;

    @Autowired
    private RankingBotRepository rankingBotRepository;

    @Autowired
    private BackpackService backpackService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private ItemService itemService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 战斗配置
    private static final int BASE_HEALTH = 150;
    private static final int MAX_TEAM_SIZE = 3;
    private BattleContext context;

    /**
     * 伤害计算结果
     */
    public static class BattleDamage {
        private int baseDamage;
        private int finalDamage;
        private boolean defended;

        // getters and setters
        public int getBaseDamage() { return baseDamage; }
        public void setBaseDamage(int baseDamage) { this.baseDamage = baseDamage; }
        public int getFinalDamage() { return finalDamage; }
        public void setFinalDamage(int finalDamage) { this.finalDamage = finalDamage; }
        public boolean isDefended() { return defended; }
        public void setDefended(boolean defended) { this.defended = defended; }
    }

    // 战斗装备选择
    public Map<String, Object> getBattleEquipment(Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 获取用户背包中的武器和护甲
            List<Backpack> backpackItems = backpackService.getUserBackpack(userId);

            List<Backpack> rangedWeapons = new ArrayList<>();
            List<Backpack> meleeWeapons = new ArrayList<>();
            List<Backpack> armors = new ArrayList<>();

            for (Backpack item : backpackItems) {
                String type = item.getItem().getType();
                if ("远程武器".equals(type) || "手枪".equals(type) || "步枪".equals(type) ||
                        "冲锋枪".equals(type) || "狙击枪".equals(type)) {
                    rangedWeapons.add(item);
                } else if ("近战武器".equals(type)) {
                    meleeWeapons.add(item);
                } else if ("护甲".equals(type)) {
                    armors.add(item);
                }
            }

            result.put("success", true);
            result.put("rangedWeapons", rangedWeapons);
            result.put("meleeWeapons", meleeWeapons);
            result.put("armors", armors);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取战斗装备失败: " + e.getMessage());
        }
        return result;
    }

    // 开始战斗
    @Transactional
    public Map<String, Object> startBattle(Long userId, String mode,
                                           List<Long> rangedWeaponIds,
                                           Long meleeWeaponId,
                                           Long armorId) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 验证装备
            if (rangedWeaponIds == null || rangedWeaponIds.isEmpty()) {
                result.put("success", false);
                result.put("message", "至少需要选择一把远程武器");
                return result;
            }

            if (meleeWeaponId == null) {
                result.put("success", false);
                result.put("message", "必须选择一把近战武器");
                return result;
            }

            if (rangedWeaponIds.size() > 2) {
                result.put("success", false);
                result.put("message", "最多只能选择两把远程武器");
                return result;
            }

            // 获取用户状态
            UserStats userStats = getUserStats(userId);

            // 生成敌人队伍
            List<BattleCharacter> enemyTeam = generateEnemyTeam(userStats.getRankPoints(), userStats.getPerformance());

            // 生成队友队伍（人机）
            List<BattleCharacter> teammateTeam = generateTeammateTeam(userStats.getRankPoints());

            // 创建玩家角色
            BattleCharacter player = createPlayerCharacter(userId, rangedWeaponIds, meleeWeaponId, armorId);

            // 创建战斗上下文
            BattleContext context = new BattleContext();
            context.setPlayer(player);
            context.setTeammates(teammateTeam);
            context.setEnemies(enemyTeam);
            context.setMode(mode);
            context.setFirstTurn(ThreadLocalRandom.current().nextBoolean());

            result.put("success", true);
            result.put("battleId", System.currentTimeMillis()); // 临时战斗ID
            result.put("context", context);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "开始战斗失败: " + e.getMessage());
        }
        return result;
    }

    // 执行战斗回合
    @Transactional
    public Map<String, Object> executeBattleTurn(Long battleId, BattleContext context,
                                                 Integer targetIndex, String weaponType) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 玩家行动
            boolean battleEnded = executePlayerAction(context, targetIndex, weaponType);

            if (!battleEnded) {
                // AI行动
                executeAIActions(context);
                battleEnded = checkBattleEnd(context);
            }

            result.put("success", true);
            result.put("battleEnded", battleEnded);
            result.put("context", context);
            result.put("battleResult", battleEnded ? getBattleResult(context) : null);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "执行战斗回合失败: " + e.getMessage());
        }
        return result;
    }

    // 结束战斗并结算
    @Transactional
    public Map<String, Object> finishBattle(Long userId, BattleContext context, BattleResult result) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserStats userStats = getUserStats(userId);

            // 计算奖励
            Map<String, Integer> rewards = calculateRewards(context, result, userStats);

            // 更新用户数据
            updateUserStats(userStats, result, rewards);

            // 保存战斗记录
            saveBattleRecord(userId, context, result, rewards);

            response.put("success", true);
            response.put("rewards", rewards);
            response.put("updatedStats", userStats);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "战斗结算失败: " + e.getMessage());
        }
        return response;
    }

    // 内部类和方法
    public  UserStats getUserStats(Long userId) {
        return userStatsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserStats stats = new UserStats();
                    stats.setUserId(userId);
                    return userStatsRepository.save(stats);
                });
    }

    private List<BattleCharacter> generateEnemyTeam(Integer playerRankPoints, Integer playerPerformance) {
        List<BattleCharacter> enemies = new ArrayList<>();
        int teamSize = MAX_TEAM_SIZE;

        for (int i = 0; i < teamSize; i++) {
            // 根据玩家段位和表现生成敌人
            int enemyRankPoints = playerRankPoints + ThreadLocalRandom.current().nextInt(-400, 401);
            int enemyPerformance = Math.max(0, Math.min(500,
                    playerPerformance + ThreadLocalRandom.current().nextInt(-100, 101)));

            BattleCharacter enemy = generateAICharacter("ENEMY_" + (i+1), enemyRankPoints, enemyPerformance, false);
            enemies.add(enemy);
        }

        return enemies;
    }

    private List<BattleCharacter> generateTeammateTeam(Integer playerRankPoints) {
        List<BattleCharacter> teammates = new ArrayList<>();
        int teamSize = MAX_TEAM_SIZE - 1; // 减去玩家自己

        for (int i = 0; i < teamSize; i++) {
            int teammateRankPoints = playerRankPoints + ThreadLocalRandom.current().nextInt(-200, 201);
            int teammatePerformance = ThreadLocalRandom.current().nextInt(50, 401);

            BattleCharacter teammate = generateAICharacter("TEAMMATE_" + (i+1), teammateRankPoints, teammatePerformance, true);
            teammates.add(teammate);
        }

        return teammates;
    }

    private BattleCharacter generateAICharacter(String name, Integer rankPoints, Integer performance, boolean isTeammate) {
        BattleCharacter character = new BattleCharacter();
        character.setName(name);
        character.setHealth(BASE_HEALTH);
        character.setMaxHealth(BASE_HEALTH);
        character.setRankPoints(rankPoints);
        character.setPerformance(performance);
        character.setBot(true);

        // 根据表现等级生成装备
        String performanceGrade = getPerformanceGrade(performance);
        generateAIEquipment(character, performanceGrade);

        return character;
    }

    private void generateAIEquipment(BattleCharacter character, String performanceGrade) {
        // 根据表现等级生成装备的逻辑
        // S: 昂贵装备, A: 较贵装备, B: 普通装备, C: 便宜装备, D: 可能没有远程武器或护甲
        // 这里需要根据实际的物品数据库来生成合适的装备
    }

    private BattleCharacter createPlayerCharacter(Long userId, List<Long> rangedWeaponIds,
                                                  Long meleeWeaponId, Long armorId) {
        BattleCharacter player = new BattleCharacter();
        player.setName("Player");
        player.setHealth(BASE_HEALTH);
        player.setMaxHealth(BASE_HEALTH);
        player.setBot(false);

        // 设置玩家装备
        // 这里需要从背包中获取具体的装备信息

        return player;
    }

    private Map<String, Integer> calculateRewards(BattleContext context, BattleResult result, UserStats userStats) {
        Map<String, Integer> rewards = new HashMap<>();
        int kills = result.getPlayerKills();
        boolean isWin = "WIN".equals(result.getResult());
        String mode = context.getMode();

        Random random = new Random();

        if ("QUICK".equals(mode)) {
            if (isWin) {
                switch (kills) {
                    case 0:
                        rewards.put("experience", random.nextInt(31) + 10);
                        rewards.put("money", random.nextInt(401) + 100);
                        rewards.put("performance", random.nextInt(11) + 10);
                        break;
                    case 1:
                        rewards.put("experience", random.nextInt(36) + 40);
                        rewards.put("money", random.nextInt(451) + 150);
                        rewards.put("performance", random.nextInt(11) + 15);
                        break;
                    case 2:
                        rewards.put("experience", random.nextInt(46) + 80);
                        rewards.put("money", random.nextInt(451) + 220);
                        rewards.put("performance", random.nextInt(11) + 25);
                        break;
                    case 3:
                        rewards.put("experience", random.nextInt(61) + 120);
                        rewards.put("money", random.nextInt(481) + 200);
                        rewards.put("performance", random.nextInt(21) + 35);
                        break;
                }
            } else {
                switch (kills) {
                    case 0:
                        rewards.put("experience", random.nextInt(6) + 5);
                        rewards.put("money", random.nextInt(81) + 20);
                        rewards.put("performance", random.nextInt(11) - 35);
                        break;
                    case 1:
                        rewards.put("experience", random.nextInt(21) + 20);
                        rewards.put("money", random.nextInt(91) + 50);
                        rewards.put("performance", random.nextInt(11) - 5);
                        break;
                    case 2:
                        rewards.put("experience", random.nextInt(26) + 40);
                        rewards.put("money", random.nextInt(111) + 90);
                        rewards.put("performance", random.nextInt(11) + 5);
                        break;
                    case 3:
                        rewards.put("experience", random.nextInt(36) + 40);
                        rewards.put("money", random.nextInt(151) + 150);
                        rewards.put("performance", random.nextInt(16) + 20);
                        break;
                }
            }
        } else if ("RANKED".equals(mode)) {
            // 排位模式奖励是快速模式的1.5倍
            Map<String, Integer> baseRewards = calculateRewards(new BattleContext() {{ setMode("QUICK"); }},
                    result, userStats);

            for (Map.Entry<String, Integer> entry : baseRewards.entrySet()) {
                if (!"performance".equals(entry.getKey())) {
                    rewards.put(entry.getKey(), (int)(entry.getValue() * 1.5));
                } else {
                    rewards.put(entry.getKey(), entry.getValue());
                }
            }

            // 段位分奖励
            if (isWin) {
                switch (kills) {
                    case 0: rewards.put("rankPoints", random.nextInt(6) + 10); break;
                    case 1: rewards.put("rankPoints", random.nextInt(11) + 20); break;
                    case 2: rewards.put("rankPoints", random.nextInt(11) + 30); break;
                    case 3: rewards.put("rankPoints", random.nextInt(16) + 45); break;
                }
            } else {
                switch (kills) {
                    case 0: rewards.put("rankPoints", random.nextInt(16) - 45); break;
                    case 1: rewards.put("rankPoints", random.nextInt(11) - 20); break;
                    case 2: rewards.put("rankPoints", random.nextInt(16) - 10); break;
                    case 3: rewards.put("rankPoints", random.nextInt(11) + 5); break;
                }
            }
        }

        return rewards;
    }

    private void updateUserStats(UserStats stats, BattleResult result, Map<String, Integer> rewards) {
        stats.setExperience(stats.getExperience() + rewards.getOrDefault("experience", 0));
        stats.setPerformance(Math.max(0, Math.min(500,
                stats.getPerformance() + rewards.getOrDefault("performance", 0))));
        stats.setRankPoints(Math.max(0,
                stats.getRankPoints() + rewards.getOrDefault("rankPoints", 0)));

        if ("WIN".equals(result.getResult())) {
            stats.setWins(stats.getWins() + 1);
        } else {
            stats.setLosses(stats.getLosses() + 1);
        }

        stats.setTotalKills(stats.getTotalKills() + result.getPlayerKills());
        stats.setTotalDeaths(stats.getTotalDeaths() + result.getPlayerDeaths());

        // 检查升级
        boolean leveledUp = stats.checkLevelUp();

        userStatsRepository.save(stats);

        // 添加金币到钱包
        if (rewards.containsKey("money")) {
            walletService.addMoney(stats.getUserId(), rewards.get("money"));
        }
    }

    private void saveBattleRecord(Long userId, BattleContext context, BattleResult result, Map<String, Integer> rewards) {
        BattleRecord record = new BattleRecord();
        record.setUserId(userId);
        record.setMode(context.getMode());
        record.setResult(result.getResult());
        record.setKills(result.getPlayerKills());
        record.setExperienceGained(rewards.getOrDefault("experience", 0));
        record.setMoneyGained(rewards.getOrDefault("money", 0));
        record.setPerformanceGained(rewards.getOrDefault("performance", 0));
        record.setRankPointsGained(rewards.getOrDefault("rankPoints", 0));
        record.setDurationSeconds(result.getDurationSeconds());

        battleRecordRepository.save(record);
    }

    private String getPerformanceGrade(Integer performance) {
        if (performance >= 400) return "S";
        if (performance >= 300) return "A";
        if (performance >= 200) return "B";
        if (performance >= 100) return "C";
        return "D";
    }

    // 战斗相关的内部类
    public static class BattleCharacter {
        private String name;
        private Integer health;
        private Integer maxHealth;
        private Integer rankPoints;
        private Integer performance;
        private Boolean isBot;
        private List<Backpack> rangedWeapons;
        private Backpack meleeWeapon;
        private Backpack armor;
        private Boolean isAlive = true;

        // getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getHealth() { return health; }
        public void setHealth(Integer health) { this.health = health; }
        public Integer getMaxHealth() { return maxHealth; }
        public void setMaxHealth(Integer maxHealth) { this.maxHealth = maxHealth; }
        public Integer getRankPoints() { return rankPoints; }
        public void setRankPoints(Integer rankPoints) { this.rankPoints = rankPoints; }
        public Integer getPerformance() { return performance; }
        public void setPerformance(Integer performance) { this.performance = performance; }
        public Boolean getBot() { return isBot; }
        public void setBot(Boolean bot) { isBot = bot; }
        public List<Backpack> getRangedWeapons() { return rangedWeapons; }
        public void setRangedWeapons(List<Backpack> rangedWeapons) { this.rangedWeapons = rangedWeapons; }
        public Backpack getMeleeWeapon() { return meleeWeapon; }
        public void setMeleeWeapon(Backpack meleeWeapon) { this.meleeWeapon = meleeWeapon; }
        public Backpack getArmor() { return armor; }
        public void setArmor(Backpack armor) { this.armor = armor; }
        public Boolean getAlive() { return isAlive; }
        public void setAlive(Boolean alive) { isAlive = alive; }
    }

    public static class BattleContext {
        private BattleCharacter player;
        private List<BattleCharacter> teammates;
        private List<BattleCharacter> enemies;
        private String mode;
        private Boolean firstTurn;
        private Integer currentTurn = 0;
        private Boolean playerTurn;

        // getters and setters
        public BattleCharacter getPlayer() { return player; }
        public void setPlayer(BattleCharacter player) { this.player = player; }
        public List<BattleCharacter> getTeammates() { return teammates; }
        public void setTeammates(List<BattleCharacter> teammates) { this.teammates = teammates; }
        public List<BattleCharacter> getEnemies() { return enemies; }
        public void setEnemies(List<BattleCharacter> enemies) { this.enemies = enemies; }
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public Boolean getFirstTurn() { return firstTurn; }
        public void setFirstTurn(Boolean firstTurn) { this.firstTurn = firstTurn; }
        public Integer getCurrentTurn() { return currentTurn; }
        public void setCurrentTurn(Integer currentTurn) { this.currentTurn = currentTurn; }
        public Boolean getPlayerTurn() { return playerTurn; }
        public void setPlayerTurn(Boolean playerTurn) { this.playerTurn = playerTurn; }
    }

    public static class BattleResult {
        private String result; // WIN, LOSE
        private Integer playerKills;
        private Integer playerDeaths;
        private Integer durationSeconds;

        // getters and setters
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
        public Integer getPlayerKills() { return playerKills; }
        public void setPlayerKills(Integer playerKills) { this.playerKills = playerKills; }
        public Integer getPlayerDeaths() { return playerDeaths; }
        public void setPlayerDeaths(Integer playerDeaths) { this.playerDeaths = playerDeaths; }
        public Integer getDurationSeconds() { return durationSeconds; }
        public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
    }

    /**
     * 获取战斗记录
     */
    public List<Map<String, Object>> getBattleRecords(Long userId, Integer limit) {
        List<Map<String, Object>> records = new ArrayList<>();

        try {
            List<BattleRecord> battleRecords = battleRecordRepository.findTop10ByUserIdOrderByBattleTimeDesc(userId);

            for (BattleRecord record : battleRecords) {
                Map<String, Object> recordData = new HashMap<>();
                recordData.put("id", record.getId());
                recordData.put("mode", record.getMode());
                recordData.put("result", record.getResult());
                recordData.put("kills", record.getKills());
                recordData.put("experienceGained", record.getExperienceGained());
                recordData.put("moneyGained", record.getMoneyGained());
                recordData.put("performanceGained", record.getPerformanceGained());
                recordData.put("rankPointsGained", record.getRankPointsGained());
                recordData.put("battleTime", record.getBattleTime());
                recordData.put("durationSeconds", record.getDurationSeconds());

                records.add(recordData);
            }

        } catch (Exception e) {
            // 记录错误但不抛出，返回空列表
            System.err.println("获取战斗记录失败: " + e.getMessage());
        }

        return records;
    }

    /**
     * 计算伤害
     */
    public BattleDamage calculateDamage(BattleCharacter attacker, BattleCharacter target, String weaponType) {
        BattleDamage damage = new BattleDamage();
        Random random = new Random();

        try {
            if ("RANGED".equals(weaponType)) {
                // 远程武器伤害 = 武器伤害 × 子弹伤害倍率
                Backpack weapon = attacker.getRangedWeapons().get(0); // 使用第一把远程武器
                Backpack bullet = findSuitableBullet(attacker, weapon);

                float weaponDamage = weapon.getItem().getDamage();
                float bulletMultiplier = bullet != null ? bullet.getItem().getDamage() : 1.0f;
                float baseDamage = weaponDamage * bulletMultiplier;

                // 耐久度影响
                if (weapon.getCurrentDurability() != null) {
                    float durabilityRatio = (float) weapon.getCurrentDurability() / weapon.getMaxDurability();
                    baseDamage *= durabilityRatio;
                }

                damage.setBaseDamage((int) baseDamage);

            } else if ("MELEE".equals(weaponType)) {
                // 近战武器伤害 = 武器本身伤害
                Backpack weapon = attacker.getMeleeWeapon();
                float baseDamage = weapon.getItem().getDamage();

                // 耐久度影响
                if (weapon.getCurrentDurability() != null) {
                    float durabilityRatio = (float) weapon.getCurrentDurability() / weapon.getMaxDurability();
                    baseDamage *= durabilityRatio;
                }

                damage.setBaseDamage((int) baseDamage);
            }

            // 护甲防御计算
            if (target.getArmor() != null && target.getArmor().getCurrentDurability() > 0) {
                Item armor = target.getArmor().getItem();
                int defense = armor.getArmorDefense();

                if (damage.getBaseDamage() <= defense) {
                    // 完全防御
                    damage.setFinalDamage(1);
                    damage.setDefended(true);

                    // 护甲耐久消耗
                    int durabilityLoss = random.nextInt(11) + 10; // 10-20
                    target.getArmor().setCurrentDurability(
                            Math.max(0, target.getArmor().getCurrentDurability() - durabilityLoss)
                    );
                } else {
                    // 部分防御
                    damage.setFinalDamage((int) ((damage.getBaseDamage() - defense) * 1.1));
                    damage.setDefended(false);

                    // 护甲耐久消耗
                    int durabilityLoss = random.nextInt(11) + 10; // 10-20
                    target.getArmor().setCurrentDurability(
                            Math.max(0, target.getArmor().getCurrentDurability() - durabilityLoss)
                    );
                }
            } else {
                // 无护甲
                damage.setFinalDamage(damage.getBaseDamage());
                damage.setDefended(false);
            }

        } catch (Exception e) {
            // 计算失败时使用基础伤害
            damage.setBaseDamage(10);
            damage.setFinalDamage(10);
            damage.setDefended(false);
        }

        return damage;
    }

    /**
     * 寻找合适的子弹
     */
    private Backpack findSuitableBullet(BattleCharacter character, Backpack weapon) {
        if (character.getRangedWeapons() == null) return null;

        String requiredBullet = weapon.getItem().getUsedBullet();
        if (requiredBullet == null || requiredBullet.isEmpty()) return null;

        for (Backpack item : character.getRangedWeapons()) {
            if (requiredBullet.equals(item.getItem().getName()) && item.getQuantity() > 0) {
                return item;
            }
        }

        return null;
    }

    /**
     * 执行玩家行动
     */
    private boolean executePlayerAction(BattleContext context, Integer targetIndex, String weaponType) {
        BattleCharacter player = context.getPlayer();
        List<BattleCharacter> enemies = context.getEnemies();

        // 验证目标
        if (targetIndex < 0 || targetIndex >= enemies.size()) {
            return false;
        }

        BattleCharacter target = enemies.get(targetIndex);
        if (!target.getAlive()) {
            return false;
        }

        // 计算伤害
        BattleDamage damage = calculateDamage(player, target, weaponType);

        // 应用伤害
        target.setHealth(target.getHealth() - damage.getFinalDamage());

        // 检查目标是否死亡
        if (target.getHealth() <= 0) {
            target.setAlive(false);
            target.setHealth(0);
            // 记录击杀
            // 这里可以在BattleResult中记录
        }

        // 消耗弹药（如果是远程武器）
        if ("RANGED".equals(weaponType) && player.getRangedWeapons() != null && !player.getRangedWeapons().isEmpty()) {
            Backpack weapon = player.getRangedWeapons().get(0);
            Backpack bullet = findSuitableBullet(player, weapon);
            if (bullet != null) {
                bullet.setQuantity(bullet.getQuantity() - 1);
            }

            // 武器耐久消耗
            if (weapon.getCurrentDurability() != null) {
                weapon.setCurrentDurability(weapon.getCurrentDurability() - 1);
            }
        }

        // 近战武器耐久消耗
        if ("MELEE".equals(weaponType) && player.getMeleeWeapon() != null) {
            Backpack weapon = player.getMeleeWeapon();
            if (weapon.getCurrentDurability() != null) {
                weapon.setCurrentDurability(weapon.getCurrentDurability() - 1);
            }
        }

        context.setCurrentTurn(context.getCurrentTurn() + 1);
        context.setPlayerTurn(false);

        return checkBattleEnd(context);
    }

    /**
     * 执行AI行动
     */
    private void executeAIActions(BattleContext context) {
        // 队友AI行动
        for (BattleCharacter teammate : context.getTeammates()) {
            if (teammate.getAlive()) {
                executeSingleAIAction(teammate, context.getEnemies(), true);
            }
        }

        // 敌人AI行动
        for (BattleCharacter enemy : context.getEnemies()) {
            if (enemy.getAlive()) {
                executeSingleAIAction(enemy, getAllAliveTeammates(context), false);
            }
        }

        context.setCurrentTurn(context.getCurrentTurn() + 1);
        context.setPlayerTurn(true);
    }

    /**
     * 执行单个AI行动
     */
    private void executeSingleAIAction(BattleCharacter ai, List<BattleCharacter> targets, boolean isTeammate) {
        if (targets.isEmpty()) return;

        // AI逻辑：优先攻击血量低的目标
        BattleCharacter target = targets.stream()
                .filter(BattleCharacter::getAlive)
                .min(Comparator.comparing(BattleCharacter::getHealth))
                .orElse(targets.get(0));

        // 随机选择武器类型
        String weaponType = (ai.getRangedWeapons() != null && !ai.getRangedWeapons().isEmpty()) ?
                "RANGED" : "MELEE";

        // 计算伤害
        BattleDamage damage = calculateDamage(ai, target, weaponType);

        // 应用伤害
        target.setHealth(target.getHealth() - damage.getFinalDamage());

        // 检查目标是否死亡
        if (target.getHealth() <= 0) {
            target.setAlive(false);
            target.setHealth(0);
        }
    }

    /**
     * 获取所有存活的队友（包括玩家）
     */
    private List<BattleCharacter> getAllAliveTeammates(BattleContext context) {
        List<BattleCharacter> allTeammates = new ArrayList<>();
        allTeammates.add(context.getPlayer());
        allTeammates.addAll(context.getTeammates());

        return allTeammates.stream()
                .filter(BattleCharacter::getAlive)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 检查战斗是否结束
     */
    private boolean checkBattleEnd(BattleContext context) {
        this.context = context;
        // 检查敌人是否全部死亡
        boolean allEnemiesDead = context.getEnemies().stream()
                .noneMatch(BattleCharacter::getAlive);

        // 检查队友（包括玩家）是否全部死亡
        boolean allTeammatesDead = getAllAliveTeammates(context).isEmpty();

        return allEnemiesDead || allTeammatesDead;
    }

    /**
     * 获取战斗结果
     */
    public BattleResult getBattleResult(BattleContext context) {
        BattleResult result = new BattleResult();

        // 检查敌人是否全部死亡
        boolean allEnemiesDead = context.getEnemies().stream()
                .noneMatch(BattleCharacter::getAlive);

        if (allEnemiesDead) {
            result.setResult("WIN");
        } else {
            result.setResult("LOSE");
        }

        // 计算玩家击杀数（这里需要实现具体的击杀记录逻辑）
        result.setPlayerKills(0);
        result.setPlayerDeaths(context.getPlayer().getAlive() ? 0 : 1);
        result.setDurationSeconds(context.getCurrentTurn() * 30); // 假设每回合30秒

        return result;
    }
}