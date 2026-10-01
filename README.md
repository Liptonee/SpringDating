# Dating Backend

REST API дейтинг-приложения: регистрация, профили, фото, свайпы, взаимные матчи.
Pet-проект на Spring Boot 4 с полным циклом аутентификации, хранилищем фото и кэшированием.

---

## Стек

| Технология | Версия | Зачем |
|---|---|---|
| Java | 25 | Язык |
| Spring Boot | 4.1.1 | Фреймворк |
| Spring Security | 7.x | Аутентификация, авторизация |
| PostgreSQL | 18 | Основная БД |
| Flyway | 11.x | Миграции схемы |
| Redis | 7 | Refresh-токены, кэш профилей и фото |
| MinIO | latest | S3-совместимое хранилище фото |
| JWT (jjwt) | 0.12.6 | Access-токены |
| MapStruct | 1.6.3 | Маппинг Entity ↔ DTO |
| Lombok | 1.18.44 | Бойлерплейт |
| springdoc-openapi | 3.1.0 | Swagger UI |

---

## Быстрый старт

### Требования
- Docker и Docker Compose
- JDK 25
- Maven (или использовать `./mvnw`)

### Запуск

```bash
# 1. Поднять инфраструктуру (Postgres + Redis + MinIO)
docker compose up -d

# 2. Запустить приложение
./mvnw spring-boot:run

# 3. Открыть Swagger
open http://localhost:8080/swagger-ui.html
```

### Доступы

| Сервис | URL | Логин / пароль |
|---|---|---|
| API | http://localhost:8080 | — |
| Swagger UI | http://localhost:8080/swagger-ui.html | — |
| MinIO Console | http://localhost:9001 | `admin` / `adminadmin` |
| PostgreSQL | `localhost:5432` | `postgres` / `root` |
| Redis | `localhost:6379` | без пароля |

### Загрузка тестовых данных

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

При старте с профилем `dev` создаются 12 демо-пользователей с фото и заполненными профилями.
Все пароли: `password123`.

Демо-аккаунты для входа:

| Email | Пол | Возраст |
|---|---|---|
| `anna@demo.com` | FEMALE | 25 |
| `maria@demo.com` | FEMALE | 28 |
| `olga@demo.com` | FEMALE | 24 |
| `egor@demo.com` | MALE | 30 |
| `ivan@demo.com` | MALE | 27 |
| ... | ... | ... |

---


### Доменные модули

| Модуль | Ответственность |
|---|---|
| `auth` | Регистрация, вход, refresh, logout |
| `user` | Профиль, предпочтения, готовность к колоде |
| `photo` | Загрузка/удаление, presigned URL, назначение главного |
| `deck` | Формирование колоды карточек по предпочтениям |
| `swipe` | LIKE/DISLIKE, идемпотентность |
| `match` | Создание матча при взаимном лайке |
| `common.security` | JWT, фильтры, refresh-token service |
| `common.exception` | Глобальный handler, доменные исключения |
| `common.config` | MinIO, Redis, Swagger, Security, AOP |

### Схема БД

- `users` — пользователи с профилем и предпочтениями
- `photos` — метаданные фото (сами файлы в MinIO), `is_main`, `UNIQUE (user_id, is_main) WHERE is_main = true` (частичный)
- `swipes` — `UNIQUE (from_id, to_id)`, action = LIKE/DISLIKE
- `matches` — `UNIQUE (first_user_id, second_user_id)`, `CHECK (first_user_id < second_user_id)` — нормализация пары

Все миграции в `src/main/resources/db/migration/`, применяются Flyway при старте.

---

## Аутентификация

Схема — **JWT access + HttpOnly cookie refresh + Redis**.

### Поток

1. `POST /api/auth/login` — проверка email/пароль.
    - Возвращает **access JWT** в теле (TTL 15 минут).
    - Устанавливает **refresh token** в HttpOnly cookie (TTL 7 дней).
    - Refresh-токен в Redis хранится **в виде SHA-256 хэша** (защита от дампа Redis).

2. Защищённые запросы: `Authorization: Bearer <access>`.

3. `POST /api/auth/refresh` — **ротация**:
    - Читает refresh из cookie.
    - Проверяет наличие в Redis.
    - Удаляет старый, создаёт новый.
    - Возвращает новый access + ставит новый refresh в cookie.

