package org.petproject.dating_backend.common.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.petproject.dating_backend.photo.PhotoService;
import org.petproject.dating_backend.user.UserEntity;
import org.petproject.dating_backend.user.UserGender;
import org.petproject.dating_backend.user.UserRepository;
import org.petproject.dating_backend.user.UserRole;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;

@Component
@Profile("dev")
@ConditionalOnProperty(name = "seed.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class SeedDataService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PhotoService photoService;

    private static final String DEMO_PASSWORD = "password123";

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Seed skipped: {} users already exist", userRepository.count());
            return;
        }

        log.info("Seeding demo users...");
        List<UserSeed> seeds = List.of(
                new UserSeed("Анна",  "anna@demo.com",  UserGender.FEMALE, 25, "Москва", "Люблю кофе, книги и долгие прогулки",  new Color(255, 182, 193)),
                new UserSeed("Мария", "maria@demo.com", UserGender.FEMALE, 28, "Москва", "Фотограф, ищу вдохновение",           new Color(221, 160, 221)),
                new UserSeed("Ольга", "olga@demo.com",  UserGender.FEMALE, 24, "Москва", "Программист, любительница котиков",    new Color(176, 224, 230)),
                new UserSeed("Елена", "elena@demo.com", UserGender.FEMALE, 31, "Москва", "Йога, велосипед, путешествия",         new Color(255, 218, 185)),
                new UserSeed("Дарья", "daria@demo.com", UserGender.FEMALE, 27, "Москва", "Врач, люблю живопись",                 new Color(240, 230, 140)),
                new UserSeed("Егор",  "egor@demo.com",  UserGender.MALE,   30, "Москва", "Бегаю по утрам, читаю по вечерам",     new Color(135, 206, 235)),
                new UserSeed("Иван",  "ivan@demo.com",  UserGender.MALE,   27, "Москва", "Разработчик, геймер, гитарист",        new Color(152, 251, 152)),
                new UserSeed("Пётр",  "petr@demo.com",  UserGender.MALE,   33, "Москва", "Предприниматель, люблю горы",          new Color(255, 160, 122)),
                new UserSeed("Максим","maxim@demo.com", UserGender.MALE,   26, "Москва", "Дизайнер, сноубордист",                new Color(175, 238, 238)),
                new UserSeed("Артём", "artem@demo.com", UserGender.MALE,   29, "Москва", "Повар, обожаю итальянскую кухню",       new Color(250, 250, 210)),
                new UserSeed("Сергей","sergey@demo.com",UserGender.MALE,   35, "Москва", "Инженер, автолюбитель",                new Color(211, 211, 211)),
                new UserSeed("Дмитрий","dmitry@demo.com",UserGender.MALE,  22, "Москва", "Студент, играю в баскетбол",           new Color(255, 228, 196))
        );

        int photosCount = 0;
        for (UserSeed seed : seeds) {
            UserEntity user = createUser(seed);
            try {
                loadPhotos(user, seed);
                photosCount++;
            } catch (Exception e) {
                log.error("Failed to load photos for {}", seed.email(), e);
            }
        }

        log.info("Seeded {} users, {} with photos. Password for all: '{}'",
                seeds.size(), photosCount, DEMO_PASSWORD);
    }

    private UserEntity createUser(UserSeed seed) {
        UserEntity user = new UserEntity();
        user.setEmail(seed.email());
        user.setFirstName(seed.firstName());
        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        user.setRole(UserRole.USER);
        user.setGender(seed.gender());
        user.setAge((short) seed.age());
        user.setCity(seed.city());
        user.setShortAbout(seed.shortAbout());
        user.setFullAbout(seed.shortAbout() + ". Заполните описание подробнее в профиле.");
        // Предпочтения: ищем противоположный пол в диапазоне ±5 лет
        user.setPreferredAgeMin((short) Math.max(18, seed.age() - 5));
        user.setPreferredAgeMax((short) Math.min(100, seed.age() + 5));
        user.setReadyForDeck(false);
        return userRepository.save(user);
    }

    private void loadPhotos(UserEntity user, UserSeed seed) throws Exception {
        // Загружаем 2 фото: первое станет главным автоматически
        for (int i = 0; i < 2; i++) {
            byte[] png = generateAvatarPng(seed, i);
            String filename = "seed-" + seed.email().split("@")[0] + "-" + i + ".png";
            MockMultipartFile file = new MockMultipartFile(
                    "file", filename, "image/png", png
            );
            photoService.uploadPhoto(user.getId(), file);
        }
        // После загрузки хотя бы одного фото профиль готов к колоде
        UserEntity managed = userRepository.findById(user.getId()).orElseThrow();
        managed.setReadyForDeck(true);
        userRepository.save(managed);
    }

    /**
     * Генерирует PNG 400x400 с цветным фоном и первой буквой имени.
     */
    private byte[] generateAvatarPng(UserSeed seed, int variant) throws IOException {
        int size = 400;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Фон
            Color bg = variant == 0
                    ? seed.color()
                    : seed.color().darker();
            g.setColor(bg);
            g.fillRect(0, 0, size, size);

            // Инициал
            String letter = seed.firstName().substring(0, 1);
            g.setColor(contrastColor(bg));
            g.setFont(new Font("SansSerif", Font.BOLD, 220));
            FontMetrics fm = g.getFontMetrics();
            int x = (size - fm.stringWidth(letter)) / 2;
            int y = (size - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(letter, x, y);

            // Подпись снизу
            g.setFont(new Font("SansSerif", Font.PLAIN, 28));
            String caption = seed.firstName() + ", " + seed.age();
            fm = g.getFontMetrics();
            g.drawString(caption, (size - fm.stringWidth(caption)) / 2, size - 40);
        } finally {
            g.dispose();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    private Color contrastColor(Color bg) {
        // Простая формула контраста
        double luminance = (0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue()) / 255;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    private record UserSeed(
            String firstName,
            String email,
            UserGender gender,
            int age,
            String city,
            String shortAbout,
            Color color
    ) {}
}