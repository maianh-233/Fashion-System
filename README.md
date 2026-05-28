# 👗 Lunaria Boutique — Fashion E-commerce System

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-Microservices-6DB33F?logo=springboot)
![React](https://img.shields.io/badge/React-Vite-61DAFB?logo=react)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-316192?logo=postgresql)
![Kafka](https://img.shields.io/badge/Kafka-Event%20Driven-231F20?logo=apachekafka)
![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED?logo=docker)
![Status](https://img.shields.io/badge/Status-In%20Development-yellow)

---

## 🎯 Mục tiêu dự án

Hệ thống thương mại điện tử thời trang đa cửa hàng được xây dựng nhằm:

* Thực hành **kiến trúc Microservices thực tế với Spring Boot**
* Tìm hiểu **hệ thống phân tán, event-driven architecture (Kafka)**
* Xây dựng full-flow e-commerce: sản phẩm → đơn hàng → thanh toán → giao hàng

---

## ⚙️ Tech Stack

**Backend**

* Spring Boot Microservices
* Spring Cloud Gateway
* Spring Security + JWT (RBAC)
* Kafka (Event-driven system)
* WebSocket (Realtime chat)
* PostgreSQL + Flyway

**Frontend**

* React (Vite)
* TailwindCSS
* Axios

**Infrastructure**

* Docker
* Cloudinary (image storage)

---

## ✨ Key Features

* 🛍️ Hệ thống bán hàng đa cửa hàng (multi-store)
* 📍 Tự động chọn cửa hàng gần nhất theo vị trí giao hàng
* 💳 Thanh toán: VNPay / MoMo / COD
* 🔐 Phân quyền hệ thống (Admin / Staff / Customer)
* 💬 Chat realtime khách hàng ↔ nhân viên theo đơn hàng
* 🤖 Chat admin ↔ chatbot hệ thống
* 📦 Quản lý sản phẩm, đơn hàng, kho hàng
* ⚡ Event-driven order processing bằng Kafka

---

## 🧠 Kiến trúc nổi bật

* Microservices tách theo domain (Order, Product, Payment,...)
* API Gateway định tuyến request
* Kafka xử lý event bất đồng bộ
* WebSocket hỗ trợ realtime chat
* PostgreSQL + Flyway migration cho từng service

---

## 🚧 Trạng thái

⚠️ Dự án đang trong quá trình phát triển
Một số module frontend sử dụng mock data để mô phỏng luồng nghiệp vụ.

---

## 👨‍💻 Vai trò dự án

> Dự án cá nhân nhằm nâng cao kỹ năng backend và system design, tập trung vào:

* Thiết kế hệ thống backend quy mô lớn
* Xử lý bất đồng bộ với Kafka
* Xây dựng kiến trúc microservices chuẩn production
* Tích hợp realtime + payment + geolocation


