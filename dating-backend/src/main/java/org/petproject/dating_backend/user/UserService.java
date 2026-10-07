package org.petproject.dating_backend.user;

import lombok.RequiredArgsConstructor;
import org.petproject.dating_backend.common.exception.BadRequestException;
import org.petproject.dating_backend.common.exception.ConflictException;
import org.petproject.dating_backend.common.exception.ForbiddenException;
import org.petproject.dating_backend.common.exception.NotFoundException;
import org.petproject.dating_backend.photo.PhotoService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PhotoService photoService;
    public static final String CACHE_NAME = "users";

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "'profile:' + #curUserId")
    public ProfileUserDto getCurrentUser(Long curUserId) {
        return userMapper.toProfileDto(userRepository.findByIdOrElseThrow(curUserId));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "'get:' + #userId")
    public GetUserDto getUser(Long userId) {
        return userMapper.toGetDto(userRepository.findByIdOrElseThrow(userId));
    }

    @Transactional()
    @Caching(
            put = @CachePut(value = CACHE_NAME, key = "'profile:' + #curUserId"),
            evict = {
                    @CacheEvict(value = CACHE_NAME, key = "'get:' + #curUserId"),
                    @CacheEvict(value = CACHE_NAME, key = "'firstName:' + #curUserId")
            }
    )
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

        Short newMin = patchProfileUserDto.preferredAgeMin() != null
                ? patchProfileUserDto.preferredAgeMin()
                : userEntity.getPreferredAgeMin();
        Short newMax = patchProfileUserDto.preferredAgeMax() != null
                ? patchProfileUserDto.preferredAgeMax()
                : userEntity.getPreferredAgeMax();
        if (newMin != null && newMax != null && newMin <= newMax) {
            throw new BadRequestException("Минимум диапазона должен быть <= максимума", 400);
        }
        userEntity.setPreferredAgeMin(newMin);
        userEntity.setPreferredAgeMax(newMax);


        if (getNotReadyFields(curUserId).isEmpty()) {
            userEntity.setReadyForDeck(true);
        }

        return userMapper.toProfileDto(userRepository.save(userEntity));
    }


    @Transactional(readOnly = true)
    public List<GetUserDto> getByPreferences(Long curUserId, Short quantity) {
        if (!getNotReadyFields(curUserId).isEmpty()) {
            throw new ForbiddenException("Профиль пользователя не готов к получению колоды", 403);
        }
        UserEntity user = userRepository.findByIdOrElseThrow(curUserId);

        UserGender gender = user.getGender().equals(UserGender.MALE)
                ? UserGender.FEMALE : UserGender.MALE;
        Short ageMin = user.getPreferredAgeMin();
        Short ageMax = user.getPreferredAgeMax();
        String city = user.getCity();

        Pageable pageable = PageRequest.of(0, quantity);
        return userRepository.findByPreferences(curUserId, gender, ageMin, ageMax, city, pageable)
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
        if (!photoService.hasMainPhoto(curUserId)) resultSet.add("main photo");
        if (user.getPreferredAgeMax() == null) resultSet.add("preferred age max");
        if (user.getPreferredAgeMin() == null) resultSet.add("preferred age min");

        return resultSet;
    }

    @Transactional(readOnly = true)
    public Map<Long, String> getFirstNamesByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return userRepository.findFirstNamesByIds(ids).stream()
                .collect(Collectors.toMap(
                        UserRepository.UserNameProjection::getId,
                        UserRepository.UserNameProjection::getFirstName
                ));
    }

}

