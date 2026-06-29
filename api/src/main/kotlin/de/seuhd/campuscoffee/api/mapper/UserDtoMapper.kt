package de.seuhd.campuscoffee.api.mapper

import de.seuhd.campuscoffee.api.dtos.UserDto
import de.seuhd.campuscoffee.domain.model.objects.Role
import de.seuhd.campuscoffee.domain.model.objects.User
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean

// GEN AI GENERIERT, daich dachte ich hätte MAPPING PROBLEME
@Mapper(componentModel = "spring", imports = [Role::class])
@ConditionalOnMissingBean
interface UserDtoMapper : DtoMapper<User, UserDto> {
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(source = "password", target = "password")
    @Mapping(
        target = "roles",
        expression = "java(source.getRoles() != null ? source.getRoles() : java.util.Set.of(Role.USER))"
    )
    override fun toDomain(source: UserDto): User

    @Mapping(target = "password", ignore = true)
    override fun fromDomain(source: User): UserDto
}
