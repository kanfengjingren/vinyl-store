-- AlterTable
ALTER TABLE `orders` ADD COLUMN `shippingAddress` TEXT NULL;

-- AlterTable
ALTER TABLE `users` ADD COLUMN `defaultAddress` TEXT NULL;
