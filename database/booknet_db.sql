-- MySQL dump 10.13  Distrib 26.7.0, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: booknet_db
-- ------------------------------------------------------
-- Server version	26.7.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `booknet_db`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `booknet_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `booknet_db`;

--
-- Table structure for table `account_access_audits`
--

DROP TABLE IF EXISTS `account_access_audits`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `account_access_audits` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `action` varchar(30) NOT NULL,
  `actor_id` bigint NOT NULL,
  `new_value` varchar(100) NOT NULL,
  `previous_value` varchar(100) NOT NULL,
  `reason` varchar(300) DEFAULT NULL,
  `target_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `account_access_audits`
--

LOCK TABLES `account_access_audits` WRITE;
/*!40000 ALTER TABLE `account_access_audits` DISABLE KEYS */;
/*!40000 ALTER TABLE `account_access_audits` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `account_tokens`
--

DROP TABLE IF EXISTS `account_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `account_tokens` (
  `expires_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `issued_at` datetime(6) NOT NULL,
  `used_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `token_hash` varchar(64) NOT NULL,
  `purpose` enum('INVITE_ACCOUNT','RESET_PASSWORD','VERIFY_EMAIL') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK25jalhpd9d1y7l4fq6mdsnb1d` (`token_hash`),
  KEY `idx_account_token_user` (`user_id`,`purpose`),
  CONSTRAINT `FK19h8y4psfcpn5ct8b9mm2o2na` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `account_tokens`
--

LOCK TABLES `account_tokens` WRITE;
/*!40000 ALTER TABLE `account_tokens` DISABLE KEYS */;
/*!40000 ALTER TABLE `account_tokens` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `audit_logs`
--

DROP TABLE IF EXISTS `audit_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` varchar(60) NOT NULL,
  `actor_email` varchar(150) NOT NULL,
  `actor_role` varchar(30) DEFAULT NULL,
  `description` text,
  `occurred_at` datetime(6) NOT NULL,
  `target_id` bigint DEFAULT NULL,
  `target_type` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_audit_occurred_at` (`occurred_at` DESC),
  KEY `idx_audit_actor` (`actor_email`),
  KEY `idx_audit_target` (`target_type`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_logs`
--

LOCK TABLES `audit_logs` WRITE;
/*!40000 ALTER TABLE `audit_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `audit_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `authors`
--

DROP TABLE IF EXISTS `authors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `authors` (
  `id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `name` varchar(150) NOT NULL,
  `bio` text,
  `image_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `authors`
--

LOCK TABLES `authors` WRITE;
/*!40000 ALTER TABLE `authors` DISABLE KEYS */;
INSERT INTO `authors` VALUES (1,'2026-11-25 11:05:54.000000','2026-11-26 10:28:42.000000','Joshua Bloch','Author of Effective Java','/covers/4bfdf1d3-b606-4083-955b-692deba78fd6.jpg'),(2,'2026-11-25 11:05:54.000000','2026-11-26 10:28:43.000000','Robert C. Martin','Author of Clean Code','/covers/ddeebcb5-09ce-4934-a7c0-8cfaf93c3a79.jpg'),(3,'2026-11-25 11:05:54.000000','2026-11-26 10:28:44.000000','Martin Fowler','Author of Refactoring','/covers/1cb8b1e5-a9b2-4061-a435-7f15e37726d0.jpg'),(4,'2026-11-25 11:05:54.000000','2026-11-26 10:29:47.000000','Nguyễn Nhật Ánh','Vietnamese fiction author','/covers/e93cb336-36f5-48a9-a8ec-c70e0540ebc7.png'),(5,'2026-11-25 13:45:27.000000','2026-11-26 10:28:46.000000','David Thomas','Computer programmer, author and co-author of The Pragmatic Programmer.','/covers/7358fb57-d4b0-48bb-ae95-20bd57a8c91a.jpg'),(6,'2026-11-25 13:45:27.000000','2026-11-26 10:28:49.000000','Martin Kleppmann','Researcher in distributed systems and author at the University of Cambridge.','/covers/6fd053d0-8ef4-4ca3-bca8-d2c2f9296040.jpg'),(7,'2026-11-25 13:45:27.000000','2026-11-26 10:28:50.000000','James Clear','American writer and speaker focused on habits, decision making, and continuous improvement.','/covers/58cbd1cd-925e-4fac-9be3-aa1e08ef1edc.jpg'),(8,'2026-11-25 13:45:27.000000','2026-11-26 10:28:51.000000','Paulo Coelho','Brazilian lyricist and novelist, best known for his international bestseller The Alchemist.','/covers/59681d86-0f25-4921-a2bb-4e1927b0982d.jpg'),(9,'2026-11-25 13:45:27.000000','2026-11-26 10:28:51.000000','George Orwell','English novelist, essayist, journalist, and critic famous for 1984 and Animal Farm.','/covers/f9c16495-ccd3-4181-9fdd-a09d3414f594.jpg'),(10,'2026-11-25 13:45:27.000000','2026-11-26 10:28:52.000000','Yuval Noah Harari','Israeli public intellectual, historian and professor in the Department of History at the Hebrew University of Jerusalem.','/covers/b06d99b7-21e9-4224-b083-055ee56b24f7.jpg'),(11,'2026-11-25 13:45:27.000000','2026-11-26 10:28:52.000000','Daniel Kahneman','Nobel Prize-winning psychologist and economist known for research on judgment and decision-making.','/covers/fbd2e500-811c-433a-a1e4-8701c36b7ccd.jpg'),(12,'2026-11-25 13:45:27.000000','2026-11-26 10:28:53.000000','Frank Herbert','American science fiction author best known for the 1965 novel Dune.','/covers/f14fdf88-90ae-4dc4-bdca-737dfeef0eac.jpg'),(13,'2026-11-26 10:26:13.000000','2026-11-26 10:28:53.000000','Erich Gamma','Swiss computer scientist and co-author of the Gang of Four Design Patterns book.',NULL),(14,'2026-11-26 10:26:13.000000','2026-11-26 10:29:01.000000','Eric Evans','Author and thought leader in software modeling and Domain-Driven Design.','/covers/9b331100-ed6d-42ce-9969-4c601f7f5cc1.jpg'),(15,'2026-11-26 10:26:13.000000','2026-11-26 10:29:03.000000','Eric Freeman','Computer scientist, author, and former CTO of Disney Online.',NULL),(16,'2026-11-26 10:26:13.000000','2026-11-26 10:29:07.000000','Thomas H. Cormen','Professor Emeritus of Computer Science at Dartmouth College and co-author of CLRS.',NULL),(17,'2026-11-26 10:26:13.000000','2026-11-26 10:29:11.000000','J.R.R. Tolkien','English writer, philologist, and academic, creator of Middle-earth.','/covers/da1e7f63-5d6d-45a0-a5a6-17b1a3a2a494.jpg'),(18,'2026-11-26 10:26:13.000000','2026-11-26 10:29:13.000000','J.K. Rowling','British author and philanthropist best known for the Harry Potter fantasy series.','/covers/2057ab92-6069-4d57-b922-d8d181c0e95e.jpg'),(19,'2026-11-26 10:26:13.000000','2026-11-26 10:29:16.000000','F. Scott Fitzgerald','American novelist and short-story writer of the Jazz Age.','/covers/718cff63-68de-4416-bda9-59bba5197c70.jpg'),(20,'2026-11-26 10:26:13.000000','2026-11-26 10:29:19.000000','Harper Lee','American novelist widely known for her Pulitzer Prize-winning classic To Kill a Mockingbird.','/covers/54bf8603-34c9-4e8a-8b62-67cc8fd387ab.jpg'),(21,'2026-11-26 10:26:14.000000','2026-11-26 10:29:22.000000','Jane Austen','English novelist known for her sharp social commentary and romantic fiction.','/covers/416c8d25-87e4-42de-9fe9-0c3684da6521.jpg'),(22,'2026-11-26 10:26:14.000000','2026-11-26 10:29:24.000000','Fyodor Dostoevsky','Russian novelist, short story writer, and philosopher of psychological depth.','/covers/ae060939-7ef3-4c60-acc3-fb1d7c86a7ce.jpg'),(23,'2026-11-26 10:26:14.000000','2026-11-26 10:29:29.000000','Haruki Murakami','Japanese writer whose surreal and nostalgic fiction gained international acclaim.','/covers/9eac559d-891e-4db9-9236-c2b6c310f127.jpg'),(24,'2026-11-26 10:26:14.000000','2026-11-26 10:29:33.000000','Cal Newport','Computer science professor at Georgetown University and author on focus and technology.','/covers/fe0b6674-7734-47cf-b333-7e4f3dd40636.jpg'),(25,'2026-11-26 10:26:14.000000','2026-11-26 10:29:34.000000','Morgan Housel','Author and partner at Collaborative Fund writing on behavioral finance and investing.',NULL),(26,'2026-11-26 10:26:14.000000','2026-11-26 10:29:36.000000','Peter Thiel','Entrepreneur, venture capitalist, and co-founder of PayPal and Palantir.','/covers/f23a04a1-fd33-4bfc-a79e-8822fdd3724b.jpg'),(27,'2026-11-26 10:26:14.000000','2026-11-26 10:29:39.000000','Tara Westover','American memoirist, essayist, and historian awarded the National Humanities Medal.','/covers/81e41a59-0cd7-403e-b312-06cd844f4239.jpg'),(28,'2026-11-26 10:26:14.000000','2026-11-26 10:29:41.000000','Isaac Asimov','American writer and professor of biochemistry, prolific master of science fiction.','/covers/335ebff2-02b0-42da-92f2-102563c81564.jpg'),(29,'2026-11-26 10:26:14.000000','2026-11-26 10:29:44.000000','Sun Tzu','Ancient Chinese military general, strategist, and philosopher.','/covers/79e05178-f53a-42d2-95be-c94e7c48b449.jpg'),(30,'2026-11-26 10:26:14.000000','2026-11-26 10:29:47.000000','Tô Hoài','Nhà văn lớn của văn học Việt Nam hiện đại, tác giả nhiều tác phẩm kinh điển cho thiếu nhi.','/covers/ed3b4018-7af8-419c-bb17-6c912b5a2544.jpg');
/*!40000 ALTER TABLE `authors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `book_authors`
--

DROP TABLE IF EXISTS `book_authors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_authors` (
  `author_id` int NOT NULL,
  `book_id` bigint NOT NULL,
  PRIMARY KEY (`author_id`,`book_id`),
  KEY `FKbhqtkv2cndf10uhtknaqbyo0a` (`book_id`),
  CONSTRAINT `FKbhqtkv2cndf10uhtknaqbyo0a` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`),
  CONSTRAINT `FKo86065vktj3hy1m7syr9cn7va` FOREIGN KEY (`author_id`) REFERENCES `authors` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `book_authors`
--

LOCK TABLES `book_authors` WRITE;
/*!40000 ALTER TABLE `book_authors` DISABLE KEYS */;
INSERT INTO `book_authors` VALUES (1,1),(2,2),(3,3),(4,4),(5,5),(6,6),(7,7),(8,8),(9,9),(10,10),(11,11),(12,12),(13,13),(14,14),(15,15),(16,16),(17,17),(18,18),(19,19),(20,20),(21,21),(22,22),(23,23),(23,24),(24,25),(25,26),(26,27),(27,28),(28,29),(29,30),(30,31),(4,32);
/*!40000 ALTER TABLE `book_authors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `book_categories`
--

DROP TABLE IF EXISTS `book_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_categories` (
  `category_id` int NOT NULL,
  `book_id` bigint NOT NULL,
  PRIMARY KEY (`category_id`,`book_id`),
  KEY `FK3k3ahp5vqlgmrr9swqqprmbxy` (`book_id`),
  CONSTRAINT `FK3k3ahp5vqlgmrr9swqqprmbxy` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`),
  CONSTRAINT `FKrg2xlmc92mm2y5b1wmhd2g0y0` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `book_categories`
--

LOCK TABLES `book_categories` WRITE;
/*!40000 ALTER TABLE `book_categories` DISABLE KEYS */;
INSERT INTO `book_categories` VALUES (1,1),(1,2),(1,3),(2,4),(1,5),(1,6),(3,7),(2,8),(2,9),(4,10),(5,11),(6,12),(1,13),(1,14),(1,15),(1,16),(7,17),(7,18),(8,19),(8,20),(8,21),(8,22),(2,23),(2,24),(3,25),(9,26),(9,27),(10,28),(6,29),(11,30),(2,31),(2,32);
/*!40000 ALTER TABLE `book_categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `book_copies`
--

DROP TABLE IF EXISTS `book_copies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_copies` (
  `acquired_date` date DEFAULT NULL,
  `book_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `barcode` varchar(40) NOT NULL,
  `shelf_location` varchar(50) DEFAULT NULL,
  `condition` varchar(255) DEFAULT NULL,
  `status` enum('AVAILABLE','BORROWED','DAMAGED','DISCONTINUED','LOST','RESERVED') NOT NULL,
  `book-condition` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKs7b8kwf25ikn6y6hbbso8fn2f` (`barcode`),
  KEY `FKhlawea8y2e2dv0ta58vc6f5nr` (`book_id`),
  CONSTRAINT `FKhlawea8y2e2dv0ta58vc6f5nr` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=74 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `book_copies`
--

LOCK TABLES `book_copies` WRITE;
/*!40000 ALTER TABLE `book_copies` DISABLE KEYS */;
INSERT INTO `book_copies` VALUES ('2026-11-25',1,'2026-11-25 11:05:54.000000',1,'2026-11-25 06:24:28.766262','BC-EJ-01','A1-01',NULL,'BORROWED','Good'),('2026-11-25',1,'2026-11-25 11:05:54.000000',2,'2026-11-25 06:25:37.126153','BC-EJ-02','A1-01',NULL,'BORROWED','Good'),('2026-11-25',2,'2026-11-25 11:05:54.000000',3,'2026-11-25 06:24:28.766262','BC-CC-01','A1-02',NULL,'BORROWED','Good'),('2026-11-25',2,'2026-11-25 11:05:54.000000',4,'2026-11-25 11:05:54.000000','BC-CC-02','A1-02',NULL,'AVAILABLE','Good'),('2026-11-25',3,'2026-11-25 11:05:54.000000',5,'2026-11-25 06:25:37.127151','BC-RF-01','A1-03',NULL,'BORROWED','Good'),('2026-11-25',3,'2026-11-25 11:05:54.000000',6,'2026-11-25 11:05:54.000000','BC-RF-02','A1-03',NULL,'AVAILABLE','Good'),('2026-11-25',4,'2026-11-25 11:05:54.000000',7,'2026-11-25 06:25:37.127151','BC-MB-01','B2-01',NULL,'BORROWED','Good'),('2026-11-25',4,'2026-11-25 11:05:54.000000',8,'2026-11-25 11:05:54.000000','BC-MB-02','B2-01',NULL,'AVAILABLE','Good'),('2026-11-25',5,'2026-11-25 13:45:27.000000',9,'2026-11-25 13:45:27.000000','BC-PP-01','A1-04',NULL,'AVAILABLE','Good'),('2026-11-25',5,'2026-11-25 13:45:27.000000',10,'2026-11-25 13:45:27.000000','BC-PP-02','A1-04',NULL,'AVAILABLE','Good'),('2026-11-25',6,'2026-11-25 13:45:27.000000',11,'2026-11-25 13:45:27.000000','BC-DDIA-01','A1-05',NULL,'AVAILABLE','Good'),('2026-11-25',6,'2026-11-25 13:45:27.000000',12,'2026-11-25 13:45:27.000000','BC-DDIA-02','A1-05',NULL,'AVAILABLE','Good'),('2026-11-25',7,'2026-11-25 13:45:27.000000',13,'2026-11-25 13:45:27.000000','BC-AH-01','C1-01',NULL,'AVAILABLE','Good'),('2026-11-25',7,'2026-11-25 13:45:27.000000',14,'2026-11-25 13:45:27.000000','BC-AH-02','C1-01',NULL,'AVAILABLE','Good'),('2026-11-25',8,'2026-11-25 13:45:27.000000',15,'2026-11-25 13:45:27.000000','BC-AL-01','B1-01',NULL,'AVAILABLE','Good'),('2026-11-25',8,'2026-11-25 13:45:27.000000',16,'2026-11-25 13:45:27.000000','BC-AL-02','B1-01',NULL,'AVAILABLE','Good'),('2026-11-25',9,'2026-11-25 13:45:27.000000',17,'2026-11-25 13:45:27.000000','BC-1984-01','B1-02',NULL,'AVAILABLE','Good'),('2026-11-25',9,'2026-11-25 13:45:27.000000',18,'2026-11-25 13:45:27.000000','BC-1984-02','B1-02',NULL,'AVAILABLE','Good'),('2026-11-25',10,'2026-11-25 13:45:27.000000',19,'2026-11-25 13:45:27.000000','BC-SP-01','D1-01',NULL,'AVAILABLE','Good'),('2026-11-25',10,'2026-11-25 13:45:27.000000',20,'2026-11-25 13:45:27.000000','BC-SP-02','D1-01',NULL,'AVAILABLE','Good'),('2026-11-25',11,'2026-11-25 13:45:27.000000',21,'2026-11-25 13:45:27.000000','BC-TF-01','C2-01',NULL,'AVAILABLE','Good'),('2026-11-25',11,'2026-11-25 13:45:27.000000',22,'2026-11-25 13:45:27.000000','BC-TF-02','C2-01',NULL,'AVAILABLE','Good'),('2026-11-25',12,'2026-11-25 13:45:27.000000',23,'2026-11-25 13:45:27.000000','BC-DN-01','B3-01',NULL,'AVAILABLE','Good'),('2026-11-25',12,'2026-11-25 13:45:27.000000',24,'2026-11-25 13:45:27.000000','BC-DN-02','B3-01',NULL,'AVAILABLE','Good'),('2026-11-26',13,'2026-11-26 10:26:13.000000',25,'2026-11-26 10:26:13.000000','BC-DP-01','A1-06',NULL,'AVAILABLE','Good'),('2026-11-26',13,'2026-11-26 10:26:13.000000',26,'2026-11-26 10:26:13.000000','BC-DP-02','A1-06',NULL,'AVAILABLE','Good'),('2026-11-26',13,'2026-11-26 10:26:13.000000',27,'2026-11-26 10:26:13.000000','BC-DP-03','A1-06',NULL,'AVAILABLE','Good'),('2026-11-26',14,'2026-11-26 10:26:13.000000',28,'2026-11-26 10:26:13.000000','BC-DDD-01','A1-07',NULL,'AVAILABLE','Good'),('2026-11-26',14,'2026-11-26 10:26:13.000000',29,'2026-11-26 10:26:13.000000','BC-DDD-02','A1-07',NULL,'AVAILABLE','Good'),('2026-11-26',15,'2026-11-26 10:26:13.000000',30,'2026-11-26 10:26:13.000000','BC-HFDP-01','A1-08',NULL,'AVAILABLE','Good'),('2026-11-26',15,'2026-11-26 10:26:13.000000',31,'2026-11-26 10:26:13.000000','BC-HFDP-02','A1-08',NULL,'AVAILABLE','Good'),('2026-11-26',15,'2026-11-26 10:26:13.000000',32,'2026-11-26 10:26:13.000000','BC-HFDP-03','A1-08',NULL,'AVAILABLE','Good'),('2026-11-26',16,'2026-11-26 10:26:13.000000',33,'2026-11-26 10:26:13.000000','BC-CLRS-01','A1-09',NULL,'AVAILABLE','Good'),('2026-11-26',16,'2026-11-26 10:26:13.000000',34,'2026-11-26 10:26:13.000000','BC-CLRS-02','A1-09',NULL,'AVAILABLE','Good'),('2026-11-26',16,'2026-11-26 10:26:13.000000',35,'2026-11-26 10:26:13.000000','BC-CLRS-03','A1-09',NULL,'AVAILABLE','Good'),('2026-11-26',17,'2026-11-26 10:26:13.000000',36,'2026-11-26 10:26:13.000000','BC-HB-01','B3-02',NULL,'AVAILABLE','Good'),('2026-11-26',17,'2026-11-26 10:26:13.000000',37,'2026-11-26 10:26:13.000000','BC-HB-02','B3-02',NULL,'AVAILABLE','Good'),('2026-11-26',17,'2026-11-26 10:26:13.000000',38,'2026-11-26 10:26:13.000000','BC-HB-03','B3-02',NULL,'AVAILABLE','Good'),('2026-11-26',18,'2026-11-26 10:26:13.000000',39,'2026-11-26 10:26:13.000000','BC-HP1-01','B3-03',NULL,'AVAILABLE','Good'),('2026-11-26',18,'2026-11-26 10:26:13.000000',40,'2026-11-26 10:26:13.000000','BC-HP1-02','B3-03',NULL,'AVAILABLE','Good'),('2026-11-26',18,'2026-11-26 10:26:13.000000',41,'2026-11-26 10:26:13.000000','BC-HP1-03','B3-03',NULL,'AVAILABLE','Good'),('2026-11-26',19,'2026-11-26 10:26:13.000000',42,'2026-11-26 10:26:13.000000','BC-GG-01','B1-03',NULL,'AVAILABLE','Good'),('2026-11-26',19,'2026-11-26 10:26:13.000000',43,'2026-11-26 10:26:13.000000','BC-GG-02','B1-03',NULL,'AVAILABLE','Good'),('2026-11-26',20,'2026-11-26 10:26:14.000000',44,'2026-11-26 10:26:14.000000','BC-TKAM-01','B1-04',NULL,'AVAILABLE','Good'),('2026-11-26',20,'2026-11-26 10:26:14.000000',45,'2026-11-26 10:26:14.000000','BC-TKAM-02','B1-04',NULL,'AVAILABLE','Good'),('2026-11-26',21,'2026-11-26 10:26:14.000000',46,'2026-11-26 10:26:14.000000','BC-PAP-01','B1-05',NULL,'AVAILABLE','Good'),('2026-11-26',21,'2026-11-26 10:26:14.000000',47,'2026-11-26 10:26:14.000000','BC-PAP-02','B1-05',NULL,'AVAILABLE','Good'),('2026-11-26',22,'2026-11-26 10:26:14.000000',48,'2026-11-26 10:26:14.000000','BC-CAP-01','B1-06',NULL,'AVAILABLE','Good'),('2026-11-26',22,'2026-11-26 10:26:14.000000',49,'2026-11-26 10:26:14.000000','BC-CAP-02','B1-06',NULL,'AVAILABLE','Good'),('2026-11-26',23,'2026-11-26 10:26:14.000000',50,'2026-11-26 10:26:14.000000','BC-NW-01','B2-02',NULL,'AVAILABLE','Good'),('2026-11-26',23,'2026-11-26 10:26:14.000000',51,'2026-11-26 10:26:14.000000','BC-NW-02','B2-02',NULL,'AVAILABLE','Good'),('2026-11-26',24,'2026-11-26 10:26:14.000000',52,'2026-11-26 10:26:14.000000','BC-KOS-01','B2-03',NULL,'AVAILABLE','Good'),('2026-11-26',24,'2026-11-26 10:26:14.000000',53,'2026-11-26 10:26:14.000000','BC-KOS-02','B2-03',NULL,'AVAILABLE','Good'),('2026-11-26',25,'2026-11-26 10:26:14.000000',54,'2026-11-26 10:26:14.000000','BC-DW-01','C1-02',NULL,'AVAILABLE','Good'),('2026-11-26',25,'2026-11-26 10:26:14.000000',55,'2026-11-26 10:26:14.000000','BC-DW-02','C1-02',NULL,'AVAILABLE','Good'),('2026-11-26',25,'2026-11-26 10:26:14.000000',56,'2026-11-26 10:26:14.000000','BC-DW-03','C1-02',NULL,'AVAILABLE','Good'),('2026-11-26',26,'2026-11-26 10:26:14.000000',57,'2026-11-26 10:26:14.000000','BC-POM-01','C2-02',NULL,'AVAILABLE','Good'),('2026-11-26',26,'2026-11-26 10:26:14.000000',58,'2026-11-26 10:26:14.000000','BC-POM-02','C2-02',NULL,'AVAILABLE','Good'),('2026-11-26',26,'2026-11-26 10:26:14.000000',59,'2026-11-26 10:26:14.000000','BC-POM-03','C2-02',NULL,'AVAILABLE','Good'),('2026-11-26',27,'2026-11-26 10:26:14.000000',60,'2026-11-26 10:26:14.000000','BC-ZTO-01','C2-03',NULL,'AVAILABLE','Good'),('2026-11-26',27,'2026-11-26 10:26:14.000000',61,'2026-11-26 10:26:14.000000','BC-ZTO-02','C2-03',NULL,'AVAILABLE','Good'),('2026-11-26',28,'2026-11-26 10:26:14.000000',62,'2026-11-26 10:26:14.000000','BC-EDU-01','D1-02',NULL,'AVAILABLE','Good'),('2026-11-26',28,'2026-11-26 10:26:14.000000',63,'2026-11-26 10:26:14.000000','BC-EDU-02','D1-02',NULL,'AVAILABLE','Good'),('2026-11-26',29,'2026-11-26 10:26:14.000000',64,'2026-11-26 10:26:14.000000','BC-FND-01','B3-04',NULL,'AVAILABLE','Good'),('2026-11-26',29,'2026-11-26 10:26:14.000000',65,'2026-11-26 10:26:14.000000','BC-FND-02','B3-04',NULL,'AVAILABLE','Good'),('2026-11-26',30,'2026-11-26 10:26:14.000000',66,'2026-11-26 10:26:14.000000','BC-AOW-01','D2-01',NULL,'AVAILABLE','Good'),('2026-11-26',30,'2026-11-26 10:26:14.000000',67,'2026-11-26 10:26:14.000000','BC-AOW-02','D2-01',NULL,'AVAILABLE','Good'),('2026-11-26',31,'2026-11-26 10:26:14.000000',68,'2026-11-26 10:26:14.000000','BC-DM-01','B2-04',NULL,'AVAILABLE','Good'),('2026-11-26',31,'2026-11-26 10:26:14.000000',69,'2026-11-26 10:26:14.000000','BC-DM-02','B2-04',NULL,'AVAILABLE','Good'),('2026-11-26',31,'2026-11-26 10:26:14.000000',70,'2026-11-26 10:26:14.000000','BC-DM-03','B2-04',NULL,'AVAILABLE','Good'),('2026-11-26',32,'2026-11-26 10:26:14.000000',71,'2026-11-26 10:26:14.000000','BC-VT-01','B2-05',NULL,'AVAILABLE','Good'),('2026-11-26',32,'2026-11-26 10:26:14.000000',72,'2026-11-26 10:26:14.000000','BC-VT-02','B2-05',NULL,'AVAILABLE','Good'),('2026-11-26',32,'2026-11-26 10:26:14.000000',73,'2026-11-26 10:26:14.000000','BC-VT-03','B2-05',NULL,'AVAILABLE','Good');
/*!40000 ALTER TABLE `book_copies` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `books`
--

DROP TABLE IF EXISTS `books`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `books` (
  `is_deleted` bit(1) NOT NULL,
  `page_count` int DEFAULT NULL,
  `publish_year` smallint DEFAULT NULL,
  `publisher_id` int DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `isbn` varchar(20) DEFAULT NULL,
  `language` varchar(30) DEFAULT NULL,
  `book_code` varchar(50) DEFAULT NULL,
  `edition` varchar(50) DEFAULT NULL,
  `translator` varchar(150) DEFAULT NULL,
  `cover_url` varchar(255) DEFAULT NULL,
  `description` text,
  `keywords` varchar(255) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `publication_status` enum('DISCONTINUED','DRAFT','PUBLISHED') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKkibbepcitr0a3cpk3rfr7nihn` (`isbn`),
  UNIQUE KEY `UK8osspp2iqw7t0nj1k35s7j98h` (`book_code`),
  KEY `FKayy5edfrqnegqj3882nce6qo8` (`publisher_id`),
  CONSTRAINT `FKayy5edfrqnegqj3882nce6qo8` FOREIGN KEY (`publisher_id`) REFERENCES `publishers` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `books`
--

LOCK TABLES `books` WRITE;
/*!40000 ALTER TABLE `books` DISABLE KEYS */;
INSERT INTO `books` VALUES (0x00,416,2018,1,'2026-11-25 11:05:54.000000',1,'2026-11-25 13:47:39.000000','9780134685991','en','BK-001',NULL,NULL,'/covers/3b4996d6-a016-4d01-94dd-0a7bd5ea02d5.jpg','The definitive guide to best practices for the Java platform, updated for Java 7, 8, and 9.','java,programming,best practices,oop','Effective Java','PUBLISHED'),(0x00,464,2008,2,'2026-11-25 11:05:54.000000',2,'2026-11-25 13:47:39.000000','9780132350884','en','BK-002',NULL,NULL,'/covers/257c6cea-d074-4b8a-ad06-7503050e7fa8.jpg','A Handbook of Agile Software Craftsmanship teaching principles, patterns, and practices of writing clean code.','clean code,software,refactoring,agile','Clean Code','PUBLISHED'),(0x00,448,2018,1,'2026-11-25 11:05:54.000000',3,'2026-11-25 13:47:39.000000','9780134757599','en','BK-003',NULL,NULL,'/covers/a78c8bcf-0cb0-45e4-9631-c4042436f1b4.jpg','Improving the Design of Existing Code (2nd Edition) with JavaScript examples and practical refactoring catalog.','refactoring,design,architecture,software','Refactoring','PUBLISHED'),(0x00,300,2019,3,'2026-11-25 11:05:54.000000',4,'2026-11-25 13:47:39.000000','9786041098765','vi','BK-004',NULL,NULL,'/covers/d8ff90e8-2179-4778-b85a-468e1022c270.jpg','Câu chuyện tình thầm lặng và hoài niệm của Ngạn dành cho Hà Lan từ làng Đo Đo đến phố thị.','van hoc viet nam,tieu thuyet,nguyen nhat anh','Mắt Biếc','PUBLISHED'),(0x00,352,2019,1,'2026-11-25 13:45:27.000000',5,'2026-11-25 13:47:39.000000','9780135957059','en','BK-005',NULL,NULL,'/covers/66013492-c162-4784-9c51-7f40f6be1a4e.jpg','Your Journey to Mastery, 20th Anniversary Edition — practical wisdom for modern software developers.','pragmatic,software engineering,career,craftsmanship','The Pragmatic Programmer','PUBLISHED'),(0x00,616,2017,4,'2026-11-25 13:45:27.000000',6,'2026-11-25 13:47:39.000000','9781449373320','en','BK-006',NULL,NULL,'/covers/4314f9bd-d561-4972-a619-b10f78e5b245.jpg','The Big Ideas Behind Reliable, Scalable, and Maintainable Systems — databases, replication, partitioning, and stream processing.','distributed systems,databases,scalability,backend','Designing Data-Intensive Applications','PUBLISHED'),(0x00,320,2018,5,'2026-11-25 13:45:27.000000',7,'2026-11-25 13:47:39.000000','9780735211292','en','BK-007',NULL,NULL,'/covers/b03e7524-8fb8-479d-b9fb-b5f5d17c4baf.jpg','An Easy & Proven Way to Build Good Habits & Break Bad Ones through tiny daily changes and identity-based systems.','habits,productivity,self-improvement,psychology','Atomic Habits','PUBLISHED'),(0x00,208,2014,6,'2026-11-25 13:45:27.000000',8,'2026-11-25 13:47:39.000000','9780062315007','en','BK-008',NULL,NULL,'/covers/9f37ec20-9333-4895-ac38-1541b2245f12.jpg','A magical fable about following your Personal Legend across the deserts of Egypt.','fiction,classic,journey,philosophy','The Alchemist','PUBLISHED'),(0x00,328,1961,7,'2026-11-25 13:45:27.000000',9,'2026-11-25 13:47:39.000000','9780451524935','en','BK-009',NULL,NULL,'/covers/564fb1c9-2e12-4d75-ac06-2ea7664f681e.jpg','A dystopian social science fiction novel and cautionary tale about totalitarianism and surveillance.','dystopian,classic,literature,politics','1984','PUBLISHED'),(0x00,464,2015,8,'2026-11-25 13:45:27.000000',10,'2026-11-25 13:47:39.000000','9780062316097','en','BK-010',NULL,NULL,'/covers/be5f2ea2-4114-4902-b557-ee876631319a.jpg','A sweeping narrative of human history from the Cognitive Revolution to the 21st century.','history,anthropology,science,humanity','Sapiens: A Brief History of Humankind','PUBLISHED'),(0x00,499,2013,9,'2026-11-25 13:45:27.000000',11,'2026-11-25 13:47:39.000000','9780374533557','en','BK-011',NULL,NULL,'/covers/8ff1a21b-2244-413c-8a47-f4aedd2a97ce.jpg','Explores the two systems that drive the way we think: fast intuitive thinking and slow deliberate reasoning.','psychology,behavioral economics,decision making','Thinking, Fast and Slow','PUBLISHED'),(0x00,896,1990,10,'2026-11-25 13:45:27.000000',12,'2026-11-25 13:47:39.000000','9780441172719','en','BK-012',NULL,NULL,'/covers/efabc3b3-f3cd-4646-be63-bcb790f058ed.jpg','Set on the desert planet Arrakis, Dune tells the story of Paul Atreides amidst galactic politics and ecology.','sci-fi,classic,epic,arrakis','Dune','PUBLISHED'),(0x00,395,1994,1,'2026-11-26 10:26:13.000000',13,'2026-11-26 10:28:56.000000','9780201633610','en','BK-013',NULL,NULL,'/covers/5682dfb5-cb15-4968-8d31-8a0b388f88d8.jpg','Captures 23 classic object-oriented design patterns for creating flexible, reusable software.','design patterns,oop,architecture,gof','Design Patterns: Elements of Reusable Object-Oriented Software','PUBLISHED'),(0x00,560,2003,1,'2026-11-26 10:26:13.000000',14,'2026-11-26 10:29:03.000000','9780321125215','en','BK-014',NULL,NULL,'/covers/0bf3d3fe-3166-46fe-99f0-49777b7d123f.jpg','Tackling complexity in the heart of software by aligning system models with business domain language.','ddd,architecture,software modeling','Domain-Driven Design','PUBLISHED'),(0x00,676,2004,4,'2026-11-26 10:26:13.000000',15,'2026-11-26 10:29:07.000000','9780596007126','en','BK-015',NULL,NULL,'/covers/dbf3fa14-f693-48a3-bcb6-a237267262b2.jpg','A visually rich guide to applying object-oriented design patterns in Java and modern applications.','design patterns,java,oop,head first','Head First Design Patterns','PUBLISHED'),(0x00,1292,2009,11,'2026-11-26 10:26:13.000000',16,'2026-11-26 10:29:10.000000','9780262033848','en','BK-016',NULL,NULL,'/covers/081cfc96-3a27-4373-bb96-c12e4c618ecb.jpg','Comprehensive textbook covering data structures, graph algorithms, dynamic programming, and complexity.','algorithms,data structures,computer science,clrs','Introduction to Algorithms','PUBLISHED'),(0x00,300,2012,12,'2026-11-26 10:26:13.000000',17,'2026-11-26 10:29:12.000000','9780547928227','en','BK-017',NULL,NULL,'/covers/44c53909-ac78-4910-acca-5598f06dda8a.jpg','Bilbo Baggins embarks on an unexpected journey with Thorin Oakenshield to reclaim the Lonely Mountain.','fantasy,middle-earth,classic,adventure','The Hobbit','PUBLISHED'),(0x00,223,1997,13,'2026-11-26 10:26:13.000000',18,'2026-11-26 10:29:15.000000','9780747532699','en','BK-018',NULL,NULL,'/covers/487f0b50-6610-4c70-97a9-619403780f5a.jpg','Young Harry discovers his magical heritage and enters Hogwarts School of Witchcraft and Wizardry.','fantasy,magic,hogwarts,fiction','Harry Potter and the Philosopher\'s Stone','PUBLISHED'),(0x00,180,2004,14,'2026-11-26 10:26:13.000000',19,'2026-11-26 10:29:17.000000','9780743273565','en','BK-019',NULL,NULL,'/covers/27d530d0-3524-485f-9b97-0c3169f7e47e.jpg','Nick Carraway recounts the tragic pursuit of the American Dream by millionaire Jay Gatsby.','classic,jazz age,american literature','The Great Gatsby','PUBLISHED'),(0x00,324,2006,15,'2026-11-26 10:26:14.000000',20,'2026-11-26 10:29:20.000000','9780061120084','en','BK-020',NULL,NULL,'/covers/7802c80e-66f9-43c5-bb43-185c400ae597.jpg','Through young Scout Finch\'s eyes, a story of justice, courage, and empathy in the American South.','classic,justice,pulitzer,fiction','To Kill a Mockingbird','PUBLISHED'),(0x00,435,2002,16,'2026-11-26 10:26:14.000000',21,'2026-11-26 10:29:23.000000','9780141439518','en','BK-021',NULL,NULL,'/covers/36cd7efd-2f64-46d5-a11a-c28f9d6122a4.jpg','Elizabeth Bennet navigates manners, society, and love with the proud Mr. Darcy.','classic,romance,english literature','Pride and Prejudice','PUBLISHED'),(0x00,656,2003,16,'2026-11-26 10:26:14.000000',22,'2026-11-26 10:29:26.000000','9780143058144','en','BK-022',NULL,NULL,'/covers/2e10fbb9-db73-4bc5-ae9b-eaaa659bc56c.jpg','Raskolnikov struggles with guilt, morality, and redemption in 19th-century Saint Petersburg.','russian literature,classic,philosophy','Crime and Punishment','PUBLISHED'),(0x00,296,2000,17,'2026-11-26 10:26:14.000000',23,'2026-11-26 10:29:29.000000','9780375704024','en','BK-023',NULL,NULL,'/covers/38db084d-cd51-47e3-b702-0dafef31004f.jpg','Toru Watanabe looks back on his student days in 1960s Tokyo amidst love and loss.','japanese literature,fiction,nostalgia','Norwegian Wood','PUBLISHED'),(0x00,467,2006,17,'2026-11-26 10:26:14.000000',24,'2026-11-26 10:29:31.000000','9781400079278','en','BK-024',NULL,NULL,'/covers/9c654502-271a-4037-ba7a-5f034c9468fe.jpg','Two parallel stories intertwine young runaway Kafka Tamura and elderly Nakata across Japan.','magical realism,japanese literature,fiction','Kafka on the Shore','PUBLISHED'),(0x00,296,2016,18,'2026-11-26 10:26:14.000000',25,'2026-11-26 10:29:34.000000','9781455586691','en','BK-025',NULL,NULL,'/covers/eba13aaa-a233-4755-b34c-679d817360cc.jpg','Rules for focused success in a distracted world through deliberate, distraction-free concentration.','productivity,focus,career,study','Deep Work','PUBLISHED'),(0x00,252,2020,19,'2026-11-26 10:26:14.000000',26,'2026-11-26 10:29:35.000000','9780857197689','en','BK-026',NULL,NULL,'/covers/8baae7bc-ecff-4dcb-b9ee-e12107487092.jpg','Timeless lessons on wealth, greed, and happiness showing how behavior drives financial outcomes.','finance,money,behavior,economics','The Psychology of Money','PUBLISHED'),(0x00,224,2014,20,'2026-11-26 10:26:14.000000',27,'2026-11-26 10:29:38.000000','9780804139298','en','BK-027',NULL,NULL,'/covers/7d23e40c-1f04-4e78-b7fd-7562d732c3c2.jpg','Notes on startups and how to build innovative companies that create new value from scratch.','startups,innovation,business,technology','Zero to One','PUBLISHED'),(0x00,334,2018,21,'2026-11-26 10:26:14.000000',28,'2026-11-26 10:29:41.000000','9780399590504','en','BK-028',NULL,NULL,'/covers/6c95d900-5889-4c7e-9c81-0bcf24e770c7.jpg','A memoir of a woman who leaves her survivalist upbringing to earn a PhD from Cambridge University.','memoir,education,biography,resilience','Educated','PUBLISHED'),(0x00,244,1991,22,'2026-11-26 10:26:14.000000',29,'2026-11-26 10:29:43.000000','9780553293357','en','BK-029',NULL,NULL,'/covers/3a823486-9430-4d18-b6a0-d693025c20a6.jpg','Mathematician Hari Seldon invents psychohistory to preserve human knowledge across a falling empire.','sci-fi,classic,space opera,asimov','Foundation','PUBLISHED'),(0x00,273,2005,23,'2026-11-26 10:26:14.000000',30,'2026-11-26 10:29:46.000000','9781590302255','en','BK-030',NULL,NULL,'/covers/33d7efdd-78a3-4adf-9143-fc87705021ba.jpg','Ancient treatise on strategy, leadership, planning, and adaptability.','strategy,philosophy,classic,leadership','The Art of War','PUBLISHED'),(0x00,160,2018,24,'2026-11-26 10:26:14.000000',31,'2026-11-26 10:29:47.000000','9786042100014','vi','BK-031',NULL,NULL,'/covers/dc5c06d0-ce1c-48a1-9c14-62b387a82ab5.jpg','Cuộc phiêu lưu kỳ thú của chú Dế Mèn qua nhiều vùng đất và bài học trưởng thành.','thieu nhi,van hoc viet nam,to hoai','Dế Mèn Phiêu Lưu Ký','PUBLISHED'),(0x00,208,2018,3,'2026-11-26 10:26:14.000000',32,'2026-11-26 10:29:47.000000','9786041054325','vi','BK-032',NULL,NULL,'/covers/71bd73a4-3ae8-403c-b196-8c9b9e8bb196.jpg','Những hồi ức trong trẻo và hài hước về thế giới trẻ thơ của bốn đứa trẻ Mùi, Hải Cò, Tí Sún, Tười.','tuoi tho,van hoc viet nam,nguyen nhat anh','Cho Tôi Xin Một Vé Đi Tuổi Thơ','PUBLISHED');
/*!40000 ALTER TABLE `books` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `borrow_request_items`
--

DROP TABLE IF EXISTS `borrow_request_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `borrow_request_items` (
  `assigned_copy_id` bigint DEFAULT NULL,
  `book_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `request_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKjoob6va9uwmo8jts3ui2nyl6k` (`assigned_copy_id`),
  KEY `FK8jrchex5i6xk86cveml6vtht2` (`book_id`),
  KEY `FK2ki6obrn9xbnqyopyxnq6ia09` (`request_id`),
  CONSTRAINT `FK2ki6obrn9xbnqyopyxnq6ia09` FOREIGN KEY (`request_id`) REFERENCES `borrow_requests` (`id`),
  CONSTRAINT `FK8jrchex5i6xk86cveml6vtht2` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`),
  CONSTRAINT `FKjoob6va9uwmo8jts3ui2nyl6k` FOREIGN KEY (`assigned_copy_id`) REFERENCES `book_copies` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `borrow_request_items`
--

LOCK TABLES `borrow_request_items` WRITE;
/*!40000 ALTER TABLE `borrow_request_items` DISABLE KEYS */;
INSERT INTO `borrow_request_items` VALUES (1,1,'2026-11-25 11:05:54.000000',1,1,'2026-11-25 06:24:28.763259'),(3,2,'2026-11-25 11:05:54.000000',2,1,'2026-11-25 06:24:28.763259'),(2,1,'2026-11-25 11:05:54.000000',3,2,'2026-11-25 06:25:37.126153'),(5,3,'2026-11-25 11:05:54.000000',4,2,'2026-11-25 06:25:37.126153'),(7,4,'2026-11-25 11:05:54.000000',5,2,'2026-11-25 06:25:37.126153'),(NULL,12,'2026-11-25 07:03:56.626040',6,3,'2026-11-25 07:03:56.626040');
/*!40000 ALTER TABLE `borrow_request_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `borrow_requests`
--

DROP TABLE IF EXISTS `borrow_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `borrow_requests` (
  `approved_at` datetime(6) DEFAULT NULL,
  `approved_by` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `request_date` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `request_code` varchar(50) DEFAULT NULL,
  `rejection_reason` varchar(500) DEFAULT NULL,
  `notes` varchar(1000) DEFAULT NULL,
  `status` enum('APPROVED','CANCELLED','COMPLETED','PENDING','REJECTED') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_br_request_code` (`request_code`),
  KEY `idx_br_member_status` (`member_id`,`status`),
  KEY `FKeeogpg7ld005b23pmmqyl13lw` (`approved_by`),
  CONSTRAINT `FKeeogpg7ld005b23pmmqyl13lw` FOREIGN KEY (`approved_by`) REFERENCES `users` (`id`),
  CONSTRAINT `FKr122xuo8uvat9swrgoskv601j` FOREIGN KEY (`member_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `borrow_requests`
--

LOCK TABLES `borrow_requests` WRITE;
/*!40000 ALTER TABLE `borrow_requests` DISABLE KEYS */;
INSERT INTO `borrow_requests` VALUES ('2026-11-25 11:05:54.000000',2,'2026-11-25 11:05:54.000000',1,3,'2026-11-25 11:05:54.000000','2026-11-25 06:24:28.758263','BR-MULTI-2026-01',NULL,'Sample 2-book request for multi-barcode handover testing','COMPLETED'),('2026-11-25 11:05:54.000000',2,'2026-11-25 11:05:54.000000',2,3,'2026-11-25 11:05:54.000000','2026-11-25 06:25:37.125166','BR-MULTI-2026-02',NULL,'Sample 3-book request for multi-barcode handover testing','COMPLETED'),(NULL,NULL,'2026-11-25 07:03:56.568091',3,3,'2026-11-25 07:03:56.580813','2026-11-25 07:03:56.568091','BR-1795590236559',NULL,'Borrow request submitted by member','PENDING');
/*!40000 ALTER TABLE `borrow_requests` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `categories`
--

DROP TABLE IF EXISTS `categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categories` (
  `id` int NOT NULL AUTO_INCREMENT,
  `parent_id` int DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKt8o6pivur7nn124jehx7cygw5` (`name`),
  KEY `FKsaok720gsu4u2wrgbk10b5n8d` (`parent_id`),
  CONSTRAINT `FKsaok720gsu4u2wrgbk10b5n8d` FOREIGN KEY (`parent_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `categories`
--

LOCK TABLES `categories` WRITE;
/*!40000 ALTER TABLE `categories` DISABLE KEYS */;
INSERT INTO `categories` VALUES (1,NULL,'2026-11-25 11:05:54.000000','2026-11-25 11:05:54.000000','Software Engineering'),(2,NULL,'2026-11-25 11:05:54.000000','2026-11-25 11:05:54.000000','Literature'),(3,NULL,'2026-11-25 13:45:27.000000','2026-11-25 13:45:27.000000','Life Skills'),(4,NULL,'2026-11-25 13:45:27.000000','2026-11-25 13:45:27.000000','History'),(5,NULL,'2026-11-25 13:45:27.000000','2026-11-25 13:45:27.000000','Psychology'),(6,NULL,'2026-11-25 13:45:27.000000','2026-11-25 13:45:27.000000','Science Fiction'),(7,NULL,'2026-11-26 10:26:13.000000','2026-11-26 10:26:13.000000','Fantasy'),(8,NULL,'2026-11-26 10:26:13.000000','2026-11-26 10:26:13.000000','Classic Fiction'),(9,NULL,'2026-11-26 10:26:14.000000','2026-11-26 10:26:14.000000','Business & Economics'),(10,NULL,'2026-11-26 10:26:14.000000','2026-11-26 10:26:14.000000','Biography'),(11,NULL,'2026-11-26 10:26:14.000000','2026-11-26 10:26:14.000000','Philosophy');
/*!40000 ALTER TABLE `categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `circulation_policies`
--

DROP TABLE IF EXISTS `circulation_policies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `circulation_policies` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `daily_fine_rate` decimal(10,2) NOT NULL,
  `loan_period_days` int NOT NULL,
  `max_loans_per_member` int NOT NULL,
  `renew_period_days` int NOT NULL,
  `reservation_hold_days` int NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by` varchar(150) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `circulation_policies`
--

LOCK TABLES `circulation_policies` WRITE;
/*!40000 ALTER TABLE `circulation_policies` DISABLE KEYS */;
INSERT INTO `circulation_policies` VALUES (1,5000.00,14,5,7,2,'2026-11-25 02:13:22.758974','system');
/*!40000 ALTER TABLE `circulation_policies` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `email_logs`
--

DROP TABLE IF EXISTS `email_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `email_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `body` text NOT NULL,
  `error_message` varchar(500) DEFAULT NULL,
  `idempotency_key` varchar(120) DEFAULT NULL,
  `last_attempt_at` datetime(6) NOT NULL,
  `recipient_email` varchar(160) NOT NULL,
  `recipient_name` varchar(120) NOT NULL,
  `recipient_user_id` bigint DEFAULT NULL,
  `reference_id` bigint DEFAULT NULL,
  `reference_type` varchar(30) DEFAULT NULL,
  `retry_count` int NOT NULL,
  `sent_at` datetime(6) DEFAULT NULL,
  `sent_by_name` varchar(120) DEFAULT NULL,
  `sent_by_user_id` bigint DEFAULT NULL,
  `status` enum('FAILED','SENT') NOT NULL,
  `subject` varchar(250) NOT NULL,
  `template_name` varchar(100) NOT NULL,
  `template_type` enum('DUE_SOON','OVERDUE','PICKUP_READY') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_email_log_created` (`created_at`),
  KEY `idx_email_log_idempotency` (`idempotency_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `email_logs`
--

LOCK TABLES `email_logs` WRITE;
/*!40000 ALTER TABLE `email_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `email_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `email_templates`
--

DROP TABLE IF EXISTS `email_templates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `email_templates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `active` bit(1) NOT NULL,
  `body_template` text NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `subject_template` varchar(250) NOT NULL,
  `type` enum('DUE_SOON','OVERDUE','PICKUP_READY') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKcj66pbqmgymp4ocky084ekv1` (`type`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `email_templates`
--

LOCK TABLES `email_templates` WRITE;
/*!40000 ALTER TABLE `email_templates` DISABLE KEYS */;
INSERT INTO `email_templates` VALUES (1,'2026-11-19 06:55:12.346804','2026-11-19 06:55:12.346804',0x01,'Dear {{memberName}},\n\nYour borrowed copy of \"{{bookTitle}}\" is due back on {{dueDate}}.\nPlease return the book to the Circulation Desk on time or request a renewal from My books if no other reader is waiting.\n\nMember card: {{memberCode}}\nWarm regards,\nbooknet Library','Reminder sent 3 days before a borrowed book\'s due date','Due soon','[booknet] Reminder: {{bookTitle}} is due on {{dueDate}}','DUE_SOON'),(2,'2026-11-19 06:55:12.382804','2026-11-19 06:55:12.382804',0x01,'Dear {{memberName}},\n\nYour borrowed book \"{{bookTitle}}\" was due on {{dueDate}} and is currently {{daysOverdue}} days overdue.\nPlease return it to the Circulation Desk at your earliest convenience so another waiting reader can borrow it and to prevent further late fees.\n\nMember card: {{memberCode}}\nWarm regards,\nbooknet Library','Notice for loans past their due date with daily late fee guidance','Overdue books','[booknet] Overdue notice: {{bookTitle}} is {{daysOverdue}} days overdue','OVERDUE'),(3,'2026-11-19 06:55:12.387789','2026-11-19 06:55:12.387789',0x01,'Dear {{memberName}},\n\nGood news! A copy of \"{{bookTitle}}\" ({{copyBarcode}}) is now held for reservation {{reservationCode}}.\nPlease bring your library card ({{memberCode}}) to the Circulation Desk before {{pickupUntil}}. Holds expire automatically at the end of the pickup window.\n\nWarm regards,\nbooknet Library','Hold alert confirming shelf pickup window before expiry','Reservation ready','[booknet] Your reserved book \"{{bookTitle}}\" is ready for pickup','PICKUP_READY');
/*!40000 ALTER TABLE `email_templates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `favorite_books`
--

DROP TABLE IF EXISTS `favorite_books`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `favorite_books` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `saved_at` datetime(6) NOT NULL,
  `book_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_book_favorite` (`user_id`,`book_id`),
  KEY `idx_favorite_user` (`user_id`),
  KEY `idx_favorite_book` (`book_id`),
  CONSTRAINT `FK6pkfn8g4ittyogd3tg0p8i20` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKej0uxtwc4vsr2rbeqi65o3og8` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `favorite_books`
--

LOCK TABLES `favorite_books` WRITE;
/*!40000 ALTER TABLE `favorite_books` DISABLE KEYS */;
/*!40000 ALTER TABLE `favorite_books` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fines`
--

DROP TABLE IF EXISTS `fines`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fines` (
  `amount` decimal(10,2) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `loan_id` bigint NOT NULL,
  `member_id` bigint NOT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `reason` enum('DAMAGED','LOST','OVERDUE') NOT NULL,
  `status` enum('ESTIMATED','PAID','UNPAID','WAIVED') NOT NULL,
  `adjusted_at` datetime(6) DEFAULT NULL,
  `adjustment_note` text,
  `confirmed_by_staff_at` datetime(6) DEFAULT NULL,
  `daily_rate` decimal(10,2) DEFAULT NULL,
  `finalized_at` datetime(6) DEFAULT NULL,
  `fine_code` varchar(30) DEFAULT NULL,
  `overdue_days` int DEFAULT NULL,
  `payment_method` varchar(20) DEFAULT NULL,
  `transaction_ref` varchar(100) DEFAULT NULL,
  `adjusted_by` bigint DEFAULT NULL,
  `confirmed_by_staff` bigint DEFAULT NULL,
  `finalized_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKplui3570krodgfomclmoj60c6` (`fine_code`),
  KEY `FKpxdidln7gte3bgknqqhknl99h` (`loan_id`),
  KEY `FKnrcqjknx2tp2q2gtuokpdgbqn` (`member_id`),
  KEY `FK5aq1ijesq2g33pj3oy04b9e2x` (`adjusted_by`),
  KEY `FKro3tu5ju3grbk39va4otj7mr` (`confirmed_by_staff`),
  KEY `FK1eksss0017qro72kmaj3u5l9n` (`finalized_by`),
  CONSTRAINT `FK1eksss0017qro72kmaj3u5l9n` FOREIGN KEY (`finalized_by`) REFERENCES `users` (`id`),
  CONSTRAINT `FK5aq1ijesq2g33pj3oy04b9e2x` FOREIGN KEY (`adjusted_by`) REFERENCES `users` (`id`),
  CONSTRAINT `FKnrcqjknx2tp2q2gtuokpdgbqn` FOREIGN KEY (`member_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKpxdidln7gte3bgknqqhknl99h` FOREIGN KEY (`loan_id`) REFERENCES `loans` (`id`),
  CONSTRAINT `FKro3tu5ju3grbk39va4otj7mr` FOREIGN KEY (`confirmed_by_staff`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fines`
--

LOCK TABLES `fines` WRITE;
/*!40000 ALTER TABLE `fines` DISABLE KEYS */;
/*!40000 ALTER TABLE `fines` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `loan_items`
--

DROP TABLE IF EXISTS `loan_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `loan_items` (
  `due_date` date NOT NULL,
  `fine_amount` decimal(12,2) DEFAULT NULL,
  `renew_count` int NOT NULL,
  `copy_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `last_renewed_at` datetime(6) DEFAULT NULL,
  `loan_id` bigint NOT NULL,
  `returned_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `damage_notes` varchar(500) DEFAULT NULL,
  `return_condition` enum('DAMAGED','GOOD','LOST') DEFAULT NULL,
  `status` enum('BORROWED','BORROWING','DAMAGED','LOST','RETURNED','RETURN_REQUESTED') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_loan_items_status` (`status`),
  KEY `idx_loan_items_copy` (`copy_id`),
  KEY `FKb1hb4c28uds2onfikt2gi5lbr` (`loan_id`),
  CONSTRAINT `FKb1hb4c28uds2onfikt2gi5lbr` FOREIGN KEY (`loan_id`) REFERENCES `loans` (`id`),
  CONSTRAINT `FKhys79rgv69qi6oqge59yhtv8r` FOREIGN KEY (`copy_id`) REFERENCES `book_copies` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `loan_items`
--

LOCK TABLES `loan_items` WRITE;
/*!40000 ALTER TABLE `loan_items` DISABLE KEYS */;
INSERT INTO `loan_items` VALUES ('2026-12-09',NULL,0,1,'2026-11-25 06:24:28.744259',1,NULL,1,NULL,'2026-11-25 06:24:28.744259',NULL,NULL,'BORROWING'),('2026-12-09',NULL,0,3,'2026-11-25 06:24:28.753245',2,NULL,1,NULL,'2026-11-25 06:24:28.753245',NULL,NULL,'BORROWING'),('2026-12-09',NULL,0,2,'2026-11-25 06:25:37.115167',3,NULL,2,NULL,'2026-11-25 06:25:37.115167',NULL,NULL,'BORROWING'),('2026-12-09',NULL,0,5,'2026-11-25 06:25:37.117166',4,NULL,2,NULL,'2026-11-25 06:25:37.117166',NULL,NULL,'BORROWING'),('2026-12-09',NULL,0,7,'2026-11-25 06:25:37.122166',5,NULL,2,NULL,'2026-11-25 06:25:37.122166',NULL,NULL,'BORROWING');
/*!40000 ALTER TABLE `loan_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `loans`
--

DROP TABLE IF EXISTS `loans`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `loans` (
  `due_date` date NOT NULL,
  `renew_count` int NOT NULL,
  `borrowed_at` datetime(6) DEFAULT NULL,
  `copy_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `issued_by` bigint DEFAULT NULL,
  `member_id` bigint NOT NULL,
  `received_by` bigint DEFAULT NULL,
  `renew_requested_at` datetime(6) DEFAULT NULL,
  `return_requested_at` datetime(6) DEFAULT NULL,
  `returned_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `note` varchar(255) DEFAULT NULL,
  `status` enum('BORROWING','LOST','OVERDUE','RENEW_PENDING','RETURNED','RETURN_PENDING') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_loans_member_status` (`member_id`,`status`),
  KEY `idx_loans_due` (`status`,`due_date`),
  KEY `FK7j7etdq4mfcn4ef7q4islnpny` (`copy_id`),
  KEY `FKlgjjil0qmrhbcefd160e6rmyk` (`issued_by`),
  KEY `FKjwg3miof8qblg97acp0c0hlb6` (`received_by`),
  CONSTRAINT `FK7j7etdq4mfcn4ef7q4islnpny` FOREIGN KEY (`copy_id`) REFERENCES `book_copies` (`id`),
  CONSTRAINT `FKjwg3miof8qblg97acp0c0hlb6` FOREIGN KEY (`received_by`) REFERENCES `users` (`id`),
  CONSTRAINT `FKlamxb97uftahy1qaxpnv14or7` FOREIGN KEY (`member_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKlgjjil0qmrhbcefd160e6rmyk` FOREIGN KEY (`issued_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `loans`
--

LOCK TABLES `loans` WRITE;
/*!40000 ALTER TABLE `loans` DISABLE KEYS */;
INSERT INTO `loans` VALUES ('2026-12-09',0,'2026-11-25 06:24:28.689260',1,'2026-11-25 06:24:28.697259',1,2,3,NULL,NULL,NULL,NULL,'2026-11-25 06:24:28.697259','','BORROWING'),('2026-12-09',0,'2026-11-25 06:25:37.111167',2,'2026-11-25 06:25:37.112151',2,2,3,NULL,NULL,NULL,NULL,'2026-11-25 06:25:37.112151','','BORROWING');
/*!40000 ALTER TABLE `loans` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `member_cards`
--

DROP TABLE IF EXISTS `member_cards`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_cards` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `activated_by` bigint DEFAULT NULL,
  `card_number` varchar(24) NOT NULL,
  `expires_on` date DEFAULT NULL,
  `issued_on` date DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKgv5od9kilokds6yl97s65yih9` (`card_number`),
  UNIQUE KEY `UKcclsn0arascpuxkr6579pswug` (`user_id`),
  CONSTRAINT `FK9i6dv18ag55s0rla9op2k42kh` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `member_cards`
--

LOCK TABLES `member_cards` WRITE;
/*!40000 ALTER TABLE `member_cards` DISABLE KEYS */;
INSERT INTO `member_cards` VALUES (1,'2026-11-20 01:55:38.660007','2026-11-20 01:55:38.660007',NULL,'BN-145F239465E34D7AA95D',NULL,NULL,4),(2,'2026-11-25 02:13:44.402337','2026-11-25 11:05:54.000000',2,'BN-EA771D44D8DD43128B47','2027-11-25','2026-11-25',3),(3,'2026-11-26 10:26:14.000000','2026-11-26 10:26:14.000000',1,'CARD-0005','2027-11-26','2026-10-27',5),(4,'2026-11-26 10:26:14.000000','2026-11-26 10:26:14.000000',1,'CARD-0006','2027-11-26','2026-10-27',6),(5,'2026-11-26 10:26:14.000000','2026-11-26 10:26:14.000000',1,'CARD-0007','2027-11-26','2026-10-27',7),(6,'2026-11-26 10:26:14.000000','2026-11-26 10:26:14.000000',1,'CARD-0008','2027-11-26','2026-10-27',8),(7,'2026-11-26 10:26:14.000000','2026-11-26 10:26:14.000000',1,'CARD-0009','2027-11-26','2026-10-27',9),(8,'2026-11-26 10:26:14.000000','2026-11-26 10:26:14.000000',1,'CARD-0010','2027-11-26','2026-10-27',10),(9,'2026-11-26 10:26:14.000000','2026-11-26 10:26:14.000000',1,'CARD-0011','2027-11-26','2026-10-27',11),(10,'2026-11-26 10:26:14.000000','2026-11-26 10:26:14.000000',1,'CARD-0012','2027-11-26','2026-10-27',12),(11,'2026-11-26 10:26:14.000000','2026-11-26 10:26:14.000000',1,'CARD-0013','2026-11-11','2025-11-11',13);
/*!40000 ALTER TABLE `member_cards` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notification_preferences`
--

DROP TABLE IF EXISTS `notification_preferences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_preferences` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `email_enabled` bit(1) NOT NULL,
  `events_newsletter_enabled` bit(1) NOT NULL,
  `website_enabled` bit(1) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKn2jopkbm16qv3xelbvoyjkd0g` (`user_id`),
  CONSTRAINT `FKt9qjvmcl36i14utm5uptyqg84` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification_preferences`
--

LOCK TABLES `notification_preferences` WRITE;
/*!40000 ALTER TABLE `notification_preferences` DISABLE KEYS */;
/*!40000 ALTER TABLE `notification_preferences` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `email_sent` bit(1) NOT NULL,
  `is_read` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `loan_id` bigint DEFAULT NULL,
  `reservation_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `title` varchar(150) NOT NULL,
  `message` varchar(500) NOT NULL,
  `type` enum('DUE_SOON','FINE','OVERDUE','RESERVATION_EXPIRED','RESERVATION_READY','SYSTEM') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_notif_user_read` (`user_id`,`is_read`,`created_at`),
  KEY `FK68sg7pbqg3kemjo5hv2kvtg5c` (`loan_id`),
  KEY `FKeua0pytgljr86idnpkhuir1mu` (`reservation_id`),
  CONSTRAINT `FK68sg7pbqg3kemjo5hv2kvtg5c` FOREIGN KEY (`loan_id`) REFERENCES `loans` (`id`),
  CONSTRAINT `FK9y21adhxn0ayjhfocscqox7bh` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKeua0pytgljr86idnpkhuir1mu` FOREIGN KEY (`reservation_id`) REFERENCES `reservations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notifications`
--

LOCK TABLES `notifications` WRITE;
/*!40000 ALTER TABLE `notifications` DISABLE KEYS */;
/*!40000 ALTER TABLE `notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oauth_identities`
--

DROP TABLE IF EXISTS `oauth_identities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oauth_identities` (
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `email` varchar(120) DEFAULT NULL,
  `provider_subject` varchar(200) NOT NULL,
  `provider_user_id` varchar(255) NOT NULL,
  `provider` enum('GOOGLE','LOCAL') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKs0u4mdifyssyt2a8mua6t653x` (`provider`,`provider_user_id`),
  UNIQUE KEY `UKdet1xkpou6bt7wvu2ihvyr9cg` (`user_id`,`provider`),
  CONSTRAINT `FKcwhpmr8ej1s107ds2ex7afd65` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oauth_identities`
--

LOCK TABLES `oauth_identities` WRITE;
/*!40000 ALTER TABLE `oauth_identities` DISABLE KEYS */;
INSERT INTO `oauth_identities` VALUES ('2026-11-20 01:55:38.609816',1,'2026-11-20 01:55:38.609816',4,'hoc07062006@gmail.com','116551716081290478761','116551716081290478761','GOOGLE');
/*!40000 ALTER TABLE `oauth_identities` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `page_visit_logs`
--

DROP TABLE IF EXISTS `page_visit_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `page_visit_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `http_method` varchar(10) DEFAULT NULL,
  `ip_address` varchar(60) DEFAULT NULL,
  `location` varchar(150) DEFAULT NULL,
  `page_name` varchar(150) DEFAULT NULL,
  `page_path` varchar(500) NOT NULL,
  `session_id` varchar(100) DEFAULT NULL,
  `user_email` varchar(150) NOT NULL,
  `user_role` varchar(30) DEFAULT NULL,
  `visited_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_visit_at` (`visited_at`),
  KEY `idx_visit_email` (`user_email`),
  KEY `idx_visit_path` (`page_path`)
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `page_visit_logs`
--

LOCK TABLES `page_visit_logs` WRITE;
/*!40000 ALTER TABLE `page_visit_logs` DISABLE KEYS */;
INSERT INTO `page_visit_logs` VALUES (1,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Home Page','/',NULL,'Anonymous (Guest)','GUEST','2026-11-26 03:20:44.443853'),(2,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Login Page','/login','5F10A80823E44804DC92EDDEACCBA0DC','Anonymous (Guest)','GUEST','2026-11-26 03:20:45.674634'),(3,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Login Page','/login','5F10A80823E44804DC92EDDEACCBA0DC','Anonymous (Guest)','GUEST','2026-11-26 03:20:49.885861'),(4,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Login Page','/login','5F10A80823E44804DC92EDDEACCBA0DC','Anonymous (Guest)','GUEST','2026-11-26 03:20:50.140929'),(5,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Member Directory','/staff/members','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:20:56.282931'),(6,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Author Management','/admin/authors','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:21:01.299072'),(7,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Author Management','/admin/authors','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:21:04.053351'),(8,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Author Management','/admin/authors','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:21:04.849426'),(9,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Author Management','/admin/authors','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:21:05.824079'),(10,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Author Management','/admin/authors','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:21:10.141444'),(11,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Author Management','/admin/authors','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:21:11.051095'),(12,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Author Management','/admin/authors','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:21:13.283177'),(13,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Author Management','/admin/authors','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:21:15.194604'),(14,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Author Management','/admin/authors','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:21:15.856185'),(15,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Author Management','/admin/authors','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:21:16.203566'),(16,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Author Management','/admin/authors','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:21:16.758232'),(17,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Author Management','/admin/authors','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:21:23.088218'),(18,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Author Management','/admin/authors','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:21:24.138085'),(19,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Author Management','/admin/authors','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:21:26.473353'),(20,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Author Management','/admin/authors','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:21:28.216054'),(21,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Author Management','/admin/authors','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:21:29.008198'),(22,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Author Management','/admin/authors','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:21:29.872161'),(23,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Author Management','/admin/authors','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:21:30.679583'),(24,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Author Management','/admin/authors','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:21:38.103545'),(25,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Author Management','/admin/authors','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:21:40.122587'),(26,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Author Management','/admin/authors','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:30:48.698269'),(27,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Author Management','/admin/authors','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:30:50.912875'),(28,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Author Management','/admin/authors','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:30:52.069854'),(29,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Author Management','/admin/authors','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:30:53.398381'),(30,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Author Management','/admin/authors','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:30:54.580060'),(31,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Author Management','/admin/authors','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:30:55.473079'),(32,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Librarian Dashboard','/staff/dashboard','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:30:57.130296'),(33,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Book Catalog','/catalog','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:30:58.103961'),(34,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Book Catalog','/catalog','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:31:00.995404'),(35,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Book Catalog','/catalog','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:31:04.411193'),(36,'GET','0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','Book Catalog','/catalog','8FDECC1D4FD8895266DEB01420BFEE54','librarian@gmail.com','LIBRARIAN','2026-11-26 03:31:13.796698');
/*!40000 ALTER TABLE `page_visit_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `publishers`
--

DROP TABLE IF EXISTS `publishers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `publishers` (
  `id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `name` varchar(150) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKan1ucpx8sw2qm194mlok8e5us` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `publishers`
--

LOCK TABLES `publishers` WRITE;
/*!40000 ALTER TABLE `publishers` DISABLE KEYS */;
INSERT INTO `publishers` VALUES (1,'2026-11-25 11:05:54.000000','2026-11-25 11:05:54.000000','Addison-Wesley'),(2,'2026-11-25 11:05:54.000000','2026-11-25 11:05:54.000000','Prentice Hall'),(3,'2026-11-25 11:05:54.000000','2026-11-25 11:05:54.000000','NXB Trẻ'),(4,'2026-11-25 13:45:27.000000','2026-11-25 13:45:27.000000','O\'Reilly Media'),(5,'2026-11-25 13:45:27.000000','2026-11-25 13:45:27.000000','Avery'),(6,'2026-11-25 13:45:27.000000','2026-11-25 13:45:27.000000','HarperOne'),(7,'2026-11-25 13:45:27.000000','2026-11-25 13:45:27.000000','Signet Classic'),(8,'2026-11-25 13:45:27.000000','2026-11-25 13:45:27.000000','Harper'),(9,'2026-11-25 13:45:27.000000','2026-11-25 13:45:27.000000','Farrar, Straus and Giroux'),(10,'2026-11-25 13:45:27.000000','2026-11-25 13:45:27.000000','Ace'),(11,'2026-11-26 10:26:13.000000','2026-11-26 10:26:13.000000','MIT Press'),(12,'2026-11-26 10:26:13.000000','2026-11-26 10:26:13.000000','Mariner Books'),(13,'2026-11-26 10:26:13.000000','2026-11-26 10:26:13.000000','Bloomsbury'),(14,'2026-11-26 10:26:13.000000','2026-11-26 10:26:13.000000','Scribner'),(15,'2026-11-26 10:26:14.000000','2026-11-26 10:26:14.000000','Harper Perennial'),(16,'2026-11-26 10:26:14.000000','2026-11-26 10:26:14.000000','Penguin Classics'),(17,'2026-11-26 10:26:14.000000','2026-11-26 10:26:14.000000','Vintage'),(18,'2026-11-26 10:26:14.000000','2026-11-26 10:26:14.000000','Grand Central Publishing'),(19,'2026-11-26 10:26:14.000000','2026-11-26 10:26:14.000000','Harriman House'),(20,'2026-11-26 10:26:14.000000','2026-11-26 10:26:14.000000','Crown Business'),(21,'2026-11-26 10:26:14.000000','2026-11-26 10:26:14.000000','Random House'),(22,'2026-11-26 10:26:14.000000','2026-11-26 10:26:14.000000','Spectra'),(23,'2026-11-26 10:26:14.000000','2026-11-26 10:26:14.000000','Shambhala'),(24,'2026-11-26 10:26:14.000000','2026-11-26 10:26:14.000000','NXB Kim Đồng');
/*!40000 ALTER TABLE `publishers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reservations`
--

DROP TABLE IF EXISTS `reservations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reservations` (
  `assigned_copy_id` bigint DEFAULT NULL,
  `book_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `pickup_until` datetime(6) DEFAULT NULL,
  `ready_at` datetime(6) DEFAULT NULL,
  `reserved_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `status` enum('CANCELLED','EXPIRED','FULFILLED','PENDING','READY') NOT NULL,
  `cancelled_at` datetime(6) DEFAULT NULL,
  `expired_at` datetime(6) DEFAULT NULL,
  `fulfilled_at` datetime(6) DEFAULT NULL,
  `notes` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_resv_queue` (`book_id`,`status`,`reserved_at`),
  KEY `idx_resv_member` (`member_id`,`status`),
  KEY `FKmdtkrer30fwg7m58h4t39lk8b` (`assigned_copy_id`),
  CONSTRAINT `FK9u64onf3n3yhm836lhi3ryl9h` FOREIGN KEY (`member_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKmdtkrer30fwg7m58h4t39lk8b` FOREIGN KEY (`assigned_copy_id`) REFERENCES `book_copies` (`id`),
  CONSTRAINT `FKrsdd3ib3landfpmgoolccjakt` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reservations`
--

LOCK TABLES `reservations` WRITE;
/*!40000 ALTER TABLE `reservations` DISABLE KEYS */;
INSERT INTO `reservations` VALUES (NULL,12,'2026-11-25 07:03:56.636903',1,3,NULL,NULL,'2026-11-25 07:03:56.640877','2026-11-25 07:03:56.636903','PENDING',NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `reservations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `return_receipt_items`
--

DROP TABLE IF EXISTS `return_receipt_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `return_receipt_items` (
  `fine_amount` decimal(12,2) DEFAULT NULL,
  `overdue_days` int DEFAULT NULL,
  `copy_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `loan_item_id` bigint DEFAULT NULL,
  `receipt_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `condition_notes` varchar(500) DEFAULT NULL,
  `note` varchar(500) DEFAULT NULL,
  `book_condition` enum('DAMAGED','GOOD','LOST') DEFAULT NULL,
  `item_condition` enum('DAMAGED','GOOD','LOST') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKkqecct6pvu1hnoqm3y9m4go4o` (`copy_id`),
  KEY `FKda9n1avj9993smjsl5di5eipn` (`loan_item_id`),
  KEY `FKhye5rylt6fubuo4m22ktpvmwy` (`receipt_id`),
  CONSTRAINT `FKda9n1avj9993smjsl5di5eipn` FOREIGN KEY (`loan_item_id`) REFERENCES `loan_items` (`id`),
  CONSTRAINT `FKhye5rylt6fubuo4m22ktpvmwy` FOREIGN KEY (`receipt_id`) REFERENCES `return_receipts` (`id`),
  CONSTRAINT `FKkqecct6pvu1hnoqm3y9m4go4o` FOREIGN KEY (`copy_id`) REFERENCES `book_copies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `return_receipt_items`
--

LOCK TABLES `return_receipt_items` WRITE;
/*!40000 ALTER TABLE `return_receipt_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `return_receipt_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `return_receipts`
--

DROP TABLE IF EXISTS `return_receipts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `return_receipts` (
  `total_fine_amount` decimal(12,2) DEFAULT NULL,
  `total_fines` decimal(12,2) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `loan_id` bigint DEFAULT NULL,
  `member_id` bigint NOT NULL,
  `received_by` bigint DEFAULT NULL,
  `return_date` datetime(6) DEFAULT NULL,
  `returned_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `receipt_code` varchar(50) NOT NULL,
  `note` varchar(500) DEFAULT NULL,
  `notes` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_receipt_code` (`receipt_code`),
  KEY `idx_receipt_member` (`member_id`),
  KEY `FKnyjtb3eoxm6o22ks0utbtfp4n` (`loan_id`),
  KEY `FK108a0g8dvmb360aicjtepn6c9` (`received_by`),
  CONSTRAINT `FK108a0g8dvmb360aicjtepn6c9` FOREIGN KEY (`received_by`) REFERENCES `users` (`id`),
  CONSTRAINT `FK8d1ljocftc6tfxv1rhueyd0ep` FOREIGN KEY (`member_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKnyjtb3eoxm6o22ks0utbtfp4n` FOREIGN KEY (`loan_id`) REFERENCES `loans` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `return_receipts`
--

LOCK TABLES `return_receipts` WRITE;
/*!40000 ALTER TABLE `return_receipts` DISABLE KEYS */;
/*!40000 ALTER TABLE `return_receipts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role_capabilities`
--

DROP TABLE IF EXISTS `role_capabilities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_capabilities` (
  `role_id` bigint NOT NULL,
  `capability` enum('AUDIT_READ','BOOK_RETIRE','CATALOG_EDIT','CIRCULATION','CIRCULATION_REPORT','COPY_MANAGE','FINE_PAYMENT','INVENTORY_REPORT','LOAN_REQUEST','MEMBER_CARD_ACTIVATE','MEMBER_READ','POLICY_MANAGE','PROFILE_UPDATE','RENEW','RESERVE','ROLE_MANAGE','SERVICE_EMAIL','USER_ACCESS_UPDATE','USER_READ') DEFAULT NULL,
  UNIQUE KEY `UKfuja8xmx3f0n8of16g4phy4s4` (`role_id`,`capability`),
  CONSTRAINT `FK60fa8j2juwedvcb3ms486sajj` FOREIGN KEY (`role_id`) REFERENCES `role_definitions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role_capabilities`
--

LOCK TABLES `role_capabilities` WRITE;
/*!40000 ALTER TABLE `role_capabilities` DISABLE KEYS */;
INSERT INTO `role_capabilities` VALUES (2,'LOAN_REQUEST'),(2,'PROFILE_UPDATE'),(2,'RENEW'),(2,'RESERVE'),(3,'BOOK_RETIRE'),(3,'CATALOG_EDIT'),(3,'CIRCULATION'),(3,'CIRCULATION_REPORT'),(3,'COPY_MANAGE'),(3,'FINE_PAYMENT'),(3,'INVENTORY_REPORT'),(3,'MEMBER_CARD_ACTIVATE'),(3,'MEMBER_READ'),(3,'PROFILE_UPDATE'),(3,'SERVICE_EMAIL'),(4,'AUDIT_READ'),(4,'BOOK_RETIRE'),(4,'CATALOG_EDIT'),(4,'CIRCULATION'),(4,'CIRCULATION_REPORT'),(4,'COPY_MANAGE'),(4,'FINE_PAYMENT'),(4,'INVENTORY_REPORT'),(4,'MEMBER_CARD_ACTIVATE'),(4,'MEMBER_READ'),(4,'POLICY_MANAGE'),(4,'PROFILE_UPDATE'),(4,'ROLE_MANAGE'),(4,'SERVICE_EMAIL'),(4,'USER_ACCESS_UPDATE'),(4,'USER_READ');
/*!40000 ALTER TABLE `role_capabilities` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role_definitions`
--

DROP TABLE IF EXISTS `role_definitions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_definitions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(500) DEFAULT NULL,
  `name` varchar(80) NOT NULL,
  `revision` bigint NOT NULL,
  `system_role` enum('ROLE_ADMIN','ROLE_GUEST','ROLE_LIBRARIAN','ROLE_USER') DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK9b54ix763ujkchxcrl6f54irf` (`name`),
  UNIQUE KEY `UK31vefdsnqdt2bj6qt5wfypkyv` (`system_role`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role_definitions`
--

LOCK TABLES `role_definitions` WRITE;
/*!40000 ALTER TABLE `role_definitions` DISABLE KEYS */;
INSERT INTO `role_definitions` VALUES (1,'Browse public pages without an account.','Guest',0,'ROLE_GUEST'),(2,'Manage a personal profile and borrowing requests. No access to other members or staff tools.','Member',0,'ROLE_USER'),(3,'Work with the catalog, circulation and all-member service records. No role or policy administration.','Librarian',0,'ROLE_LIBRARIAN'),(4,'Manage library operations with protected access to user accounts, roles, policies and the audit log.','Administrator',0,'ROLE_ADMIN');
/*!40000 ALTER TABLE `role_definitions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `name` varchar(30) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKofx66keruapi6vyqpv6f2or37` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_login_logs`
--

DROP TABLE IF EXISTS `user_login_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_login_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ip_address` varchar(60) DEFAULT NULL,
  `location` varchar(150) DEFAULT NULL,
  `login_at` datetime(6) NOT NULL,
  `login_method` varchar(30) DEFAULT NULL,
  `session_id` varchar(100) DEFAULT NULL,
  `user_agent` varchar(500) DEFAULT NULL,
  `user_email` varchar(150) NOT NULL,
  `user_name` varchar(150) DEFAULT NULL,
  `user_role` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_login_at` (`login_at`),
  KEY `idx_login_email` (`user_email`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_login_logs`
--

LOCK TABLES `user_login_logs` WRITE;
/*!40000 ALTER TABLE `user_login_logs` DISABLE KEYS */;
INSERT INTO `user_login_logs` VALUES (1,'0:0:0:0:0:0:0:1','Hanoi, Vietnam (Local)','2026-11-26 03:20:56.233581','PASSWORD','8FDECC1D4FD8895266DEB01420BFEE54','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/158.0.0.0 Safari/537.36','librarian@gmail.com','Library Librarian','LIBRARIAN');
/*!40000 ALTER TABLE `user_login_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `email_verified` bit(1) NOT NULL,
  `enabled` bit(1) NOT NULL,
  `auth_version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `member_code` varchar(20) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `username` varchar(50) DEFAULT NULL,
  `password` varchar(100) DEFAULT NULL,
  `email` varchar(120) NOT NULL,
  `full_name` varchar(120) NOT NULL,
  `avatar_url` varchar(500) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `provider` enum('GOOGLE','LOCAL') NOT NULL,
  `role` enum('ROLE_ADMIN','ROLE_GUEST','ROLE_LIBRARIAN','ROLE_USER') NOT NULL,
  `status` enum('ACTIVE','EXPIRED','SUSPENDED') NOT NULL,
  `assigned_role_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UKo9s61m9j2loqb9bhwc4k08gb0` (`member_code`),
  KEY `FKg0oxw25d7gbiqjn6f2k9adhsx` (`assigned_role_id`),
  CONSTRAINT `FKg0oxw25d7gbiqjn6f2k9adhsx` FOREIGN KEY (`assigned_role_id`) REFERENCES `role_definitions` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (0x01,0x01,0,'2026-11-17 08:28:44.974716',1,'2026-11-19 06:55:24.442500',NULL,NULL,'admin','$2a$10$8bCidf/b1as1of.TdMt6h.6reovV04VCWbreMJIw4SWRGlvtzYFFe','admin@gmail.com','Library Administrator',NULL,NULL,'LOCAL','ROLE_ADMIN','ACTIVE',4),(0x01,0x01,0,'2026-11-17 08:28:45.064715',2,'2026-11-19 06:52:00.200674',NULL,NULL,'librarian','$2a$10$G3AXPWYvZoSrKwW8XK.wE.6I9ZGDUKeD8Hz5fpL7cBj7CP5WhBguK','librarian@gmail.com','Library Librarian',NULL,NULL,'LOCAL','ROLE_LIBRARIAN','ACTIVE',3),(0x01,0x01,0,'2026-11-17 08:28:45.123112',3,'2026-11-25 11:05:54.000000','MB-0003',NULL,'user','$2a$10$10SG2VHs00seh3bQzZ72W.ojCpZIBYo/wA/8RJcdRZqfMCDwCLnfi','user@gmail.com','Booknet Member',NULL,NULL,'LOCAL','ROLE_USER','ACTIVE',2),(0x01,0x01,0,'2026-11-20 01:55:38.526047',4,'2026-11-20 01:55:38.631997',NULL,NULL,'hoc07062006',NULL,'hoc07062006@gmail.com','Quân Vũ Hồng','https://lh3.googleusercontent.com/a/ACg8ocJd8ZVGw7wLRAxnZDxOcExVqoZ__n5C17rsHbRuAztxahG5wEA=s96-c',NULL,'GOOGLE','ROLE_USER','ACTIVE',2),(0x01,0x01,0,'2026-11-26 10:26:14.000000',5,'2026-11-26 10:29:47.000000','MB-0005','0901234501','minhnguyen','$2a$10$iVcoOI9psA5O9wh048lPuulk9e2nESLfh0OMX07SeOgSO9IHj.FKa','minh.nguyen@booknet.local','Nguyễn Văn Minh','https://api.dicebear.com/9.x/initials/png?seed=Minh+Nguyen&backgroundColor=1e3f35','12 Trần Hưng Đạo, Hoàn Kiếm, Hà Nội','LOCAL','ROLE_USER','ACTIVE',NULL),(0x01,0x01,0,'2026-11-26 10:26:14.000000',6,'2026-11-26 10:29:47.000000','MB-0006','0901234502','anhtran','$2a$10$iVcoOI9psA5O9wh048lPuulk9e2nESLfh0OMX07SeOgSO9IHj.FKa','anh.tran@booknet.local','Trần Thị Lan Anh','https://api.dicebear.com/9.x/initials/png?seed=Lan+Anh&backgroundColor=2b5b4c','45 Láng Hạ, Đống Đa, Hà Nội','LOCAL','ROLE_USER','ACTIVE',NULL),(0x01,0x01,0,'2026-11-26 10:26:14.000000',7,'2026-11-26 10:29:47.000000','MB-0007','0901234503','huongle','$2a$10$iVcoOI9psA5O9wh048lPuulk9e2nESLfh0OMX07SeOgSO9IHj.FKa','huong.le@booknet.local','Lê Thu Hương','https://api.dicebear.com/9.x/initials/png?seed=Thu+Huong&backgroundColor=3b6b5a','88 Nguyễn Trãi, Thanh Xuân, Hà Nội','LOCAL','ROLE_USER','ACTIVE',NULL),(0x01,0x01,0,'2026-11-26 10:26:14.000000',8,'2026-11-26 10:29:47.000000','MB-0008','0901234504','ducpham','$2a$10$iVcoOI9psA5O9wh048lPuulk9e2nESLfh0OMX07SeOgSO9IHj.FKa','duc.pham@booknet.local','Phạm Hoàng Đức','https://api.dicebear.com/9.x/initials/png?seed=Hoang+Duc&backgroundColor=1f4e5f','19 Cầu Giấy, Hà Nội','LOCAL','ROLE_USER','ACTIVE',NULL),(0x01,0x01,0,'2026-11-26 10:26:14.000000',9,'2026-11-26 10:29:47.000000','MB-0009','0901234505','thaohoang','$2a$10$iVcoOI9psA5O9wh048lPuulk9e2nESLfh0OMX07SeOgSO9IHj.FKa','thao.hoang@booknet.local','Hoàng Phương Thảo','https://api.dicebear.com/9.x/initials/png?seed=Phuong+Thao&backgroundColor=6b4c3b','102 Kim Mã, Ba Đình, Hà Nội','LOCAL','ROLE_USER','ACTIVE',NULL),(0x01,0x01,0,'2026-11-26 10:26:14.000000',10,'2026-11-26 10:29:47.000000','MB-0010','0901234506','tuanvu','$2a$10$iVcoOI9psA5O9wh048lPuulk9e2nESLfh0OMX07SeOgSO9IHj.FKa','tuan.vu@booknet.local','Vũ Anh Tuấn','https://api.dicebear.com/9.x/initials/png?seed=Anh+Tuan&backgroundColor=495867','56 Giải Phóng, Hai Bà Trưng, Hà Nội','LOCAL','ROLE_USER','ACTIVE',NULL),(0x01,0x01,0,'2026-11-26 10:26:14.000000',11,'2026-11-26 10:29:47.000000','MB-0011','0901234507','linhdang','$2a$10$iVcoOI9psA5O9wh048lPuulk9e2nESLfh0OMX07SeOgSO9IHj.FKa','linh.dang@booknet.local','Đặng Khánh Linh','https://api.dicebear.com/9.x/initials/png?seed=Khanh+Linh&backgroundColor=5c4d7d','23 Phố Huế, Hai Bà Trưng, Hà Nội','LOCAL','ROLE_USER','ACTIVE',NULL),(0x01,0x01,0,'2026-11-26 10:26:14.000000',12,'2026-11-26 10:29:47.000000','MB-0012','0901234508','khanhbui','$2a$10$iVcoOI9psA5O9wh048lPuulk9e2nESLfh0OMX07SeOgSO9IHj.FKa','khanh.bui@booknet.local','Bùi Quốc Khánh','https://api.dicebear.com/9.x/initials/png?seed=Quoc+Khanh&backgroundColor=2e5339','77 Xã Đàn, Đống Đa, Hà Nội','LOCAL','ROLE_USER','ACTIVE',NULL),(0x01,0x01,0,'2026-11-26 10:26:14.000000',13,'2026-11-26 10:29:47.000000','MB-0013','0901234509','namdo','$2a$10$iVcoOI9psA5O9wh048lPuulk9e2nESLfh0OMX07SeOgSO9IHj.FKa','nam.do@booknet.local','Đỗ Hải Nam','https://api.dicebear.com/9.x/initials/png?seed=Hai+Nam&backgroundColor=7a3b2e','34 Bạch Mai, Hai Bà Trưng, Hà Nội','LOCAL','ROLE_USER','ACTIVE',NULL),(0x01,0x01,0,'2026-11-26 10:26:14.000000',14,'2026-11-26 10:29:47.000000','MB-0014','0901234510','trangngo','$2a$10$iVcoOI9psA5O9wh048lPuulk9e2nESLfh0OMX07SeOgSO9IHj.FKa','trang.ngo@booknet.local','Ngô Huyền Trang','https://api.dicebear.com/9.x/initials/png?seed=Huyen+Trang&backgroundColor=4f5d75','14 Hoàng Quốc Việt, Cầu Giấy, Hà Nội','LOCAL','ROLE_USER','ACTIVE',NULL),(0x01,0x01,0,'2026-11-26 10:26:14.000000',15,'2026-11-26 10:29:47.000000',NULL,'0902234511','hoalibrarian','$2a$10$iVcoOI9psA5O9wh048lPuulk9e2nESLfh0OMX07SeOgSO9IHj.FKa','hoa.librarian@booknet.local','Nguyễn Thanh Hoa','https://api.dicebear.com/9.x/initials/png?seed=Thanh+Hoa&backgroundColor=1e3f35','Booknet Main Desk','LOCAL','ROLE_LIBRARIAN','ACTIVE',NULL),(0x01,0x01,0,'2026-11-26 10:26:14.000000',16,'2026-11-26 10:29:47.000000',NULL,'0902234512','hunglibrarian','$2a$10$iVcoOI9psA5O9wh048lPuulk9e2nESLfh0OMX07SeOgSO9IHj.FKa','hung.librarian@booknet.local','Trần Mạnh Hùng','https://api.dicebear.com/9.x/initials/png?seed=Manh+Hung&backgroundColor=1e3f35','Booknet Circulation Desk','LOCAL','ROLE_LIBRARIAN','ACTIVE',NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'booknet_db'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-26 10:32:13
