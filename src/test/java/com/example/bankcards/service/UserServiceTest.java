package com.example.bankcards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bankcards.dto.user.CreateUserRequest;
import com.example.bankcards.dto.user.UpdateUserRequest;
import com.example.bankcards.dto.user.UserMapper;
import com.example.bankcards.dto.user.UserMapperImpl;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.exception.DuplicateResourceException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Spy
    private UserMapper userMapper = new UserMapperImpl();

    @InjectMocks
    private UserService userService;

    @Nested
    class Create {

        @Test
        void encodesPasswordAndPersists() {
            CreateUserRequest request = new CreateUserRequest("newuser", "Password1", "New User", Role.USER);
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(passwordEncoder.encode("Password1")).thenReturn("ENCODED");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            var response = userService.create(request);

            assertThat(response.username()).isEqualTo("newuser");
            assertThat(response.role()).isEqualTo(Role.USER);
            verify(passwordEncoder).encode("Password1");
            verify(userRepository).save(any(User.class));
        }

        @Test
        void rejectsDuplicateUsername() {
            CreateUserRequest request = new CreateUserRequest("admin", "Password1", null, Role.ADMIN);
            when(userRepository.existsByUsername("admin")).thenReturn(true);

            assertThatThrownBy(() -> userService.create(request))
                    .isInstanceOf(DuplicateResourceException.class);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    class Update {

        @Test
        void appliesOnlyNonNullFields() {
            User existing = new User("user", "oldhash", "Old Name", Role.USER);
            when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

            userService.update(1L, new UpdateUserRequest("New Name", null, false, null));

            assertAll(
                    () -> assertThat(existing.getFullName()).isEqualTo("New Name"),
                    () -> assertThat(existing.getRole()).isEqualTo(Role.USER),
                    () -> assertThat(existing.isEnabled()).isFalse(),
                    () -> assertThat(existing.getPassword()).isEqualTo("oldhash")
            );
        }
    }

    @Nested
    class Delete {

        @Test
        void rejectsUnknownUser() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