4. `POST /api/auth/logout` — удаляет refresh из Redis, очищает cookie.

---

## API Endpoints

### Auth

| Метод | Путь | Описание |
|---|---|---|
| POST | `/api/auth/register` | Регистрация |
| POST | `/api/auth/login` | Вход |
| POST | `/api/auth/refresh` | Обновление токенов |
| POST | `/api/auth/logout` | Выход |

### Users

| Метод | Путь | Описание |
|---|---|---|
| GET | `/api/users/me` | Текущий пользователь |
| PATCH | `/api/users/me` | Редактирование профиля |
| GET | `/api/users/me/completeness` | Незаполненные поля |
| GET | `/api/users/{id}` | Профиль другого пользователя |

### Photos

| Метод | Путь | Описание |
|---|---|---|
| POST | `/api/photos` | Загрузить фото (multipart) |
| GET | `/api/photos` | Список фото (+ presigned URL) |
| GET | `/api/photos/{id}` | Одно фото + URL |
| GET | `/api/photos/{id}/url` | Только URL |
| DELETE | `/api/photos/{id}` | Удалить |

Лимит: 5 фото на пользователя, 5 МБ каждое, JPEG/PNG/WebP.
Первое загруженное фото автоматически становится главным.

### Deck & Swipe

| Метод | Путь | Описание |
|---|---|---|
| GET | `/api/deck` | Колода карточек |
| POST | `/api/swipes` | LIKE или DISLIKE |
| GET | `/api/matches` | Список матчей |

---

## Ключевые решения

### Presigned URL для фото

Клиент никогда не ходит в MinIO напрямую. Сервер генерирует **presigned URL** (TTL 6 часов), клиент получает готовую ссылку в ответе. 

### Pessimistic lock при загрузке/удалении фото

`userRepository.findByIdForUpdate(userId)` берёт `SELECT ... FOR UPDATE` на строку пользователя. Защищает от race condition при проверке лимита (5 фото) и при назначении главного фото при параллельных операциях.

### Идемпотентные свайпы

Двойной тап в UI не должен ломать UX. Повторный свайп с тем же действием возвращает текущее состояние матча, а не ошибку 409. `UNIQUE (from_id, to_id)` + `saveAndFlush` + `catch (DataIntegrityViolationException)` защищают от race condition.

### Нормализация пары в matches

`CHECK (first_user_id < second_user_id)` гарантирует, что пара (A, B) и (B, A) — одна и та же строка. `UNIQUE (first_user_id, second_user_id)` — от дубликатов.

### Готовность к колоде

Пользователь не появится в чужой колоде и не получит колоду, пока не заполнит обязательные поля. Правило живёт на бэке (`getNotReadyFields`), клиент получает список незаполненного и рендерит экран заполнения. Флаг `ready_for_deck` денормализован для быстрой фильтрации.

---

## Что не сделано

- **Приватность фото.** Любой авторизованный пользователь может посмотреть чужие фото по ID. Система приватности (друзья, приватные альбомы, жалобы, блокировки) не реализована.
- **Rate limiting.** Нет ограничения на частоту запросов (login, swipe).
- **Real-time матчи.** Матч виден только через REST-polling, WebSocket не реализован.
- **Пагинация.** В deck и matches — жёсткий лимит вместо cursor-based pagination.
- **Тесты.** Покрытие нулевое. Планируется: Testcontainers + MockMvc для auth/swipe/deck.
- **Приватные секреты.** `jwt.secret` и `minio.secret-key` лежат в `application.properties`. Для прода — переменные окружения / Vault.
- **N+1 при построении колоды.** Для каждой карточки отдельный вызов presigned URL. Кэшируется частично через Redis.

---

## Разработка

### Структура

```
src/main/java/org/petproject/dating_backend/
├── auth/              # Аутентификация
├── user/              # Профили
├── photo/             # Фото
├── deck/              # Колода
├── swipe/             # Свайпы
├── match/             # Матчи
└── common/
    ├── config/        # Конфиги Spring-бинов
    ├── exception/     # Обработка ошибок
    └── security/      # JWT, фильтры, userDetails
```

### Миграции

Новая миграция — файл в `src/main/resources/db/migration/` с именем `V<N>__<описание>.sql`. Flyway применит при старте.

### Профили

- `default` — прод-настройки, без сид-данных
- `dev` — включает загрузку демо-данных при старте
