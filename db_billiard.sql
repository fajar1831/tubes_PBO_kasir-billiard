-- MariaDB dump 10.19  Distrib 10.4.32-MariaDB, for Win64 (AMD64)
--
-- Host: localhost    Database: db_billiard
-- ------------------------------------------------------
-- Server version	10.4.32-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `members`
--

DROP TABLE IF EXISTS `members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `members` (
  `member_id` varchar(20) NOT NULL,
  `member_name` varchar(100) NOT NULL,
  `phone_number` varchar(20) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `member_category` enum('Reguler','Gold','Atlet') NOT NULL DEFAULT 'Reguler',
  `registration_date` date DEFAULT NULL,
  `expiry_date` date DEFAULT NULL,
  `points` int(11) DEFAULT 0,
  `is_active` tinyint(1) DEFAULT 1,
  PRIMARY KEY (`member_id`),
  UNIQUE KEY `phone_number` (`phone_number`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `members`
--

LOCK TABLES `members` WRITE;
/*!40000 ALTER TABLE `members` DISABLE KEYS */;
INSERT INTO `members` VALUES ('100','lutfi','082314658097','lzulfian@gmail.com','Reguler','2024-12-12','2025-01-12',0,0),('110','fajar','081352467809','fajarsd@gmail.com','Gold','2025-05-06','2025-10-06',0,1),('111','Kevin','083127892634','auliakev@gmail.com','Gold','2025-05-06','2026-03-06',0,1),('113','fadli','081326547098','fmuzaki@gmail.com','Gold','2025-05-06','2026-03-06',0,1),('210','reyhan','085123476809','anandarey@gmail.com','Atlet','2025-05-06','2027-05-06',0,1);
/*!40000 ALTER TABLE `members` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `transaction_items`
--

DROP TABLE IF EXISTS `transaction_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `transaction_items` (
  `item_id` int(11) NOT NULL AUTO_INCREMENT,
  `transaction_id` int(11) NOT NULL,
  `item_name` varchar(100) NOT NULL,
  `item_type` varchar(20) NOT NULL,
  `quantity` int(11) DEFAULT NULL,
  `price_per_item` decimal(15,2) DEFAULT NULL,
  `total_price` decimal(15,2) DEFAULT NULL,
  PRIMARY KEY (`item_id`),
  KEY `transaction_id` (`transaction_id`),
  CONSTRAINT `transaction_items_ibfk_1` FOREIGN KEY (`transaction_id`) REFERENCES `transactions` (`transaction_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `transaction_items`
--

LOCK TABLES `transaction_items` WRITE;
/*!40000 ALTER TABLE `transaction_items` DISABLE KEYS */;
INSERT INTO `transaction_items` VALUES (1,1,'Minuman Soda','FNB',2,12000.00,24000.00),(2,1,'Paket Pagi Happy (3 Jam)','PAKET',1,100000.00,100000.00),(3,1,'Paket Ngemil Santai','PAKET',1,30000.00,30000.00),(4,2,'Paket Pagi Happy (3 Jam)','PAKET',1,100000.00,100000.00),(5,2,'Paket Duo F&B','PAKET',1,45000.00,45000.00),(6,3,'Kentang Goreng','FNB',4,20000.00,80000.00),(7,3,'Minuman Soda','FNB',4,12000.00,48000.00),(8,6,'Paket Pagi Happy (3 Jam)','PAKET',1,100000.00,100000.00),(9,7,'Mie Instan Kuah','FNB',1,15000.00,15000.00),(10,7,'Kentang Goreng','FNB',1,20000.00,20000.00),(11,7,'Roti Bakar Coklat Keju','FNB',1,18000.00,18000.00),(12,7,'Air Mineral Botol','FNB',1,5000.00,5000.00),(13,7,'Minuman Soda','FNB',1,12000.00,12000.00),(14,8,'Paket Pagi Happy (3 Jam)','PAKET',1,100000.00,100000.00);
/*!40000 ALTER TABLE `transaction_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `transactions`
--

DROP TABLE IF EXISTS `transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `transactions` (
  `transaction_id` int(11) NOT NULL AUTO_INCREMENT,
  `table_name` varchar(50) NOT NULL,
  `customer_name` varchar(100) DEFAULT NULL,
  `start_time` time DEFAULT NULL,
  `end_time` time DEFAULT NULL,
  `total_play_duration_minutes` int(11) DEFAULT NULL,
  `rental_cost` decimal(15,2) DEFAULT NULL,
  `individual_fnb_cost` decimal(15,2) DEFAULT NULL,
  `package_cost` decimal(15,2) DEFAULT NULL,
  `additional_cost` decimal(15,2) DEFAULT NULL,
  `member_discount_amount` decimal(15,2) DEFAULT NULL,
  `manual_discount_amount` decimal(15,2) DEFAULT NULL,
  `total_bill` decimal(15,2) NOT NULL,
  `payment_method` varchar(50) DEFAULT NULL,
  `amount_paid` decimal(15,2) DEFAULT NULL,
  `change_given` decimal(15,2) DEFAULT NULL,
  `transaction_timestamp` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`transaction_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `transactions`
--

LOCK TABLES `transactions` WRITE;
/*!40000 ALTER TABLE `transactions` DISABLE KEYS */;
INSERT INTO `transactions` VALUES (1,'Meja 1','fajar','10:00:00','13:00:00',180,0.00,24000.00,130000.00,0.00,0.00,0.00,154000.00,'QRIS',154000.00,0.00,'2025-06-05 00:23:50'),(2,'Meja 1','reyhan','09:15:00','12:15:00',180,0.00,0.00,145000.00,0.00,29000.00,0.00,116000.00,'Tunai',120000.00,4000.00,'2025-06-05 01:15:47'),(3,'VIP 1','kevin','09:00:00','11:00:00',120,60000.00,128000.00,0.00,0.00,18800.00,0.00,169200.00,'QRIS',169200.00,0.00,'2025-06-05 01:16:59'),(4,'Meja 5','lutfi','08:22:00','10:00:00',98,61666.67,0.00,0.00,0.00,0.00,0.00,61666.67,'Tunai',70000.00,8333.33,'2025-06-05 01:23:02'),(5,'Meja 1','lutfi','08:38:00','11:16:00',158,86333.33,0.00,0.00,0.00,0.00,0.00,86333.33,'Transfer Bank',86333.33,0.00,'2025-06-05 01:42:00'),(6,'Meja 1','taufik','09:24:00','12:24:00',180,0.00,0.00,100000.00,0.00,0.00,0.00,100000.00,'QRIS',100000.00,0.00,'2025-06-05 02:25:34'),(7,'Meja 4','fakhir','10:00:00','12:53:00',173,86500.00,70000.00,0.00,0.00,0.00,0.00,156500.00,'QRIS',156500.00,0.00,'2025-06-05 05:53:54'),(8,'Meja 6','septya','17:19:00','20:19:00',180,0.00,0.00,100000.00,0.00,0.00,0.00,100000.00,'QRIS',100000.00,0.00,'2025-06-08 10:21:05');
/*!40000 ALTER TABLE `transactions` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-06-08 17:32:32
