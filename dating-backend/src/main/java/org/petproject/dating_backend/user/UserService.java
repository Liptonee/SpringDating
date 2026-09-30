package org.petproject.dating_backend.user;

import lombok.RequiredArgsConstructor;
import org.petproject.dating_backend.common.exception.BadRequestException;
import org.petproject.dating_backend.common.exception.ConflictException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    public static final String CACHE_NAME = "users";

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "'profile:' + #curUserId")
    public ProfileUserDto getCurrentUser(Long curUserId) {
        return userMapper.toProfileDto(userRepository.findByIdOrThrow(curUserId));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "#userId")
    public GetUserDto getUser(Long curUserId, Long userId) {
        return userMapper.toGetDto(userRepository.findByIdOrThrow(userId));
    }

    @Transactional()
    @CachePut(value = CACHE_NAME, key = "'profile:' + #curUserId")
    @CacheEvict(value = CACHE_NAME, key = "#curUserId")
    public ProfileUserDto patchCurrentUser(Long curUserId, ProfileUserDto patchProfileUserDto) {

        UserEntity userEntity = userRepository.findByIdOrThrow(curUserId);

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
        if (newMin != null && newMax != null && newMin > newMax) {
            throw new BadRequestException("Минимум диапазона должен быть <= максимума", 400);
        }
        userEntity.setPreferredAgeMin(newMin);
        userEntity.setPreferredAgeMax(newMax);


        return userMapper.toProfileDto(userRepository.save(userEntity));
    }

    //todo
//    public List<UserEntity> getByPreferences(Long curUserId, Short quantity) {
//
//
//    }
}

