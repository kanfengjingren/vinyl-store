-- AlterTable
ALTER TABLE `comments` ADD COLUMN `replyToUserId` INTEGER NULL,
    ADD COLUMN `replyToUserName` VARCHAR(191) NULL;
