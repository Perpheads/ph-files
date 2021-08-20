-- ph_files.users definition

CREATE TABLE `users`
(
    `user_id`  int          NOT NULL AUTO_INCREMENT,
    `name`     varchar(40)  NOT NULL,
    `email`    varchar(50)  NOT NULL,
    `password` varchar(128) NOT NULL,
    `api_key`  varchar(50)  NOT NULL,
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `api_key` (`api_key`)
);


-- ph_files.cookies definition

CREATE TABLE `cookies`
(
    `cookie_id`   varchar(32) NOT NULL,
    `user_id`     int                  DEFAULT NULL,
    `cookie`      text,
    `create_date` timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`cookie_id`),
    KEY           `cookies_user_fk` (`user_id`),
    CONSTRAINT `cookies_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
);


CREATE TABLE `files`
(
    `file_id`     int          NOT NULL AUTO_INCREMENT,
    `link`        varchar(21)  NOT NULL,
    `file_name`   varchar(200) NOT NULL,
    `mime_type`   varchar(255)          DEFAULT NULL,
    `user_id`     int          NOT NULL,
    `upload_date` timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `size`        int unsigned NOT NULL DEFAULT '0',
    `thumbnail`   blob,
    `md5`         varchar(32)           DEFAULT NULL,
    PRIMARY KEY (`file_id`),
    UNIQUE KEY `link` (`link`),
    UNIQUE KEY `link_2` (`link`),
    KEY           `file_user_fk` (`user_id`),
    FULLTEXT KEY `file_name_fulltext` (`file_name`),
    CONSTRAINT `files_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
);