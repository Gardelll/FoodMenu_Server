-- 数据库结构，导出日期：2020年01月19日

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


--
-- 表的结构 `comment`
--

CREATE TABLE `comment`
(
    `cid`        bigint(25) UNSIGNED NOT NULL,
    `uid`        bigint(25) UNSIGNED NOT NULL,
    `to_food_id` bigint(25) UNSIGNED NOT NULL,
    `score`      tinyint(2) UNSIGNED NOT NULL,
    `comment`    varchar(4096)       NOT NULL,
    `time`       bigint(25) UNSIGNED NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- --------------------------------------------------------

--
-- 表的结构 `menu`
--

CREATE TABLE `menu`
(
    `food_id`   bigint(25) UNSIGNED NOT NULL,
    `window_id` int(11) UNSIGNED    NOT NULL,
    `name`      varchar(255)        NOT NULL,
    `price`     text                         DEFAULT NULL,
    `food_time` text                         DEFAULT NULL,
    `score`     tinyint(2) UNSIGNED          DEFAULT NULL,
    `hot`       bigint(25) UNSIGNED NOT NULL DEFAULT 0,
    `img_url`   text                         DEFAULT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- --------------------------------------------------------

--
-- 表的结构 `menu_edit`
--

CREATE TABLE `menu_edit`
(
    `edit_id`    bigint(25) UNSIGNED NOT NULL,
    `to_food_id` bigint(25) UNSIGNED DEFAULT NULL,
    `window_id`  int(11) UNSIGNED    NOT NULL,
    `name`       varchar(255)        NOT NULL,
    `price`      text                DEFAULT NULL,
    `food_time`  text                DEFAULT NULL,
    `img_url`    text                DEFAULT NULL,
    `time`       bigint(25) UNSIGNED NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- --------------------------------------------------------

--
-- 表的结构 `users`
--

CREATE TABLE `users`
(
    `uid`            bigint(25) UNSIGNED NOT NULL,
    `user_name`      text                NOT NULL,
    `user_pass`      text                NOT NULL,
    `token`          text                NOT NULL,
    `token_expire`   datetime            NOT NULL DEFAULT current_timestamp(),
    `nick_name`      text                NOT NULL,
    `school_num`     int(11) UNSIGNED    NOT NULL,
    `phone`          text                NOT NULL,
    `phone_verified` tinyint(1) UNSIGNED NOT NULL,
    `email`          text                NOT NULL,
    `email_verified` tinyint(1) UNSIGNED NOT NULL,
    `avatar`         varchar(1024)                DEFAULT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- --------------------------------------------------------

--
-- 表的结构 `version_info`
--

CREATE TABLE `version_info`
(
    `update_time`    int(25)       NOT NULL,
    `version_code`   int(11)       NOT NULL,
    `version_string` varchar(40)   NOT NULL,
    `update_summary` varchar(1024) NOT NULL,
    `download_url`   varchar(1024) NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

--
-- 转储表的索引
--

--
-- 表的索引 `comment`
--
ALTER TABLE `comment`
    ADD PRIMARY KEY (`cid`);

--
-- 表的索引 `menu`
--
ALTER TABLE `menu`
    ADD PRIMARY KEY (`food_id`),
    ADD KEY `window_id` (`window_id`),
    ADD KEY `name` (`name`(191));

--
-- 表的索引 `menu_edit`
--
ALTER TABLE `menu_edit`
    ADD PRIMARY KEY (`edit_id`),
    ADD KEY `window_id` (`window_id`),
    ADD KEY `name` (`name`(191));

--
-- 表的索引 `users`
--
ALTER TABLE `users`
    ADD PRIMARY KEY (`uid`);

--
-- 表的索引 `version_info`
--
ALTER TABLE `version_info`
    ADD PRIMARY KEY (`update_time`);

--
-- 在导出的表使用AUTO_INCREMENT
--

--
-- 使用表AUTO_INCREMENT `comment`
--
ALTER TABLE `comment`
    MODIFY `cid` bigint(25) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- 使用表AUTO_INCREMENT `menu`
--
ALTER TABLE `menu`
    MODIFY `food_id` bigint(25) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- 使用表AUTO_INCREMENT `menu_edit`
--
ALTER TABLE `menu_edit`
    MODIFY `edit_id` bigint(25) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- 使用表AUTO_INCREMENT `users`
--
ALTER TABLE `users`
    MODIFY `uid` bigint(25) UNSIGNED NOT NULL AUTO_INCREMENT;
COMMIT;
