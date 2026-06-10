-- AlterTable
ALTER TABLE `albums` ADD COLUMN `artistId` INTEGER NULL;

-- CreateTable
CREATE TABLE `artists` (
    `id` INTEGER NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(191) NOT NULL,
    `slug` VARCHAR(191) NOT NULL,
    `photo` VARCHAR(191) NULL,
    `foundedYear` INTEGER NULL,
    `country` VARCHAR(191) NULL,
    `description` TEXT NULL,
    `createdAt` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updatedAt` DATETIME(3) NOT NULL,

    UNIQUE INDEX `artists_slug_key`(`slug`),
    PRIMARY KEY (`id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- AddForeignKey
ALTER TABLE `albums` ADD CONSTRAINT `albums_artistId_fkey` FOREIGN KEY (`artistId`) REFERENCES `artists`(`id`) ON DELETE SET NULL ON UPDATE CASCADE;
