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

 Date: 21/11/2025 21:18:31
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for backpack
-- ----------------------------
DROP TABLE IF EXISTS `backpack`;
CREATE TABLE `backpack`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL DEFAULT 1 COMMENT '用户ID',
  `item_id` bigint NOT NULL,
  `current_durability` int NULL DEFAULT NULL COMMENT '当前耐久',
  `quantity` bigint NULL DEFAULT NULL COMMENT '数量',
  `max_durability` int NULL DEFAULT NULL COMMENT '最大耐久',
  `purchase_price` int NOT NULL COMMENT '购买价格',
  `purchase_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '购买时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_item_id`(`item_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of backpack
-- ----------------------------
INSERT INTO `backpack` VALUES (10, 1, 2, 171, 1, 200, 549, NULL);
INSERT INTO `backpack` VALUES (11, 1, 7, NULL, 5, NULL, 37, NULL);
INSERT INTO `backpack` VALUES (12, 1, 1, 30, 1, 30, 137, NULL);

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
-- Table structure for user_wallet
-- ----------------------------
DROP TABLE IF EXISTS `user_wallet`;
CREATE TABLE `user_wallet`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL DEFAULT 1 COMMENT '用户ID',
  `money` int NOT NULL DEFAULT 10000,
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_wallet
-- ----------------------------
INSERT INTO `user_wallet` VALUES (1, 1, 9137, '2025-11-21 19:28:27');

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
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (4, 'liuyang', '123456', 'Lys', '2025-11-21 20:56:05', '2025-11-21 20:58:57', 1);

SET FOREIGN_KEY_CHECKS = 1;
