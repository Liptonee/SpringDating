package org.petproject.dating_backend.user;

import lombok.RequiredArgsConstructor;
import org.petproject.dating_backend.common.exception.BadRequestException;
import org.petproject.dating_backend.common.exception.ConflictException;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    public static final String CACHE_NAME = "users";

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "#curUserId")
    public UserDto getCurrentUser(Long curUserId) {
        return userMapper.toDto(userRepository.findByIdOrThrow(curUserId));
    }

    @Cacheable(value = CACHE_NAME, key = "#userId")
    public UserDto getUser(Long curUserId, Long userId) {
        return userMapper.toDto(userRepository.findByIdOrThrow(userId));
    }

    @Transactional()
    @CachePut(value = CACHE_NAME, key = "#curUserId")
    public UserDto patchCurrentUser(Long curUserId, UserDto patchUserDto) {

        UserEntity userEntity = userRepository.findByIdOrThrow(curUserId);

        if (patchUserDto.firstName() != null && !patchUserDto.firstName().isBlank()) {
            userEntity.setFirstName(patchUserDto.firstName());
        }
        if (patchUserDto.age() != null) {
            userEntity.setAge(patchUserDto.age());
        }
        if (patchUserDto.email() != null && !patchUserDto.email().isBlank()) {
            if (userRepository.existsByEmailAndIdNot(patchUserDto.email(), curUserId)){
                throw new ConflictException("Такой email уже существует", 409);
            }
            userEntity.setEmail(patchUserDto.email());
        }
        if (patchUserDto.city() != null && !patchUserDto.city().isBlank()) {
            userEntity.setCity(patchUserDto.city());
        }
        if (patchUserDto.gender() != null) {
            userEntity.setGender(patchUserDto.gender());
        }
        if (patchUserDto.shortAbout() != null) {
            userEntity.setShortAbout(patchUserDto.shortAbout());
        }
        if (patchUserDto.fullAbout() != null) {
            userEntity.setFullAbout(patchUserDto.fullAbout());
        }


        Short newMin = patchUserDto.preferredAgeMin() != null
                ? patchUserDto.preferredAgeMin()
                : userEntity.getPreferredAgeMin();
        Short newMax = patchUserDto.preferredAgeMax() != null
                ? patchUserDto.preferredAgeMax()
                : userEntity.getPreferredAgeMax();
        if (newMin != null && newMax != null && newMin > newMax) {
            throw new BadRequestException("Минимум диапазона должен быть <= максимума", 400);
        }
        userEntity.setPreferredAgeMin(newMin);
        userEntity.setPreferredAgeMax(newMax);


        return userMapper.toDto(userRepository.save(userEntity));
    }


}
