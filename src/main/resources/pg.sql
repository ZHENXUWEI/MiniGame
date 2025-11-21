/*
 Navicat Premium Dump SQL

 Source Server         : pg
 Source Server Type    : MySQL
 Source Server Version : 80029 (8.0.29)
 Source Host           : localhost:3306
 Source Schema         : pg

 Target Server Type    : MySQL
 Target Server Version : 80029 (8.0.29)
 File Encoding         : 65001

 Date: 22/11/2025 02:34:11
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for backpack
-- ----------------------------
DROP TABLE IF EXISTS `backpack`;
CREATE TABLE `backpack`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL DEFAULT 1 COMMENT '用户ID',
  `item_id` bigint NOT NULL,
  `current_durability` int NULL DEFAULT NULL COMMENT '当前耐久',
  `quantity` bigint NULL DEFAULT NULL COMMENT '数量',
  `max_durability` int NULL DEFAULT NULL COMMENT '最大耐久',
  `purchase_price` int NOT NULL COMMENT '购买价格',
  `purchase_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '购买时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_item_id`(`item_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of backpack
-- ----------------------------
INSERT INTO `backpack` VALUES (16, 6, 1, 29, 1, 30, 140, NULL);
INSERT INTO `backpack` VALUES (17, 6, 2, 196, 1, 200, 1017, NULL);
INSERT INTO `backpack` VALUES (18, 6, 7, NULL, 3, NULL, 111, NULL);

-- ----------------------------
-- Table structure for battle_records
-- ----------------------------
DROP TABLE IF EXISTS `battle_records`;
CREATE TABLE `battle_records`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `battle_type` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'QUICK_MATCH, RANKED, TOURNAMENT',
  `result` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `kills` int NULL DEFAULT 0,
  `exp_gained` int NULL DEFAULT 0,
  `money_gained` int NULL DEFAULT 0,
  `performance_change` int NULL DEFAULT 0,
  `rank_score_change` int NULL DEFAULT 0,
  `battle_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `duration_seconds` int NOT NULL,
  `enemy_data` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
  `experience_gained` int NOT NULL,
  `mode` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `performance_gained` int NOT NULL,
  `rank_points_gained` int NULL DEFAULT NULL,
  `team_data` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_battle_time`(`battle_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of battle_records
-- ----------------------------

-- ----------------------------
-- Table structure for items
-- ----------------------------
DROP TABLE IF EXISTS `items`;
CREATE TABLE `items`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `名称` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `伤害` float NOT NULL,
  `价值` int NOT NULL,
  `耐久` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `类型` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `使用子弹` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 26 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of items
-- ----------------------------
INSERT INTO `items` VALUES (1, '生锈匕首', 25, 150, '30', '近战武器', NULL);
INSERT INTO `items` VALUES (2, '格洛克-1', 15, 850, '200', '手枪', '9x19');
INSERT INTO `items` VALUES (3, 'T951', 35, 2100, '750', '步枪', '5.8x42');
INSERT INTO `items` VALUES (4, 'Kar98', 100, 5550, '120', '栓动狙击枪', '762x54');
INSERT INTO `items` VALUES (5, 'M110', 85, 4850, '200', '连发狙击枪', '762x51');
INSERT INTO `items` VALUES (6, 'AK47', 38, 2200, '810', '步枪', '762x39');
INSERT INTO `items` VALUES (7, '9x19子弹狩猎型', 0.75, 40, NULL, '子弹', NULL);
INSERT INTO `items` VALUES (8, '9x19子弹打靶型', 0.6, 20, NULL, '子弹', NULL);
INSERT INTO `items` VALUES (9, '9x19子弹穿甲型', 0.85, 80, NULL, '子弹', NULL);
INSERT INTO `items` VALUES (10, '9x19子弹特种型', 1, 120, NULL, '子弹', NULL);
INSERT INTO `items` VALUES (11, '5.8x42子弹打靶型', 0.8, 420, NULL, '子弹', NULL);
INSERT INTO `items` VALUES (12, '5.8x42子弹特种型', 1, 760, NULL, '子弹', NULL);
INSERT INTO `items` VALUES (13, '762x39子弹狩猎型', 0.8, 420, NULL, '子弹', NULL);
INSERT INTO `items` VALUES (14, '762x39子弹打靶型', 0.6, 210, NULL, '子弹', NULL);
INSERT INTO `items` VALUES (15, '762x39子弹穿甲型', 0.95, 570, NULL, '子弹', NULL);
INSERT INTO `items` VALUES (16, '762x39子弹特种型', 1.05, 780, NULL, '子弹', NULL);
INSERT INTO `items` VALUES (17, '762x51子弹狩猎型', 0.75, 1200, NULL, '子弹', NULL);
INSERT INTO `items` VALUES (18, '762x51子弹打靶型', 0.6, 930, NULL, '子弹', NULL);
INSERT INTO `items` VALUES (19, '762x51子弹穿甲型', 0.85, 1600, NULL, '子弹', NULL);
INSERT INTO `items` VALUES (20, '762x51子弹特种型', 1, 2100, NULL, '子弹', NULL);
INSERT INTO `items` VALUES (21, '762x54子弹狩猎型', 0.8, 2100, NULL, '子弹', NULL);
INSERT INTO `items` VALUES (22, '762x54子弹打靶型', 0.7, 1440, NULL, '子弹', NULL);
INSERT INTO `items` VALUES (23, '762x54子弹穿甲型', 0.9, 2900, NULL, '子弹', NULL);
INSERT INTO `items` VALUES (24, '762x54子弹特种型', 1, 4100, NULL, '子弹', NULL);
INSERT INTO `items` VALUES (25, '皮质防刺背心', 20, 300, '30', '护甲', NULL);

-- ----------------------------
-- Table structure for ranking_bots
-- ----------------------------
DROP TABLE IF EXISTS `ranking_bots`;
CREATE TABLE `ranking_bots`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `level` int NOT NULL,
  `losses` int NOT NULL,
  `name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `performance` int NOT NULL,
  `rank_points` int NOT NULL,
  `updated_time` datetime(6) NULL DEFAULT NULL,
  `wins` int NOT NULL,
  `created_time` datetime(6) NULL DEFAULT NULL,
  `total_deaths` int NOT NULL,
  `total_kills` int NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UKjy5t9r3i3i6ebrieqiqq9a8uk`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 601 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ranking_bots
-- ----------------------------
INSERT INTO `ranking_bots` VALUES (551, 49, 441, '烈焰守卫408', 366, 19123, '2025-11-22 02:31:48.558023', 667, '2025-11-22 02:24:21.040740', 892, 1343);
INSERT INTO `ranking_bots` VALUES (552, 39, 281, '冰霜刺客785', 326, 17622, '2025-11-22 02:31:48.559023', 626, '2025-11-22 02:24:21.055663', 572, 1272);
INSERT INTO `ranking_bots` VALUES (553, 47, 198, '王者传奇280', 104, 19232, '2025-11-22 02:31:48.559023', 676, '2025-11-22 02:24:21.070095', 410, 1360);
INSERT INTO `ranking_bots` VALUES (554, 43, 260, '幽灵杀手752', 479, 10745, '2025-11-22 02:31:48.559023', 394, '2025-11-22 02:24:21.085663', 526, 807);
INSERT INTO `ranking_bots` VALUES (555, 18, 200, '雷霆枪手452', 500, 12988, '2025-11-22 02:31:48.559023', 466, '2025-11-22 02:24:21.100665', 406, 937);
INSERT INTO `ranking_bots` VALUES (556, 50, 165, '钢铁英雄322', 218, 11751, '2025-11-22 02:31:48.560024', 437, '2025-11-22 02:24:21.115598', 340, 880);
INSERT INTO `ranking_bots` VALUES (557, 9, 160, '幽灵猎手361', 432, 12063, '2025-11-22 02:31:48.560024', 418, '2025-11-22 02:24:21.130557', 331, 847);
INSERT INTO `ranking_bots` VALUES (558, 5, 141, '战神传奇786', 271, 10506, '2025-11-22 02:31:48.560024', 382, '2025-11-22 02:24:21.144990', 285, 778);
INSERT INTO `ranking_bots` VALUES (559, 51, 145, '雷霆先锋332', 194, 9139, '2025-11-22 02:31:48.560024', 317, '2025-11-22 02:24:21.160563', 303, 640);
INSERT INTO `ranking_bots` VALUES (560, 31, 123, '烈焰守卫863', 283, 9124, '2025-11-22 02:31:48.560024', 306, '2025-11-22 02:24:21.175661', 259, 621);
INSERT INTO `ranking_bots` VALUES (561, 29, 111, '烈焰枪手955', 427, 8200, '2025-11-22 02:31:48.560024', 303, '2025-11-22 02:24:21.190313', 229, 611);
INSERT INTO `ranking_bots` VALUES (562, 19, 203, '幽灵英雄406', 430, 9411, '2025-11-22 02:31:48.560024', 356, '2025-11-22 02:24:21.204320', 415, 728);
INSERT INTO `ranking_bots` VALUES (563, 50, 166, '幽灵传奇804', 177, 8031, '2025-11-22 02:31:48.560024', 301, '2025-11-22 02:24:21.219324', 340, 608);
INSERT INTO `ranking_bots` VALUES (564, 26, 123, '烈焰战士566', 166, 9215, '2025-11-22 02:31:48.560024', 324, '2025-11-22 02:24:21.234329', 255, 653);
INSERT INTO `ranking_bots` VALUES (565, 44, 151, '暗影守卫218', 150, 8194, '2025-11-22 02:31:48.560024', 297, '2025-11-22 02:24:21.249332', 306, 606);
INSERT INTO `ranking_bots` VALUES (566, 38, 143, '雷霆先锋159', 217, 7339, '2025-11-22 02:31:48.560024', 253, '2025-11-22 02:24:21.264279', 289, 511);
INSERT INTO `ranking_bots` VALUES (567, 44, 149, '战神杀手605', 275, 7204, '2025-11-22 02:31:48.560024', 276, '2025-11-22 02:24:21.278392', 300, 562);
INSERT INTO `ranking_bots` VALUES (568, 10, 173, '黄金大师964', 74, 6830, '2025-11-22 02:31:48.560024', 255, '2025-11-22 02:24:21.293396', 357, 528);
INSERT INTO `ranking_bots` VALUES (569, 12, 196, '冰霜猎手908', 149, 7473, '2025-11-22 02:31:48.560024', 276, '2025-11-22 02:24:21.307496', 398, 564);
INSERT INTO `ranking_bots` VALUES (570, 13, 153, '烈焰大师814', 413, 6901, '2025-11-22 02:31:48.560024', 264, '2025-11-22 02:24:21.321495', 312, 540);
INSERT INTO `ranking_bots` VALUES (571, 12, 104, '冰霜刺客537', 435, 8013, '2025-11-22 02:31:48.560024', 300, '2025-11-22 02:24:21.337082', 212, 610);
INSERT INTO `ranking_bots` VALUES (572, 12, 82, '幽灵传奇454', 194, 6603, '2025-11-22 02:31:48.560024', 274, '2025-11-22 02:24:21.351995', 171, 557);
INSERT INTO `ranking_bots` VALUES (573, 24, 178, '王者英雄697', 353, 6819, '2025-11-22 02:31:48.560024', 272, '2025-11-22 02:24:21.366668', 367, 557);
INSERT INTO `ranking_bots` VALUES (574, 48, 115, '战神猎手273', 312, 7351, '2025-11-22 02:31:48.560024', 268, '2025-11-22 02:24:21.381400', 242, 537);
INSERT INTO `ranking_bots` VALUES (575, 19, 103, '风暴传奇963', 109, 7010, '2025-11-22 02:31:48.560024', 286, '2025-11-22 02:24:21.396398', 215, 592);
INSERT INTO `ranking_bots` VALUES (576, 22, 97, '战神守卫489', 266, 5512, '2025-11-22 02:31:48.560024', 191, '2025-11-22 02:24:21.411402', 208, 402);
INSERT INTO `ranking_bots` VALUES (577, 46, 130, '风暴战士150', 143, 6170, '2025-11-22 02:31:48.560024', 221, '2025-11-22 02:24:21.425957', 268, 450);
INSERT INTO `ranking_bots` VALUES (578, 8, 117, '幽灵战士383', 352, 5423, '2025-11-22 02:31:48.560024', 186, '2025-11-22 02:24:21.440851', 246, 387);
INSERT INTO `ranking_bots` VALUES (579, 16, 141, '钢铁刺客625', 405, 6294, '2025-11-22 02:31:48.560024', 220, '2025-11-22 02:24:21.455858', 282, 444);
INSERT INTO `ranking_bots` VALUES (580, 23, 113, '风暴传奇502', 195, 5727, '2025-11-22 02:31:48.560024', 225, '2025-11-22 02:24:21.470860', 228, 454);
INSERT INTO `ranking_bots` VALUES (581, 21, 133, '黄金刺客846', 126, 6075, '2025-11-22 02:31:48.560024', 221, '2025-11-22 02:24:21.485502', 268, 453);
INSERT INTO `ranking_bots` VALUES (582, 17, 109, '冰霜猎手442', 293, 6123, '2025-11-22 02:31:48.560024', 236, '2025-11-22 02:24:21.499844', 232, 486);
INSERT INTO `ranking_bots` VALUES (583, 34, 99, '雷霆猎手796', 88, 5234, '2025-11-22 02:31:48.560024', 183, '2025-11-22 02:24:21.514878', 214, 371);
INSERT INTO `ranking_bots` VALUES (584, 33, 114, '钢铁猎手171', 380, 6184, '2025-11-22 02:31:48.560024', 256, '2025-11-22 02:24:21.528880', 240, 530);
INSERT INTO `ranking_bots` VALUES (585, 18, 90, '钢铁刺客385', 163, 5826, '2025-11-22 02:31:48.560024', 216, '2025-11-22 02:24:21.543884', 190, 438);
INSERT INTO `ranking_bots` VALUES (586, 47, 118, '王者刺客376', 120, 4673, '2025-11-22 02:31:48.560024', 209, '2025-11-22 02:24:21.557839', 249, 426);
INSERT INTO `ranking_bots` VALUES (587, 9, 89, '冰霜传奇978', 67, 4286, '2025-11-22 02:31:48.560024', 175, '2025-11-22 02:24:21.572843', 179, 362);
INSERT INTO `ranking_bots` VALUES (588, 12, 62, '黄金战士870', 316, 4010, '2025-11-22 02:31:48.560024', 170, '2025-11-22 02:24:21.587849', 130, 355);
INSERT INTO `ranking_bots` VALUES (589, 47, 77, '幽灵战士297', 209, 4705, '2025-11-22 02:31:48.561024', 193, '2025-11-22 02:24:21.601852', 159, 406);
INSERT INTO `ranking_bots` VALUES (590, 26, 86, '幽灵守卫580', 462, 3629, '2025-11-22 02:31:48.561024', 143, '2025-11-22 02:24:21.616855', 183, 298);
INSERT INTO `ranking_bots` VALUES (591, 41, 46, '暗影猎手400', 405, 2771, '2025-11-22 02:31:48.561024', 124, '2025-11-22 02:24:21.631810', 98, 253);
INSERT INTO `ranking_bots` VALUES (592, 48, 63, '黄金刺客547', 85, 2460, '2025-11-22 02:31:48.561024', 87, '2025-11-22 02:24:21.646819', 140, 192);
INSERT INTO `ranking_bots` VALUES (593, 47, 62, '幽灵传奇584', 446, 2506, '2025-11-22 02:31:48.561024', 102, '2025-11-22 02:24:21.661197', 136, 220);
INSERT INTO `ranking_bots` VALUES (594, 27, 85, '烈焰枪手126', 151, 2705, '2025-11-22 02:31:48.561024', 118, '2025-11-22 02:24:21.675205', 175, 253);
INSERT INTO `ranking_bots` VALUES (595, 4, 73, '暗影刺客311', 459, 2894, '2025-11-22 02:31:48.561024', 140, '2025-11-22 02:24:21.690210', 158, 281);
INSERT INTO `ranking_bots` VALUES (596, 7, 439, '风暴枪手786_1763749461026', 232, 18675, '2025-11-22 02:31:48.561024', 664, '2025-11-22 02:24:21.705627', 891, 1336);
INSERT INTO `ranking_bots` VALUES (597, 21, 255, '战神猎手573_1763749461026', 460, 18328, '2025-11-22 02:31:48.561024', 632, '2025-11-22 02:24:21.720666', 511, 1282);
INSERT INTO `ranking_bots` VALUES (598, 36, 210, '幽灵杀手955_1763749461026', 171, 15154, '2025-11-22 02:31:48.561024', 543, '2025-11-22 02:24:21.735353', 432, 1089);
INSERT INTO `ranking_bots` VALUES (599, 17, 351, '幽灵传奇654_1763749461026', 359, 14113, '2025-11-22 02:31:48.561024', 514, '2025-11-22 02:24:21.749465', 712, 1038);
INSERT INTO `ranking_bots` VALUES (600, 26, 237, '烈焰战士488_1763749461026', 322, 12604, '2025-11-22 02:31:48.561024', 464, '2025-11-22 02:24:21.764663', 475, 946);

-- ----------------------------
-- Table structure for user_battle_stats
-- ----------------------------
DROP TABLE IF EXISTS `user_battle_stats`;
CREATE TABLE `user_battle_stats`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `level` int NULL DEFAULT 1 COMMENT '等级',
  `exp` int NULL DEFAULT 0 COMMENT '经验值',
  `rank_score` int NULL DEFAULT 0 COMMENT '段位积分',
  `performance_score` int NULL DEFAULT 0 COMMENT '状态表现分',
  `wins` int NULL DEFAULT 0 COMMENT '胜场',
  `losses` int NULL DEFAULT 0 COMMENT '负场',
  `kills` int NULL DEFAULT 0 COMMENT '总击杀',
  `deaths` int NULL DEFAULT 0 COMMENT '总死亡',
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_rank_score`(`rank_score` ASC) USING BTREE,
  INDEX `idx_level`(`level` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_battle_stats
-- ----------------------------

-- ----------------------------
-- Table structure for user_stats
-- ----------------------------
DROP TABLE IF EXISTS `user_stats`;
CREATE TABLE `user_stats`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `experience` int NOT NULL,
  `level` int NOT NULL,
  `losses` int NOT NULL,
  `performance` int NOT NULL,
  `rank_points` int NOT NULL,
  `total_deaths` int NOT NULL,
  `total_kills` int NOT NULL,
  `updated_time` datetime(6) NULL DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `wins` int NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UKcgfgfs7fk42h7ck71lrs42sou`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_stats
-- ----------------------------
INSERT INTO `user_stats` VALUES (1, 0, 1, 0, 50, 0, 0, 0, '2025-11-22 00:29:06.703367', 6, 0);

-- ----------------------------
-- Table structure for user_wallet
-- ----------------------------
DROP TABLE IF EXISTS `user_wallet`;
CREATE TABLE `user_wallet`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL DEFAULT 1,
  `money` int NOT NULL DEFAULT 10000,
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_wallet
-- ----------------------------
INSERT INTO `user_wallet` VALUES (1, 1, 9137, '2025-11-21 19:28:27');
INSERT INTO `user_wallet` VALUES (2, 5, 9136, NULL);
INSERT INTO `user_wallet` VALUES (3, 6, 8605, NULL);

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '密码（加密存储）',
  `nickname` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '昵称',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `status` int NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (6, 'playerLY', '123456', 'ly', '2025-11-21 21:47:51', '2025-11-22 02:32:27', 1);

SET FOREIGN_KEY_CHECKS = 1;
