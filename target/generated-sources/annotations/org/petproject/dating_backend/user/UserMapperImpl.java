package org.petproject.dating_backend.user;

import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-26T14:57:24+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 26.0.1 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserDto toDto(UserEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Long id = null;
        String firstName = null;
        String email = null;
        String city = null;
        Integer age = null;
        Short preferredAgeMin = null;
        Short preferredAgeMax = null;
        String shortAbout = null;
        String fullAbout = null;
        UserGender gender = null;

        id = entity.getId();
        firstName = entity.getFirstName();
        email = entity.getEmail();
        city = entity.getCity();
        age = entity.getAge();
        preferredAgeMin = entity.getPreferredAgeMin();
        preferredAgeMax = entity.getPreferredAgeMax();
        shortAbout = entity.getShortAbout();
        fullAbout = entity.getFullAbout();
        gender = entity.getGender();

        UserDto userDto = new UserDto( id, firstName, email, city, age, preferredAgeMin, preferredAgeMax, shortAbout, fullAbout, gender );

        return userDto;
    }
}
