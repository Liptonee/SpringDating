package org.petproject.dating_backend.user;

import lombok.RequiredArgsConstructor;
import org.petproject.dating_backend.common.exception.BadRequestException;
import org.petproject.dating_backend.common.exception.ConflictException;
import org.petproject.dating_backend.common.exception.ForbiddenException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ObjectProvider<UserService> selfProvider;
    public static final String CACHE_NAME = "users";

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "'profile:' + #curUserId")
    public ProfileUserDto getCurrentUser(Long curUserId) {
        return userMapper.toProfileDto(userRepository.findByIdOrElseThrow(curUserId));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "#userId")
    public GetUserDto getUser(Long curUserId, Long userId) {
        return userMapper.toGetDto(userRepository.findByIdOrElseThrow(userId));
    }

    @Transactional()
    @CachePut(value = CACHE_NAME, key = "'profile:' + #curUserId")
    @CacheEvict(value = CACHE_NAME, key = "#curUserId")
    public ProfileUserDto patchCurrentUser(Long curUserId, ProfileUserDto patchProfileUserDto) {

        UserEntity userEntity = userRepository.findByIdOrElseThrow(curUserId);

        if (patchProfileUserDto.firstName() != null && !patchProfileUserDto.firstName().isBlank()) {
            userEntity.setFirstName(patchProfileUserDto.firstName());
        }
        if (patchProfileUserDto.age() != null) {
            userEntity.setAge(patchProfileUserDto.age());
        }
        if (patchProfileUserDto.email() != null && !patchProfileUserDto.email().isBlank()) {
            if (userRepository.existsByEmailAndIdNot(patchProfileUserDto.email(), curUserId)) {
                throw new ConflictException("Такой email уже существует", 409);
            }
            userEntity.setEmail(patchProfileUserDto.email());
        }
        if (patchProfileUserDto.city() != null && !patchProfileUserDto.city().isBlank()) {
            userEntity.setCity(patchProfileUserDto.city());
        }
        if (patchProfileUserDto.gender() != null) {
            userEntity.setGender(patchProfileUserDto.gender());
        }
        if (patchProfileUserDto.shortAbout() != null) {
            userEntity.setShortAbout(patchProfileUserDto.shortAbout());
        }
        if (patchProfileUserDto.fullAbout() != null) {
            userEntity.setFullAbout(patchProfileUserDto.fullAbout());
        }
        if (patchProfileUserDto.mainPhotoId() != null) {
            userEntity.setMainPhotoId(patchProfileUserDto.mainPhotoId());
        }

        Short newMin = patchProfileUserDto.preferredAgeMin() != null
                ? patchProfileUserDto.preferredAgeMin()
                : userEntity.getPreferredAgeMin();
        Short newMax = patchProfileUserDto.preferredAgeMax() != null
                ? patchProfileUserDto.preferredAgeMax()
                : userEntity.getPreferredAgeMax();
        if (newMin != null && newMax != null && newMin > newMax) {
            throw new BadRequestException("Минимум диапазона должен быть <= максимума", 400);
        }
        userEntity.setPreferredAgeMin(newMin);
        userEntity.setPreferredAgeMax(newMax);


        if (selfProvider.getObject().getNotReadyFields(curUserId).isEmpty()) {
            userEntity.setReadyForDeck(true);
        }

        return userMapper.toProfileDto(userRepository.save(userEntity));
    }

    @Transactional(readOnly = true)
    public List<GetUserDto> getByPreferences(Long curUserId, Short quantity) {
        if (!selfProvider.getObject().getNotReadyFields(curUserId).isEmpty()) {
            throw new ForbiddenException("Профиль пользователя не готов к получению колоды", 403);
        }
        UserEntity user = userRepository.findByIdOrElseThrow(curUserId);

        UserGender gender = user.getGender().equals(UserGender.MALE)
                ? UserGender.FEMALE : UserGender.MALE;
        Short ageMin = user.getPreferredAgeMin();
        Short ageMax = user.getPreferredAgeMax();
        String city = user.getCity();

        return userRepository.findByPreferences(curUserId, gender, ageMin, ageMax, city, quantity)
                .stream().map(userMapper::toGetDto).toList();
    }

    @Transactional(readOnly = true)
    public Set<String> getNotReadyFields(Long curUserId) {
        UserEntity user = userRepository.findByIdOrElseThrow(curUserId);
        Set<String> resultSet = new HashSet<>();

        if (user.getGender() == null) resultSet.add("gender");
        if (user.getAge() == null) resultSet.add("age");
        if (user.getCity() == null) resultSet.add("city");
        if (user.getShortAbout() == null) resultSet.add("short about");
        if (user.getMainPhotoId() == null) resultSet.add("main photo");
        if (user.getPreferredAgeMax() == null) resultSet.add("preferred age max");
        if (user.getPreferredAgeMin() == null) resultSet.add("preferred age min");

        return resultSet;
    }

}

