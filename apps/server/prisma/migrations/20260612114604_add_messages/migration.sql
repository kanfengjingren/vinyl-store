-- CreateTable
CREATE TABLE `messages` (
    `id` INTEGER NOT NULL AUTO_INCREMENT,
    `senderId` INTEGER NOT NULL,
    `receiverId` INTEGER NOT NULL,
    `content` TEXT NOT NULL,
    `read` BOOLEAN NOT NULL DEFAULT false,
    `createdAt` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    INDEX `messages_senderId_receiverId_idx`(`senderId`, `receiverId`),
    INDEX `messages_receiverId_idx`(`receiverId`),
    PRIMARY KEY (`id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- AddForeignKey
ALTER TABLE `messages` ADD CONSTRAINT `messages_senderId_fkey` FOREIGN KEY (`senderId`) REFERENCES `users`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `messages` ADD CONSTRAINT `messages_receiverId_fkey` FOREIGN KEY (`receiverId`) REFERENCES `users`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;
