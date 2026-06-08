-- DropForeignKey
ALTER TABLE `cart_items` DROP FOREIGN KEY `cart_items_albumId_fkey`;

-- DropForeignKey
ALTER TABLE `order_items` DROP FOREIGN KEY `order_items_albumId_fkey`;

-- AlterTable
ALTER TABLE `order_items` MODIFY `albumId` INTEGER NULL;

-- AddForeignKey
ALTER TABLE `cart_items` ADD CONSTRAINT `cart_items_albumId_fkey` FOREIGN KEY (`albumId`) REFERENCES `albums`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `order_items` ADD CONSTRAINT `order_items_albumId_fkey` FOREIGN KEY (`albumId`) REFERENCES `albums`(`id`) ON DELETE SET NULL ON UPDATE CASCADE;
